package de.dagere.kopeme.junit.rule.throughput;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This rule enables measuring, how often a method can be called meeting certain performance requirements. The method is
 * called as often as given by the parameters, where the execution times increase every call. The first test execution,
 * where the *sum* of a performance measurement does not meet one assertion, is marked as failed.
 *
 * @author reichelt
 */
public class KoPeMeThroughputRule implements TestRule {

	private final int maxsize, stepsize;

	private final Object testObject;

	public KoPeMeThroughputRule(final int stepsize, final int maxsize, final Object testObject) {
		this.stepsize = stepsize;
		this.maxsize = maxsize;
		this.testObject = testObject;
	}

	@Override
	public Statement apply(final Statement base, final Description descr) {
		if (descr.isTest()) {
			try {
				return new ThroughputStatement(base, descr, testObject, stepsize, maxsize);
			} catch (final Exception e) {
				e.printStackTrace();
				return base;
			}
		} else {
			return base;
		}
	}

}
