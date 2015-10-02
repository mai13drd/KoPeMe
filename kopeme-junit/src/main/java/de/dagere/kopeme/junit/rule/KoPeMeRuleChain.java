package de.dagere.kopeme.junit.rule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.datacollection.DataCollectorList;

/**
 * An equivalent to the JUnit {@code RuleChain} for use with {@link KoPeMeRule} or {@link KoPeMeRuleParameters}. The
 * {@code KoPeMeRule} can be added explicitly or with the convenience method {@code koPeMe} respectively
 * {@code koPeMeParameters} at the appropriate position.<br>
 * Build the {@code KoPeMeRuleChain} like a {@code RuleChain} and add the {@code koPeMe} method at the point where the
 * measurement should occur. All rules added before will be excluded from the measurement whereas all rules added after
 * will be included. If there is no rule to be excluded you can start the chain by {@code outerKoPeMe()}.
 *
 * @author krauss
 */
public class KoPeMeRuleChain extends KoPeMeRuleParameters {

	private static final Logger LOG = LogManager.getLogger(KoPeMeRuleChain.class);

	private static final KoPeMeRuleChain EMPTY_CHAIN = new KoPeMeRuleChain(Collections.<TestRule> emptyList());

	private final List<TestRule> outerRules;
	private final List<TestRule> middleRules;
	private final List<TestRule> innerRules;
	private final List<TestRule> measuredRules;

	private KoPeMeRuleChain(final List<TestRule> rules) {
		super(null);
		this.outerRules = rules;
		this.middleRules = new ArrayList<TestRule>();
		this.innerRules = new ArrayList<TestRule>();
		this.measuredRules = new ArrayList<TestRule>();
	}

	private KoPeMeRuleChain(final List<TestRule> outerRules, final List<TestRule> middleRules, final List<TestRule> innerRules, final Object testObject,
			final DataCollectorList collectors) {
		super(testObject, collectors);
		this.outerRules = outerRules;
		this.middleRules = middleRules;
		this.innerRules = innerRules;
		this.measuredRules = new ArrayList<TestRule>();
	}

	public KoPeMeRuleChain(final List<TestRule> outerRules, final List<TestRule> middleRules, final List<TestRule> innerRules, final Object testObject,
			final DataCollectorList collectors, final List<TestRule> measuredRules) {
		super(testObject, collectors);
		this.outerRules = outerRules;
		this.middleRules = middleRules;
		this.innerRules = innerRules;
		this.measuredRules = measuredRules;
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} without a {@link TestRule}. This method may be the starting point of a
	 * {@code KoPeMeRuleChain}.
	 *
	 * @return a {@code KoPeMeRuleChain} without a {@link TestRule}.
	 */
	public static KoPeMeRuleChain emptyRuleChain() {
		return EMPTY_CHAIN;
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} with a single {@link TestRule}. This method is the usual starting point of a
	 * {@code KoPeMeRuleChain}.
	 *
	 * @param outerRule the outer rule of the {@code KoPeMeRuleChain}.
	 * @return a {@code KoPeMeRuleChain} with a single {@link TestRule}.
	 */
	public static KoPeMeRuleChain outerRule(final TestRule outerRule) {
		return emptyRuleChain().around(outerRule);
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} where all following enclosed rules will be applied for each parameterization.
	 * If no further Rules are added this is equal to a {@code KoPeMeRuleParameters}.
	 *
	 * @param test The instance of the test class. Usually {@code this} does the trick.
	 * @param collectors The DataCollectorList that should be used for the measurement.
	 * @return a {@code KoPeMeRuleChain} with a single {@link TestRule}.
	 */
	public static KoPeMeRuleChain outerKoPeMeParameters(final Object test, final DataCollectorList collectors) {
		return emptyRuleChain().koPeMeParameters(test, collectors);
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} where all following enclosed rules will be applied for each parameterization.
	 * If no further Rules are added this is equal to a {@code KoPeMeRuleParameters}.
	 *
	 * @param test The instance of the test class. Usually {@code this} does the trick.
	 * @return a {@code KoPeMeRuleChain} with a single {@link TestRule}.
	 */
	public static KoPeMeRuleChain outerKoPeMeParameters(final Object test) {
		return emptyRuleChain().koPeMeParameters(test);
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} where all following enclosed rules will be applied for each performance
	 * measuring run. If no further Rules are added this is equal to a {@code KoPeMeRule}.
	 *
	 * @param test The instance of the test class. Usually {@code this} does the trick.
	 * @return a {@code KoPeMeRuleChain} with a single {@link TestRule}.
	 */
	public static KoPeMeRuleChain outerKoPeMe(final Object test, final DataCollectorList collectors) {
		return emptyRuleChain().koPeMe(test, collectors);
	}

