package de.dagere.kopeme.junit.rule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This Rule gives the possibility to test performance with a rule and without a testrunner; this makes it possible to
 * use a different testrunner. Be aware that a rule-execution does measure the time needed for @Before-, @After- and
 * possibly @Rule-Executions additional to the main execution time, but not the @BeforeClass-Execution. To avoid
 * possible impacts on the measurement use @BeforeNoMeasurement, @AfterNoMeasurement and the JUnit TestRule RuleChain
 * with the KoPeMeRule as the innermost TestRule.
 *
 * @author DaGeRe
 */
public class KoPeMeRule implements TestRule {

	private static final Logger LOG = LogManager.getLogger(KoPeMeRule.class);

	/**
	 * Object has to be acquired extra from the constructor. It cannot easily be retrieved by means of the Statement or
	 * description offered in apply.
	 */
	private final Object testObject;

	public KoPeMeRule(final Object testObject) {
		this.testObject = testObject;
	}

	@Override
	public Statement apply(final Statement base, final Description descr) {
		if (descr.isTest()) {
			// FIXME WHAT THE FUCK?!
			try {
				return new KoPeMeBasicStatement(base, descr, testObject);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			return base;
		}

	}

	public Object getTestObject() {
		return testObject;
	}
}
