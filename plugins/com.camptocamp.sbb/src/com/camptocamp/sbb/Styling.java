package com.camptocamp.sbb;

import java.awt.Color;
import java.util.List;

import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.locationtech.udig.style.sld.SLDContent;

public class Styling {
	public static final Color DARK_GREY = new Color(51, 51, 51);
	public static Style createTrackStyle() {
		return createTrackStyle(DARK_GREY);
	}
	public static Style createTrackStyle(Color trackColor) {
		StyleBuilder styleBuilder = SLDContent.getStyleBuilder();
		LineSymbolizer[] symbs = createTrackSymbolizer(styleBuilder, trackColor);
		Style style = styleBuilder.createStyle(symbs[0]);
		List<Rule> rules = style.featureTypeStyles().get(0).rules();
		
		Rule crossHatchRule = styleBuilder.createRule(symbs[1]);
		crossHatchRule.setMaxScaleDenominator(250000);
		rules.add(crossHatchRule);
		return style;
	}
	public static LineSymbolizer[] createTrackSymbolizer(StyleBuilder styleBuilder, Color trackColor) {
		Mark mark = styleBuilder.createMark("shape://vertline");
		mark.setStroke(styleBuilder.createStroke(trackColor, 1.5));
		Graphic graphicStroke = styleBuilder.createGraphic(null, mark , null); 
		Stroke stroke = styleBuilder.getStyleFactory().createStroke(null, null, null, null, null, null, null, null, graphicStroke);
		return new LineSymbolizer[]{
				styleBuilder.createLineSymbolizer(trackColor, 2),
				styleBuilder.createLineSymbolizer(stroke)};
	}

}
