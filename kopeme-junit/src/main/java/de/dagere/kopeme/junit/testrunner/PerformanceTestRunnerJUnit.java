package de.dagere.kopeme.junit.testrunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.Finishable;
import de.dagere.kopeme.TimeBoundExecution;
import de.dagere.kopeme.TimeBoundExecution.Type;
import de.dagere.kopeme.annotations.AnnotationDefaults;
import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.annotations.PerformanceTestingClass;
import de.dagere.kopeme.datacollection.TestResult;

/**
 * Runs a Performance Test with JUnit. The method which should be tested has to got the parameter TestResult. This does not work without another runner, e.g. the TheorieRunner. An
 * alternative implementation, e.g. via Rules, which would make it possible to include Theories, is not possible, because one needs to change the signature of test methods to get
 * KoPeMe-Tests running.
 * 
 * This test runner does not measure the time before and after are taking; but time rules take to execute are added to the overall-time of the method-execution.
 * 
 * @author dagere
 * 
 */
public class PerformanceTestRunnerJUnit extends BlockJUnit4ClassRunner {

   private static final PerformanceTestingClass DEFAULTPERFORMANCETESTINGCLASS = AnnotationDefaults.of(PerformanceTestingClass.class);
   private final static Logger LOG = LogManager.getLogger(PerformanceTestRunnerJUnit.class);

   protected final Class<?> klasse;
   protected boolean logFullDataClass;
   protected FrameworkMethod method;
   protected final String filename;
   protected boolean classFinished = false;
   protected PerformanceMethodStatement currentMethodStatement;

   /**
    * Initializes a PerformanceTestRunnerJUnit
    * 
    * @param klasse Class that should be tested
    * @throws InitializationError Thrown if class can't be initialized
    */
   public PerformanceTestRunnerJUnit(final Class<?> klasse) throws InitializationError {
      super(klasse);
      this.klasse = klasse;
      filename = klasse.getName();
   }

   @Override
   public void run(final RunNotifier notifier) {
      final long start = System.nanoTime();
      PerformanceTestingClass ptc = klasse.getAnnotation(PerformanceTestingClass.class);
      if (ptc == null) {
         ptc = DEFAULTPERFORMANCETESTINGCLASS;
      }
      final Finishable testRunRunnable = new Finishable() {
         @Override
         public void run() {
            PerformanceTestRunnerJUnit.super.run(notifier);
         }

         @Override
         public boolean isFinished() {
            return false;
         }

         @Override
         public void setFinished(final boolean isFinished) {
            classFinished = isFinished;
            if (currentMethodStatement != null) {
               currentMethodStatement.setFinished(isFinished);
            }

         }
      };
      logFullDataClass = ptc.logFullData();
      // This is usually a class-wide call, therefore kieker can be set to false, because its activated per-method
      final TimeBoundExecution tbe = new TimeBoundExecution(testRunRunnable, ptc.overallTimeout(), Type.CLASS, false);
      final boolean finished = tbe.execute();
      LOG.debug("Time: " + (System.nanoTime() - start) / 10E6 + " milliseconds");
      if (!finished) {
         classFinished = true;
         LOG.debug("Not finished.");
         setTestsToFail(notifier);
      }
   }

   /**
    * Sets that tests are failed.
    * 
    * @param notifier Notifier that should be notified
    */
   protected void setTestsToFail(final RunNotifier notifier) {
      final Description description = getDescription();
      final ArrayList<Description> toBeFailed = new ArrayList<>(description.getChildren()); // all testmethods will be covered and set to failed here
      toBeFailed.add(description); // the whole test class failed
      for (final Description d : toBeFailed) {
         final EachTestNotifier testNotifier = new EachTestNotifier(notifier, d);
         testNotifier.addFailure(new TimeoutException("Test timed out because of class timeout"));
      }
   }

   @Override
   protected void validateTestMethods(final List<Throwable> errors) {
      for (final FrameworkMethod each : computeTestMethods()) {
         if (each.getMethod().getParameterTypes().length > 1) {
            errors.add(new Exception("Method " + each.getName() + " is supposed to have one or zero parameters, who's type is TestResult"));
         } else {
            if (each.getMethod().getParameterTypes().length == 1 && each.getMethod().getParameterTypes()[0] != TestResult.class) {
               errors.add(new Exception("Method " + each.getName() + " has wrong parameter Type: " + each.getMethod().getParameterTypes()[0]));
            }
         }
      }
   }

   /**
    * Gets the PerformanceJUnitStatement for the test execution of the given method.
    * 
    * @param currentMethod Method that should be tested
    * @return PerformanceJUnitStatement for testing the method
    * @throws NoSuchMethodException Thrown if the method does not exist
    * @throws SecurityException Thrown if the method is not accessible
    * @throws IllegalAccessException Thrown if the method is not accessible
    * @throws IllegalArgumentException Thrown if the method has arguments
    * @throws InvocationTargetException Thrown if the method is not accessible
    */
   private PerformanceJUnitStatement getStatement(final FrameworkMethod currentMethod) throws NoSuchMethodException, SecurityException,
         IllegalAccessException,
         IllegalArgumentException,
         InvocationTargetException {

      try {
         final Object testObject = new ReflectiveCallable() {
            @Override
            protected Object runReflectiveCall() throws Throwable {
               return createTest();
            }
         }.run();
         if (classFinished) {
            return null;
         }
         LOG.debug("Statement: " + currentMethod.getName() + " " + classFinished);

         Statement testExceptionTimeoutStatement = methodInvoker(currentMethod, testObject);

         testExceptionTimeoutStatement = possiblyExpectingExceptions(currentMethod, testObject, testExceptionTimeoutStatement);

         Statement withRuleStatement = ruleInvoker(currentMethod, testObject, testExceptionTimeoutStatement);

         final List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
         final List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
         final PerformanceJUnitStatement perfStatement = new PerformanceJUnitStatement(withRuleStatement, testObject, befores, afters);

         return perfStatement;
      } catch (final Throwable e) {
         return new PerformanceFail(e);
      }
   }
   
   private Statement ruleInvoker(final FrameworkMethod currentMethod, Object testObject, Statement testExceptionTimeoutStatement) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      final Method withRulesMethod = BlockJUnit4ClassRunner.class.getDeclaredMethod("withRules", FrameworkMethod.class, Object.class, Statement.class);
      withRulesMethod.setAccessible(true);
      final Statement withRuleStatement = (Statement) withRulesMethod.invoke(this, new Object[] { currentMethod, testObject, testExceptionTimeoutStatement });
      return withRuleStatement;
   }

   @Override
   protected Statement methodBlock(final FrameworkMethod currentMethod) {
      if (currentMethod.getAnnotation(PerformanceTest.class) == null) {
         return super.methodBlock(currentMethod);
      } else {
         return createPerformanceStatementFromMethod(currentMethod);
      }
   }

   /**
    * Creates a PerformanceStatement out of a method
    * 
    * @param currentMethod Method for which the statement should be created
    * @return The statement
    */
   protected Statement createPerformanceStatementFromMethod(final FrameworkMethod currentMethod) {
      try {
         final PerformanceJUnitStatement callee = getStatement(currentMethod);

         LOG.trace("Im methodBlock für " + currentMethod.getName());

         this.method = currentMethod;

         if (!classFinished) {
            currentMethodStatement = new PerformanceMethodStatement(callee, filename, klasse, method, logFullDataClass);
            return currentMethodStatement;
         } else {
            return new Statement() {
               @Override
               public void evaluate() throws Throwable {
                  throw new TimeoutException("Test timed out because of class timeout.");
               }
            };
         }

      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }
}
