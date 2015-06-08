package com.camptocamp.sbb;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.ObjectId;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.plumbing.RevParse;
import org.locationtech.geogig.api.plumbing.merge.Conflict;
import org.locationtech.geogig.api.porcelain.AddOp;
import org.locationtech.geogig.api.porcelain.BranchCreateOp;
import org.locationtech.geogig.api.porcelain.BranchDeleteOp;
import org.locationtech.geogig.api.porcelain.CheckoutOp;
import org.locationtech.geogig.api.porcelain.CommitOp;
import org.locationtech.geogig.api.porcelain.MergeConflictsException;
import org.locationtech.geogig.api.porcelain.MergeOp;
import org.locationtech.geogig.api.porcelain.StatusOp;
import org.locationtech.geogig.api.porcelain.StatusOp.StatusSummary;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.selection.CommitCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.style.sld.SLDContent;
import org.locationtech.udig.ui.PlatformGIS;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import com.camptocamp.sbb.views.BranchTypeView;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class BranchMerge {
	public final String MERGE_BRANCH_NAME = "INTERNAL_MERGE_BRANCH";

	private final String ftName;
	private Ref ref;

	private BranchTypeView view;
	private Iterable<Conflict> conflicts;

	public BranchMerge(Ref ref, String ftName, BranchTypeView view) {
		this.ftName = ftName;
		this.ref = ref;
		this.view = view;
	}

	void handleConflict(IProgressMonitor monitor) {
		GeoGIG geoGig = getGeoGig();
		StatusOp statusOp = geoGig.command(StatusOp.class);
		StatusSummary statusReport = statusOp.call();
		this.conflicts = statusReport.getConflicts().get();
		monitor.worked(1);

		StyleBuilder styleBuilder = SLDContent.getStyleBuilder();
		FilterFactory2 filterFactory = styleBuilder.getFilterFactory();
		Set<FeatureId> conflictFids = Sets.newHashSet();
		CheckoutOp checkoutOp = geoGig.command(CheckoutOp.class);
		checkoutOp.setOurs(true);
		AddOp addOp = geoGig.command(AddOp.class);

		for (Conflict conflict : conflicts) {
			String path = conflict.getPath();
			String[] parts = path.split("/");
			String fid = parts[parts.length - 1];
			String ft = parts[parts.length - 2];
			if (ft.equals(ftName)) {
				conflictFids.add(filterFactory.featureId(fid));
			}
			checkoutOp.addPath(path);
			addOp.addPattern(path);
		}

		Rule conflictRule = null;
		if (!conflictFids.isEmpty()) {
			checkoutOp.call();
			addOp.call();
			monitor.worked(1);

			geoGig.command(CommitOp.class).setMessage("Merge Conflict Merge").call();
			Stroke stroke = styleBuilder.createStroke(Color.BLACK, 2, new float[] { 5, 5 });
			LineSymbolizer lineSym = styleBuilder.createLineSymbolizer(stroke);
			
			conflictRule = styleBuilder.createRule(lineSym);
			org.opengis.filter.Id idFilter = filterFactory.id(conflictFids);
			conflictRule.setFilter(idFilter);
		}

		Style conflictStyle = Styling.createTrackStyle();
		if (conflictRule != null) {
			List<Rule> rules = conflictStyle.featureTypeStyles().get(0).rules();
			rules.add(conflictRule);
		}
		Function<Map, Void> zoomToFirstConflict = new Function<Map, Void>() {

			@Override
			public Void apply(Map map) {
				BranchTypeView.zoom(view.getViewSite(), map);
				return null;
			}
		};
		monitor.worked(1);

		Activator.getDefault().showInMap(view.getSite(), "Conflict Resolution", zoomToFirstConflict,
				new LayerToAdd(ref.getName(), ftName, createRefStyle(styleBuilder), ref.localName()),
				new LayerToAdd("master", ftName, createMasterStyle(styleBuilder), "master"),
				new LayerToAdd(MERGE_BRANCH_NAME, ftName, conflictStyle, "Merge Result"));
	}

	private Style createMasterStyle(StyleBuilder styleBuilder) {
		Stroke stroke = styleBuilder.createStroke(Color.YELLOW, 2, new float[] { 5, 2 });
		LineSymbolizer sym = styleBuilder.createLineSymbolizer(stroke);
		return styleBuilder.createStyle(sym);
	}

	private Style createRefStyle(StyleBuilder styleBuilder) {
		Stroke stroke = styleBuilder.createStroke(Color.BLACK, 2, new float[] { 5, 2 });
		LineSymbolizer sym = styleBuilder.createLineSymbolizer(stroke);
		return styleBuilder.createStyle(sym);
	}

	private GeoGIG getGeoGig() {
		return Activator.getDefault().getGeoGig();
	}

	public void execute() {
		PlatformGIS.runInProgressDialog("Executing Merge", false, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Merging", 6);
				BranchCreateOp createOp = getGeoGig().command(BranchCreateOp.class);
				createOp.setForce(true);
				createOp.setName(MERGE_BRANCH_NAME);
				createOp.setAutoCheckout(true);
				createOp.setSource("master");
				createOp.call();

				monitor.worked(1);
				Optional<ObjectId> objectId = getGeoGig().command(RevParse.class).setRefSpec(ref.getName()).call();

				MergeOp mergeOp = getGeoGig().command(MergeOp.class);
				mergeOp.addCommit(Suppliers.ofInstance(objectId.get()));

				try {
					mergeOp.call();
					CheckoutOp checkoutOp = getGeoGig().command(CheckoutOp.class);
					checkoutOp.setSource("master");
					checkoutOp.setForce(true);
					checkoutOp.call();
					monitor.worked(1);

					objectId = getGeoGig().command(RevParse.class).setRefSpec(MERGE_BRANCH_NAME).call();
					mergeOp = getGeoGig().command(MergeOp.class);
					mergeOp.addCommit(Suppliers.ofInstance(objectId.get()));
					mergeOp.call();
					monitor.worked(1);

					BranchDeleteOp deleteOp = getGeoGig().command(BranchDeleteOp.class);
					deleteOp.setName(ref.getName());
					deleteOp.call();
					deleteOp.setName(MERGE_BRANCH_NAME);
					deleteOp.call();
					monitor.worked(1);

					updateViewer();
				} catch (MergeConflictsException e) {
					monitor.worked(1);
					monitor.setTaskName("Handling Conflicts");
					handleConflict(monitor);
				}
				monitor.done();
			}
		}, false);

	}

	public void finishMerge() {
		PlatformGIS.runInProgressDialog("Finish Merge", false, new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Finish Merge", 5);
				Map activeMap = (Map) ApplicationGIS.getActiveMap();
				try {
					activeMap.getEditManagerInternal().commitTransaction();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				CheckoutOp checkoutOp = getGeoGig().command(CheckoutOp.class);
				checkoutOp.setForce(true);
				checkoutOp.setSource("master");
				checkoutOp.call();

				monitor.worked(1);
				Optional<ObjectId> objectId = getGeoGig().command(RevParse.class).setRefSpec(MERGE_BRANCH_NAME).call();

				MergeOp mergeOp = getGeoGig().command(MergeOp.class);
				mergeOp.addCommit(Suppliers.ofInstance(objectId.get()));

				try {
					mergeOp.call();
					monitor.worked(1);
				} catch (MergeConflictsException e) {
					StatusOp statusOp = getGeoGig().command(StatusOp.class);
					StatusSummary statusReport = statusOp.call();
					Iterable<Conflict> conflicts = statusReport.getConflicts().get();
					checkoutOp.setOurs(true);
					AddOp addOp = getGeoGig().command(AddOp.class);
					for (Conflict conflict : conflicts) {
						String path = conflict.getPath();
						checkoutOp.addPath(path);
						addOp.addPattern(path);
					}

					checkoutOp.setTheirs(true);
					checkoutOp.call();
					addOp.call();

					monitor.worked(1);
					CommitOp commitOp = getGeoGig().command(CommitOp.class);
					commitOp.call();
				}
				monitor.worked(1);

				List<Layer> toRemove = Lists.newArrayList();
				for (Layer layer : activeMap.getLayersInternal()) {
					if (layer.getBlackboard().contains(LayerToAdd.GEOGIG_MARKER)) {
						toRemove.add(layer);
					}
				}
				activeMap.getLayersInternal().removeAll(toRemove);

				Layer mergedLayer = new LayerToAdd("master", ftName, Styling.createTrackStyle()).createLayer(activeMap.getLayerFactory());
				activeMap.getLayersInternal().add(mergedLayer);
				activeMap.setName(ftName + " (master)");

				BranchDeleteOp deleteOp = getGeoGig().command(BranchDeleteOp.class);
				deleteOp.setName(ref.getName());
				deleteOp.call();

				deleteOp.setName(MERGE_BRANCH_NAME);
				deleteOp.call();
				monitor.worked(1);

				updateViewer();
				monitor.done();
			}

		}, false);
	}
	

	private void updateViewer() {
		PlatformGIS.asyncInDisplayThread(new Runnable() {
			public void run() {
				view.warmCache();
				view.viewer.refresh();
			}
		}, true);
	}
}
