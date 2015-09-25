package de.dagere.kopeme.junit.rule;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runners.model.Statement;

/**
 * This Rule gives the possibility to test performance with a rule and without a testrunner; this makes it possible to
 * use a different testrunner. Be aware that a rule-execution does measure the time needed for @Before-Executions
 * together with the main execution time, but not the @BeforeClass-Execution.
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
	public Statement apply(final Statement stmt, final Description descr) {
		if (descr.isTest()) {
			Class<?> testClass = testObject.getClass();
			Method testMethod = acquireMethod(descr.getMethodName(), testClass);
			TestRunnables runnables = new TestRunnables(new Runnable() {
				@Override
				public void run() {
					try {
						stmt.evaluate();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}, testClass, testObject);
			// FIXME WHAT THE FUCK?!
			return new KoPeMeStandardRuleStatement(runnables, testMethod, testClass.getName() + ".yaml"); 
		} else {
			return stmt;
		}

	}

	/**
	 * Will return just the method with the given name if available, else it looks for the method by decoding standard
	 * naming Patterns of JUnit Runners. Works for unchanged names or changes that follow the pattern of the
	 * Parameterized JUnit runner.
	 * 
	 * @param methodName Name of the method as given by the runner
	 * @param testClass Class containing the method
	 * @return The Method with the given Name
	 */
	private Method acquireMethod(final String methodName, Class<?> testClass) {
		Method testMethod = null;
		try {
			// testClass = Class.forName(descr.getClassName());
			testMethod = testClass.getMethod(methodName);
		} catch (NoSuchMethodException wrongName) {
			if (methodName.contains("[") && methodName.contains("]")) {
				try {
					testMethod = testClass.getMethod(methodName.substring(0, methodName.lastIndexOf("[")));
				} catch (NoSuchMethodException stillWrongName) {
					wrongName.printStackTrace();
				}
			} else {
				wrongName.printStackTrace();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return testMethod;
	}
}
