package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.Color;
import java.awt.Paint;

import org.jfree.chart.renderer.PaintScale;

public class ColorScale implements PaintScale {
	
	private final double lowerBound;
	private final double upperBound;
	private final Color lowerColor;
	private final Color zeroColor;
	private final Color upperColor;
	private final Color nanColor;
	
	private static double EPSILON = 1e-30;

	public ColorScale(final double lowerBound,
					  final double upperBound,
					  final Color lowerColor,
					  final Color zeroColor,
					  final Color upperColor,
					  final Color nanColor) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.lowerColor = lowerColor;
		this.zeroColor = zeroColor;
		this.upperColor = upperColor;
		this.nanColor = nanColor;
	}

	@Override
	public double getLowerBound() {
		return lowerBound;
	}

	@Override
	public double getUpperBound() {
		return upperBound;
	}
	
	public Color getLowerColor() {
		return lowerColor;
	}
	
	public Color getZeroColor() {
		return zeroColor;
	}
	
	public Color getUpperColor() {
		return upperColor;
	}
	
	public Color getNanColor() {
		return nanColor;
	}

	@Override
	public Paint getPaint(double value) {
		if (Double.isNaN(value))
			return nanColor;
		
		return getPaint(value, lowerBound, upperBound, lowerColor, zeroColor, upperColor);
	}
	
	public static Paint getPaint(final double value,
								 final double lowerBound,
								 final double upperBound,
								 final Color lowerColor,
								 final Color zeroColor,
								 final Color upperColor) {
		final boolean hasZero = lowerBound < -EPSILON && upperBound > EPSILON && zeroColor != null;
		// System.out.println("getPaint("+value+","+lowerBound+","+upperBound+","+lowerColor+","+zeroColor+","+upperColor+")");
		
		if (hasZero && value < EPSILON && value > -EPSILON)
			return zeroColor;
		
		final Color color = value < 0.0 ? lowerColor : upperColor;

		// Linearly interpolate the value
		final double f = value < 0.0 ?
				invLinearInterp(value, lowerBound, 0) : invLinearInterp(value, 0, upperBound);
		float t = (float) (value < 0.0 ? linearInterp(f, 0.0, 1.0) : linearInterp(f, 1.0, 0.0));
		
		// Make sure it's between 0.0-1.0
		t = Math.max(0.0f, t);
		t = Math.min(1.0f, t);
		return interpolate(zeroColor, color, t);
	}

	/**
	 * Computes an inverse linear interpolation, returning an interpolation
	 * fraction. Returns 0.5 if the min and max values are the same.
	 * @param x the interpolated value
	 * @param min the minimum value (corresponds to f==0)
	 * @param min the maximum value (corresponds to f==1)
	 * @return the inferred interpolation fraction
	 */
    private static double invLinearInterp(final double x, final double min, final double max) {
        final double denom = max - min;
        return (denom < EPSILON && denom > -EPSILON ? 0 : (x - min) / denom);
    }

    /**
	 * Computes a linear interpolation between two values.
	 * @param f the interpolation fraction (typically between 0 and 1)
	 * @param min the minimum value (corresponds to f==0)
	 * @param max the maximum value (corresponds to f==1)
	 * @return the interpolated value
	 */
	private static double linearInterp(final double f, final double min, final double max) {
		return min + f * (max - min);
	}

	private static Color interpolate(Color b, Color a, float t) {
		float[] acomp = a.getRGBComponents(null);
		float[] bcomp = b.getRGBComponents(null);
		float[] ccomp = new float[4];

		for (int i=0; i < 4; i++) {
			ccomp[i] = acomp[i] + (bcomp[i]-acomp[i])*t;
		}

		return new Color(ccomp[0], ccomp[1], ccomp[2], ccomp[3]);
	}
}