	/**
	 * Returns a {@code KoPeMeRuleChain} where all following enclosed rules will be applied for each performance
	 * measuring run. If no further Rules are added this is equal to a {@code KoPeMeRule}.
	 *
	 * @param test The instance of the test class. Usually {@code this} does the trick.
	 * @return a {@code KoPeMeRuleChain} with a single {@link TestRule}.
	 */
	public static KoPeMeRuleChain outerKoPeMe(final Object test) {
		return emptyRuleChain().koPeMe(test);
	}

	/**
	 * Create a new {@code KoPeMeRuleChain}, which encloses the {@code nextRule} with the rules of the current
	 * {@code KoPeMeRuleChain}. Enclosing a {@code KoPeMeRuleParameters}
	 *
	 * @param enclosedRule the rule to enclose.
	 * @return a new {@code KoPeMeRuleChain}.
	 */
	public KoPeMeRuleChain around(final TestRule enclosedRule) {
		if (enclosedRule instanceof KoPeMeRuleParameters) {
			return koPeMeParameters(((KoPeMeRuleParameters) enclosedRule).getTestObject());
		}
		if (enclosedRule instanceof KoPeMeRule) {
			return koPeMe(((KoPeMeRule) enclosedRule).getTestObject());
		}
		final List<TestRule> innerChain = new ArrayList<TestRule>();
		final List<TestRule> middleChain = new ArrayList<TestRule>();
		final List<TestRule> outerChain = new ArrayList<TestRule>();
		if (getTestObject() != null) {
			innerChain.add(enclosedRule);
			innerChain.addAll(innerRules);
			middleChain.addAll(middleRules);
			outerChain.addAll(outerRules);
		} else {
			outerChain.add(enclosedRule);
			outerChain.addAll(outerRules);
		}
		return new KoPeMeRuleChain(outerChain, middleChain, innerChain, getTestObject(), getCollectors());
	}

	public KoPeMeRuleChain measure(final TestRule enclosedRule) {
		final List<TestRule> measuredChain = new ArrayList<>();
		measuredChain.add(enclosedRule);
		measuredChain.addAll(measuredRules);
		final List<TestRule> innerChain = new ArrayList<TestRule>();
		final List<TestRule> middleChain = new ArrayList<TestRule>();
		final List<TestRule> outerChain = new ArrayList<TestRule>();
		innerChain.addAll(innerRules);
		middleChain.addAll(middleRules);
		outerChain.addAll(outerRules);
		return new KoPeMeRuleChain(outerChain, middleChain, innerChain, getTestObject(), getCollectors(), measuredChain);
	}

	public KoPeMeRuleChain koPeMeParameters(final Object test, final DataCollectorList collectors) {
		return new KoPeMeRuleChain(outerRules, new ArrayList<TestRule>(), new ArrayList<TestRule>(), test, collectors);
	}

