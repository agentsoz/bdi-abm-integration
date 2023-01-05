package io.github.agentsoz.util;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Random;

public class Util {

	/**
	 * Normalises the values of the given array, such that each value is divided
	 * by the sum of all values.
	 * 
	 * @param values
	 * @return
	 */
	public static double[] normalise(double[] values) {
		double sum = 0;
		if (values == null || values.length <= 1) {
			return values;
		}
		for (double d : values) {
			sum += Math.abs(d);
		}
		if (sum == 0) {
			return values;
		}
		double[] norm = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			norm[i] = values[i] / sum;
		}
		return norm;
	}

	/**
	 * Cumulates the values of the given array, such that in the returned array,
	 * the value at position i is the sum of all values from index 0..i in the
	 * input array.
	 * 
	 * @param values
	 * @return
	 */
	public static double[] cumulate(double[] values) {
		if (values == null || values.length <= 1) {
			return values;
		}
		double[] out = new double[values.length];
		out[0] = values[0];
		for (int i = 1; i < values.length; i++) {
			out[i] = out[i - 1] + values[i];
		}
		return out;
	}

	/**
	 * Probabilistically selects an index from the provided array, where each
	 * element of the array gives the cumulative probability associated with
	 * that index. To ensure that at least one index is selected, make sure the
	 * array is normalised, such that the cumulative probability for element
	 * values[length-1] is 1.0.
	 * 
	 * @param values
	 *            cumulative probabilities associated with the indexes
	 * @param random
	 *            the random generator to use
	 * @return the selected index in the range [0:values.length-1], or -1 if
	 *         none was selected
	 */
	static int selectIndexFromCumulativeProbabilities(double[] values, Random random) {
		int index = -1;
		double roll = random.nextDouble();
		if (values != null && values.length > 0) {
			for (int i = 0; i < values.length; i++) {
				if (roll <= values[i]) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	/**
	 * Eulicidian distance between two locations
	 *
	 * @param a
	 * @param b
	 * @return
	 */
	public static double distance(double[] a, double[] b) {

		double dx = b[1] - a[1];
		double dy = b[0] - a[0];
		return Math.sqrt(dx * dx + dy * dy);
	}


}
