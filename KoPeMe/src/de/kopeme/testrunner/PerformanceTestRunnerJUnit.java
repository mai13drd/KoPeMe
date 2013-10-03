package de.kopeme.testrunner;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.win32.test.TestEventLog;
import org.junit.Assert;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.Theories.TheoryAnchor;
import org.junit.experimental.theories.internal.Assignments;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import de.kopeme.PerformanceTest;
import de.kopeme.TestExecution;
import de.kopeme.datacollection.TestResult;
import de.kopeme.paralleltests.ParallelPerformanceTest;
import de.kopeme.paralleltests.ParallelTestExecution;

/**
 * Runs a Performance Test with JUnit. The method which should be tested has to
 * got the parameter TestResult. This does not work without another runner, e.g.
 * the TheorieRunner. 
 * An alternative implementation, e.g. via Rules, which would make it possible
 * to include Theories, is not possible, because one needs to change the signature
 * of test methods to get KoPeMe-Tests running.
 * @author dagere
 *
 */
public class PerformanceTestRunnerJUnit extends BlockJUnit4ClassRunner {

	private String klasse;
	
	public PerformanceTestRunnerJUnit(Class<?> klasse)
			throws InitializationError {
		super(klasse);
		this.klasse = klasse.getName();
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		PerformanceTest a = method.getAnnotation(PerformanceTest.class);
		if (a != null)
			super.runChild(method, notifier);
		else {
			Description testBeschreibung = Description.createTestDescription(
					this.getTestClass().getJavaClass(), method.getName());
			notifier.fireTestIgnored(testBeschreibung);
		}
	}

	@Override
    protected void validateTestMethods(List<Throwable> errors) {
        for (FrameworkMethod each : computeTestMethods()) {
        	if ( each.getMethod().getParameterTypes().length != 1)
        	{
        		errors.add(new Exception("Method " + each.getName()
                + " is supposed to have exactly one parameter, who's type is TestResult"));
        	}
        	if ( each.getMethod().getParameterTypes()[0] != TestResult.class)
        	{
        		errors.add(new Exception("Method " + each.getName()
                        + " has wrong parameter Type: " + each.getMethod().getParameterTypes()[0]));
        	}
        }
    }
	
	
	@Override
	protected Statement methodInvoker(FrameworkMethod method, Object test) {
		return new PerformanceExecutionStatement(method, test);
	}

	public class PerformanceExecutionStatement extends Statement {

		private final FrameworkMethod fTestMethod;
		private Object fTarget;

		public PerformanceExecutionStatement(FrameworkMethod testMethod,
				Object target) {
			fTestMethod = testMethod;
			fTarget = target;
		}

		@Override
		public void evaluate() throws Throwable {
			TestExecution te;
			if (fTestMethod.getAnnotation(ParallelPerformanceTest.class) != null)
			{
				te = new ParallelTestExecution(fTarget.getClass(), fTarget, fTestMethod.getMethod());
			}
			else
			{
				te = new TestExecution(fTarget.getClass(), fTarget, fTestMethod.getMethod());
			}
			te.runTest();
//			String filename = klasse + "::" + fTestMethod.getName() + ".yml";
//			int executions = 5;
//			TestResult tr = new TestResult(filename, false, executions);
//			fTestMethod.invokeExplosively(fTarget, tr);
		}
	}
}
