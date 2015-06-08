package com.camptocamp.sbb;

import java.awt.Color;
import java.util.List;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.Filter;

public class Styling {
	public static Style createTrackStyle() {
		return createTrackStyle(Color.RED);
	}
	public static Style createTrackStyle(Color trackColor) {
		StyleBuilder styleBuilder = SLDContent.getStyleBuilder();
		Mark mark = styleBuilder.createMark("circle", trackColor, trackColor, 1);
		PointSymbolizer stationsSymb = styleBuilder.createPointSymbolizer(styleBuilder.createGraphic(null, mark, null, 1.0, 6, 0));
		
		try {
			Filter stationFilter = CQL.toFilter("type='platform' or type='service' or type='station'");
			
			LineSymbolizer railSymb = styleBuilder.createLineSymbolizer(Color.RED, 1.2);
			Rule railRule = styleBuilder.createRule(railSymb);
			railRule.setFilter(styleBuilder.getFilterFactory().not(stationFilter));

			Style style = styleBuilder.createStyle(stationsSymb);
			List<Rule> rules = style.featureTypeStyles().get(0).rules();
			rules.get(0).setFilter(stationFilter);
			rules.add(railRule);
			return style;
		} catch (CQLException e) {
			e.printStackTrace();
			return styleBuilder.createStyle();
		}
		
	}
}