	/**
	 * Sets when to start/stop the performance measurement. Usually called last (innermost) unless some Rule should
	 * specifically be measured with each parameterized run. If this method (or alternatively explicit adding of an
	 * {@code .around(KoPeMeRuleParameters)} ) is not used the {@code KoPeMeRuleChain} will act like a {@code RuleChain}
	 *
	 * @param test The instance of the test class. Usually {@code this} does the trick.
	 * @return a new {@code KoPeMeRuleChain} with the point of performance measurement set innermost.
	 */
	public KoPeMeRuleChain koPeMeParameters(final Object test) {
		return new KoPeMeRuleChain(outerRules, new ArrayList<TestRule>(), new ArrayList<TestRule>(), test, getCollectors());
	}

	public KoPeMeRuleChain koPeMe(final Object test, final DataCollectorList collectors) {
		return new KoPeMeRuleChain(outerRules, innerRules, new ArrayList<TestRule>(), test, collectors);
	}

	public KoPeMeRuleChain koPeMe(final Object test) {
		return new KoPeMeRuleChain(outerRules, innerRules, new ArrayList<TestRule>(), test, getCollectors());
	}

	public KoPeMeRuleChain koPeMe() {
		if (getTestObject() == null) {
			throw new RuntimeException("No testObject predefined!");
		}
		return new KoPeMeRuleChain(outerRules, innerRules, new ArrayList<TestRule>(), getTestObject(), getCollectors());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Statement apply(final Statement base, final Description description) {
		Statement chainStatement;
		if (getTestObject() != null) {
			try {
				chainStatement = new KoPeMeChainStatement(base, description, getTestObject(), getCollectors(), isParameters);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			chainStatement = base;
		}
		for (final TestRule outer : outerRules) {
			chainStatement = outer.apply(chainStatement, description);
		}
		return chainStatement;
	}

	// Strange stuff to intercept whether the user wants to use the parameterized or normal variant of KoPeMe with this
	// Rule. maybe it would be better to extend TestRule directly and use privately either the one or the other Rule.
	// You'd need to overwrite two Statements (the Basic and the Parameters), or do some tricky stuff.

	private boolean isParameters = true;

	@Override
	protected Method fetchParametersMethod() {
		try {
			return super.fetchParametersMethod();
		} catch (final NoParameterException e) {
			isParameters = false;
			return null;
		}
	}

	@Override
	protected Field[] fetchParameterFields() throws NoParameterException {
		try {
			return super.fetchParameterFields();
		} catch (final NoParameterException e) {
			if (isParameters) {
				throw e;
			}
			return null;
		}
	}

	@Override
	protected Parameters fetchParametersAnnotation() {
		try {
			return super.fetchParametersAnnotation();
		} catch (final NullPointerException e) {
			return null;
		}
	}

	// The Statement itself. Needs to overwrite measured, inner and middle Rule application. Also evaluate chooses
	// whether to use basic or Parameters evaluate function.

	public class KoPeMeChainStatement extends KoPeMeParametersStatement {

		public KoPeMeChainStatement(final Statement base, final Description description, final Object testObject,
				final DataCollectorList collectors, final boolean isParameters) throws Exception {
			super(base, description, testObject, collectors);
		}

		@Override
		public void evaluate() throws Throwable {
			if (isParameters) {
				super.evaluate();
			} else {
				super.applyBaseRule(base, description);
			}
		}

		@Override
		protected Statement applyMeasuredRules(final Statement base, final Description description) {
			Statement chain = base;
			for (final TestRule measure : measuredRules) {
				chain = measure.apply(chain, description);
			}
			return chain;
		}

		@Override
		protected Statement applyInnerRules(final Statement base, final Description description) {
			Statement chain = base;
			for (final TestRule inner : innerRules) {
				chain = inner.apply(chain, description);
			}
			return chain;
		}

		@Override
		protected Statement applyMiddleRules(final KoPeMeBasicStatement koPeMeStatement, final Description description) {
			Statement middleStatement = koPeMeStatement;
			for (final TestRule middle : middleRules) {
				middleStatement = middle.apply(middleStatement, description);
			}
			return middleStatement;
		}
	}
}
