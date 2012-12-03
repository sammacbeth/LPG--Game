package uk.ac.imperial.lpgdash.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import uk.ac.imperial.presage2.core.util.random.Random;

public class BolzmannDistribution<C> {

	List<Pair<C, Double>> values = new LinkedList<Pair<C, Double>>();

	public BolzmannDistribution() {
		super();
	}

	public void addValue(C key, double v) {
		values.add(Pair.of(key, v));
	}

	public C keyAt(double x) {
		if (x > 1 || x < 0)
			throw new IllegalArgumentException("x must be between 0 and 1");
		double chances[] = new double[values.size()];
		int i = 0;
		double sum = 0;
		for (Pair<C, Double> v : values) {
			chances[i] = Math.exp(v.getValue());
			sum += chances[i];
			i++;
		}
		for (i = 0; i < chances.length; i++) {
			chances[i] /= sum;
			x -= chances[i];
			if (x <= 0) {
				return values.get(i).getKey();
			}
		}
		return null;
	}

	public C choose() {
		return keyAt(Random.randomDouble());
	}

}
