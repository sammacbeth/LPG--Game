package uk.ac.imperial.lpgdash.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import uk.ac.imperial.presage2.core.util.random.Random;

public class UniformDistribution<O> {

	/**
	 * List of values with associated weighting in the distribution.
	 */
	List<Pair<O, Double>> values = new ArrayList<Pair<O, Double>>();
	/**
	 * Sum of all value weights.
	 */
	double weightSum = 0;
	/**
	 * Where our logical zero is. Allows us to deal with negative values in the
	 * distribution.
	 */
	double zero = 0;

	public UniformDistribution() {
		super();
	}

	public void addValue(O key, double v) {
		v -= zero;
		if (v < 0) {
			setZero(zero + v);
			v = 0;
		}
		values.add(Pair.of(key, v));
		weightSum += v;
	}

	public O keyAt(double x) {
		if (x > 1 || x < 0)
			throw new IllegalArgumentException("x must be between 0 and 1");
		if (weightSum == 0) {
			return values.get(Random.randomInt(values.size())).getKey();
		}
		for (Pair<O, Double> v : values) {
			double pCurrent = v.getValue() / weightSum;
			x -= pCurrent;
			if (x <= 0) {
				return v.getKey();
			}
		}
		return null;
	}

	public O choose() {
		return keyAt(Random.randomDouble());
	}

	private void setZero(double z) {
		double dz = zero - z;
		weightSum = 0;
		for (int i = 0; i < values.size(); i++) {
			Pair<O, Double> v = values.get(i);
			double wNew = v.getValue() + dz;
			values.set(i, Pair.of(v.getKey(), wNew));
			weightSum += wNew;
		}
		zero = z;
	}

}
