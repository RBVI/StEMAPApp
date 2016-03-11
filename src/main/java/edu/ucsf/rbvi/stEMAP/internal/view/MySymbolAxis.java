package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.Tick;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

class MySymbolAxis extends SymbolAxis {
	public MySymbolAxis(String title, String[] tickLabels) {
		super(title, tickLabels);
	}

	@Override
	public List refreshTicksHorizontal(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
		List ticks = new ArrayList();

		Font tickLabelFont = getTickLabelFont();
		g2.setFont(tickLabelFont);

		double size = getTickUnit().getSize();
		int count = calculateVisibleTickCount();

		double lowestTickValue = calculateLowestVisibleTickValue();

		double previousDrawnTickLabelPos = 0.0;
		double previousDrawnTickLabelLength = 0.0;
		for (int i = 0; i < count; i++) {
			double currentTickValue = lowestTickValue + (i * size);
			double xx = valueToJava2D(currentTickValue, dataArea, edge);
			String tickLabel;
			NumberFormat formatter = getNumberFormatOverride();
			if (formatter != null) {
				tickLabel = formatter.format(currentTickValue);
			} else {
				tickLabel = valueToString(currentTickValue);
			}

			// avoid to draw overlapping tick labels
			Rectangle2D bounds = TextUtilities.getTextBounds(tickLabel, g2, g2.getFontMetrics());
			double tickLabelLength = isVerticalTickLabels() ? bounds.getHeight() : bounds.getWidth();
			boolean tickLabelsOverlapping = false;
			if (i > 0) {
				double avgTickLabelLength = (previousDrawnTickLabelLength + tickLabelLength) / 2.0;
				if (Math.abs(xx - previousDrawnTickLabelPos) < avgTickLabelLength) {
					tickLabelsOverlapping = true;
				}
			}
			if (tickLabelsOverlapping) {
				tickLabel = ""; // don't draw this tick label
			} else {
				// remember these values for next comparison
				previousDrawnTickLabelPos = xx;
				previousDrawnTickLabelLength = tickLabelLength;
			}

			TextAnchor anchor;
			TextAnchor rotationAnchor;
			double angle = 0.0;
			if (isVerticalTickLabels()) {
				anchor = TextAnchor.CENTER_RIGHT;
				rotationAnchor = TextAnchor.CENTER_RIGHT;
				if (edge == RectangleEdge.TOP) {
					angle = Math.PI / 2.0;
				} else {
					angle = -Math.PI / 2.0;
				}
			} else {
				if (edge == RectangleEdge.TOP) {
					anchor = TextAnchor.BOTTOM_CENTER;
					rotationAnchor = TextAnchor.BOTTOM_CENTER;
				} else {
					anchor = TextAnchor.TOP_CENTER;
					rotationAnchor = TextAnchor.TOP_CENTER;
				}
			}
			Tick tick = new NumberTick(new Double(currentTickValue), tickLabel, anchor, rotationAnchor, angle);
			ticks.add(tick);
		}
		return ticks;
	}

	@Override
	public List refreshTicksVertical(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
		List ticks = new ArrayList();

		Font tickLabelFont = getTickLabelFont();
		g2.setFont(tickLabelFont);

		double size = getTickUnit().getSize();
		int count = calculateVisibleTickCount();

		double lowestTickValue = calculateLowestVisibleTickValue();

		double previousDrawnTickLabelPos = 0.0;
		double previousDrawnTickLabelLength = 0.0;
		for (int i = 0; i < count; i++) {
			double currentTickValue = lowestTickValue + (i * size);
			double yy = valueToJava2D(currentTickValue, dataArea, edge);
			String tickLabel;
			NumberFormat formatter = getNumberFormatOverride();
			if (formatter != null) {
				tickLabel = formatter.format(currentTickValue);
			} else {
				tickLabel = valueToString(currentTickValue);
			}

			// avoid to draw overlapping tick labels
			Rectangle2D bounds = TextUtilities.getTextBounds(tickLabel, g2, g2.getFontMetrics());
			double tickLabelLength = isVerticalTickLabels() ? bounds.getWidth() : bounds.getHeight();
			boolean tickLabelsOverlapping = false;
			if (i > 0) {
				double avgTickLabelLength = (previousDrawnTickLabelLength + tickLabelLength) / 2.0;
				if (Math.abs(yy - previousDrawnTickLabelPos) < avgTickLabelLength) {
					tickLabelsOverlapping = true;
				}
			}
			if (tickLabelsOverlapping) {
				tickLabel = ""; // don't draw this tick label
			} else {
				// remember these values for next comparison
				previousDrawnTickLabelPos = yy;
				previousDrawnTickLabelLength = tickLabelLength;
			}

			TextAnchor anchor;
			TextAnchor rotationAnchor;
			double angle = 0.0;
			if (isVerticalTickLabels()) {
				anchor = TextAnchor.BOTTOM_CENTER;
				rotationAnchor = TextAnchor.BOTTOM_CENTER;
				if (edge == RectangleEdge.LEFT) {
					angle = -Math.PI / 2.0;
				} else {
					angle = Math.PI / 2.0;
				}
			} else {
				if (edge == RectangleEdge.LEFT) {
					anchor = TextAnchor.CENTER_RIGHT;
					rotationAnchor = TextAnchor.CENTER_RIGHT;
				} else {
					anchor = TextAnchor.CENTER_LEFT;
					rotationAnchor = TextAnchor.CENTER_LEFT;
				}
			}
			Tick tick = new NumberTick(new Double(currentTickValue), tickLabel, anchor, rotationAnchor, angle);
			ticks.add(tick);
		}
		return ticks;
	}
}
