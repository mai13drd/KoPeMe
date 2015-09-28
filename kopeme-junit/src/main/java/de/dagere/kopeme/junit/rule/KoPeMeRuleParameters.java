package de.dagere.kopeme.junit.rule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TestRule for realizing advanced parameterized performance measurements. Uses the <code>Parameters<code> and
 * <code>Parameter<code> Annotations resembling those of the Parameters runner, additionally an alternative
 * <code>WeightedParameters<code> Annotation. Cannot inject into the constructor, use field injection instead. Use
 * either <code>Parameters<code> or <code>WeightedParameters<code> method for the injection. Can be used in conjunction
 * with any standard runner (even with the Parameterized runner).
 *
 * @author krauss
 */
public class KoPeMeRuleParameters extends KoPeMeRule {

	private static final Logger LOG = LogManager.getLogger(KoPeMeRuleParameters.class);

	/**
	 * Annotation for fields of the test class which will be initialized by the method annotated by
	 * <code>Parameters</code> or <code>WeightedParameters</code>. Index range must start at 0. Default value is 0.
	 *
	 * @author krauss
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Parameter {
		int value() default 0;
	}

	/**
	 * Annotation for a method which provides parameters to be injected into the test class fields by the TestRule
	 * <code>KoPeMeRuleParameters</code>. The method has to be public and static. Return an Iterable over a set of
	 * Parameters just like for the Parameterized runner. The type of the parameters can be either an Object or an
	 * Object[]. <br>
	 * The testing will stop if the performance value of the specified collector goes beyond the totalBoundary, Thus it
	 * is possible to find the maximum load whithin a given frame of acceptable performance cost. <br>
	 * Annotation for a method which provides parameters to be injected into the test class fields by the TestRule
	 * <code>KoPeMeRuleParameters</code>. The method has to be public and static. Return an order preserving Map mapping
	 * sets of parameters to weights. The parameter type can be either an Object or an Object[]. <br>
	 * This enables testing which configuration produces the best quality weighted by their performance. This comparable
	 * value can be used to stop further assessment after an optimum has been found. To stop further testing the
	 * collector must be specified and a dropoffBoundary given: The testing will be stopped after any given parameter
	 * set has its weighted performance drop under the dropoffBoundary times the best yet achieved weighted performance.
	 * <br>
	 * Examples: (A) Acquiring the maximum Throughput: <br>
	 * The parameters set how many processes run in parallel while the weighting is equal to that number. Having more
	 * processes executed decreases the performance of a single process but the overall efficiency may rise to a global
	 * optimum and then drop again. <br>
	 * (B) Precision tradeoff: <br>
	 * The parameters are used to set the precision of complex calculations, the weighting classifies what value the
	 * given level of precision has for the case at hand.
	 *
	 * @author krauss
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Parameters {
		String name()

		default "{index}";

		String collectorname();

		long totalBoundary()

		default Long.MAX_VALUE;

		double relativeBoundary() default 0;
	}

	private final Method parametersMethod;
	private final Field[] parameterFields;

	public KoPeMeRuleParameters(final Object test) {
		super(test);
		parametersMethod = fetchParametersMethod();
		parameterFields = fetchParameterFields();
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new KoPeMeParameterStatement(base, description);
	}

	private Method fetchParametersMethod() {
		Method parametersMethod = null;
		for (final Method method : getTestObject().getClass().getMethods()) {
			if (method.isAnnotationPresent(Parameters.class)) {
				if (parametersMethod == null) {
					parametersMethod = method;
				} else {
					LOG.warn("Multiple Methods annotated with @Parameters, only one method used: {}", method.getName());
				}
			}
		}
		if (parametersMethod == null) {
			throw new RuntimeException("No Method annotated with @Parameters present!");
		}
		return parametersMethod;
	}

	private Field[] fetchParameterFields() {
		final Map<Field, Integer> parameterFieldMap = new LinkedHashMap<>();
		for (final Field field : getTestObject().getClass().getFields()) {
			if (field.isAnnotationPresent(Parameter.class)) {
				parameterFieldMap.put(field, field.getAnnotation(Parameter.class).value());
			}
		}
		final Field[] parameterFields = new Field[parameterFieldMap.size()];
		for (final Map.Entry<Field, Integer> field : parameterFieldMap.entrySet()) {
			int position = field.getValue();
			while (parameterFields[position] != null) {
				position++;
			}
			parameterFields[position] = field.getKey();
		}
		return parameterFields;
	}

	private final class KoPeMeParameterStatement extends Statement {
		private final Statement base;
		private final Description description;
		final ParameterTestResult result;

		private KoPeMeParameterStatement(final Statement base, final Description description) {
			this.description = description;
			this.base = base;
			final Parameters ann = parametersMethod.getAnnotation(Parameters.class);
			result = new ParameterTestResult(ann.totalBoundary(), ann.relativeBoundary(), ann.collectorname());
		}

		@SuppressWarnings("unchecked")
		@Override
		public void evaluate() throws Throwable {

			final Map<Object[], Double> parameterSets = fetchWeightedParameters();
			for (final Map.Entry<Object[], Double> instanceParameters : parameterSets.entrySet()) {
				final Object[] parameters = instanceParameters.getKey();
				parameterizeTest(parameters);
				final KoPeMeStandardRuleStatement standardStatement = applyBaseRule();
				if (standardStatement == null) {
					break;
				}
				standardStatement.evaluate();
				final Double weight = instanceParameters.getValue();
				if (!result.addResult(standardStatement, parameters, weight)) {
					break;
				}
			}
		}

		private KoPeMeStandardRuleStatement applyBaseRule() {
			final Statement statement = KoPeMeRuleParameters.super.apply(base, description);
			final KoPeMeStandardRuleStatement standardStatement;
			if (statement instanceof KoPeMeStandardRuleStatement) {
				standardStatement = (KoPeMeStandardRuleStatement) statement;
			} else {
				standardStatement = null;
			}
			return standardStatement;
		}

		private Map<Object[], Double> fetchWeightedParameters() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			final Object parameterObject = parametersMethod.invoke(getTestObject());
			final Map<Object[], Double> weightedParameters = new LinkedHashMap<>();
			if (parameterObject instanceof Iterable) {
				for (final Object parameter : (Iterable<?>) parameterObject) {
					if (parameter instanceof Object[]) {
						weightedParameters.put((Object[]) parameter, 1d);
					} else {
						weightedParameters.put(new Object[] {parameter}, 1d);
					}
				}
			} else if (parameterObject instanceof Map) {
				for (final Map.Entry<?, Double> parameter : ((Map<?, Double>) parameterObject).entrySet()) {
					if (parameter.getKey() instanceof Object[]) {
						weightedParameters.put((Object[]) parameter.getKey(), parameter.getValue());
					} else {
						weightedParameters.put(new Object[] {parameter.getKey()}, parameter.getValue());
					}
				}
			}
			return weightedParameters;
		}

		@SuppressWarnings("unchecked")
		private Object[] parameterizeTest(final Object[] parameters) throws Exception, IllegalAccessException {
			if (parameters.length != parameterFields.length) {
				throw new Exception("Number of Parameter fields for injection is not equal to the number of returned values of the Parameters!"
						+ " Counted fields: " + parameterFields.length + ", returned values: " + parameters.length);
			}
			for (int i = 0; i < parameterFields.length; i++) {
				parameterFields[i].set(getTestObject(), parameters[i]);
			}
			return parameters;
		}

	}
}
