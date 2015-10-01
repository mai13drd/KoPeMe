package de.dagere.kopeme.junit.rule.throughput;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class KoPeMeComplexThroughtputRule implements TestRule, IOberserveExecutionTimes {

	private final int maxsize, stepsize;
	private int currentsize;

	private final Object testObject;

	public KoPeMeComplexThroughtputRule(final int startvalue, final int stepsize, final int maxsize, final Object testObject) {
		this.stepsize = stepsize;
		this.maxsize = maxsize;
		currentsize = startvalue;
		this.testObject = testObject;
	}

	public int getCurrentSize() {
		return currentsize;
	}

	@Override
	public Statement apply(final Statement base, final Description descr) {
		if (descr.isTest()) {
			try {
				return new ComplexThroughputStatement(base, descr, testObject, currentsize, stepsize, maxsize, this);
			} catch (final Exception e) {
				e.printStackTrace();
				return base;
			}
		} else {
			return base;
		}
	}

	@Override
	public void setSize(final int size) {
		currentsize = size;
	}

}