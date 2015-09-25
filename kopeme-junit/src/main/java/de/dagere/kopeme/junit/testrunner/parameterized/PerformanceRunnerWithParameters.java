package de.dagere.kopeme.junit.testrunner.parameterized;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.TestWithParameters;

import de.dagere.kopeme.junit.testrunner.PerformanceTestRunnerJUnit;

/**
 * Implementation of a JUnit Test Runner for use with a {@link Parameterized} Runner annotated with the {@link UseParametersRunnerFactory} 
 * with value {@link ParameterizedPerformanceRunnerFactory}.
 * 
 * @author krauss
 */
public class PerformanceRunnerWithParameters extends PerformanceTestRunnerJUnit {
	
	// TODO: Implement Field Injection.
	// TODO: Alternatively extend BlockJUnit4ClassRunnerWithParameters and use a Performance Rule.
	
	private Object[] parameters;
	private String name;

	public PerformanceRunnerWithParameters(TestWithParameters test) throws InitializationError {
		super(test.getTestClass().getJavaClass());
		parameters = test.getParameters().toArray(
				new Object[test.getParameters().size()]);
		name = test.getName();
	}

	@Override
	public Object createTest() throws Exception {
		return createTestUsingConstructorInjection();
	}

	private Object createTestUsingConstructorInjection() throws Exception {
		return getTestClass().getOnlyConstructor().newInstance(parameters);
	}

	@Override
	protected String getName() {
		return name;
	}

	@Override
	protected String testName(FrameworkMethod method) {
		return method.getName() + getName();
	}

	@Override
	protected void validateConstructor(List<Throwable> errors) {
	}
	
    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return childrenInvoker(notifier);
    }
    
    @Override
    protected Annotation[] getRunnerAnnotations() {
        return new Annotation[0];
    }
}
