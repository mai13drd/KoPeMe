package de.dagere.kopeme.junit.rule;

import static de.dagere.kopeme.PerformanceTestUtils.saveData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.Finishable;
import de.dagere.kopeme.TimeBoundExecution;
import de.dagere.kopeme.TimeBoundExecution.Type;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.SaveableTestData;
import junit.framework.AssertionFailedError;

/**
 * Represents an execution of all runs of one test
 * 
 * TODO: Overthink weather directly configure test runs in KoPeMeRule would be more nice
 * 
 * @author dagere
 * 
 */
public class KoPeMeStandardRuleStatement extends KoPeMeBasicStatement {

   private static final Logger LOG = LogManager.getLogger(KoPeMeStandardRuleStatement.class);

   private final TestResult tr;

   public KoPeMeStandardRuleStatement(final TestRunnables runnables, final Method method, final String filename) {
      super(runnables, method, filename);
      tr = new TestResult(method.getName(), annotation.warmupExecutions(), datacollectors);
   }

   @Override
   public void evaluate() throws Throwable {
      final Finishable finishable = new Finishable() {
         @Override
         public void run() {
            try {
               executeSimpleTest(tr);
               if (!assertationvalues.isEmpty()) {
                  tr.checkValues(assertationvalues);
               }
            } catch (IllegalAccessException | InvocationTargetException e) {
               e.printStackTrace();
            } catch (final Throwable e) {
               e.printStackTrace();
            }
         }

         @Override
         public boolean isFinished() {
            return KoPeMeStandardRuleStatement.this.isFinished;
         }

         @Override
         public void setFinished(final boolean isFinished) {
            KoPeMeStandardRuleStatement.this.isFinished = isFinished;
         }
      };

      final TimeBoundExecution tbe = new TimeBoundExecution(finishable, annotation.timeout(), Type.METHOD, annotation.useKieker());
      tbe.execute();
      LOG.info("Test {} beendet", filename);
   }

   private void executeSimpleTest(final TestResult tr) throws Throwable {
      if (!checkCollectorValidity(tr)) {
         LOG.warn("Not all Collectors are valid!");
      }
      try {
         // Run warmup
         if (annotation.warmupExecutions() > 0) {
            final TestResult deletableResult = new TestResult(method.getName(), annotation.warmupExecutions(), datacollectors);
            runMainExecution(deletableResult, "warmup execution ", annotation.warmupExecutions(), annotation.repetitions());
         }
         if (!isFinished) {
            runMainExecution(tr, "execution ", annotation.executionTimes(), annotation.repetitions());
         }
      } catch (final AssertionFailedError t) {
         tr.finalizeCollection(t);
         saveData(SaveableTestData.createAssertFailedTestData(tr.getMethodName(), filename, tr, annotation.warmupExecutions(),
               annotation.repetitions(), annotation.logFullData()));
         throw t;
      } catch (final Throwable t) {
         tr.finalizeCollection(t);
         saveData(SaveableTestData.createErrorTestData(tr.getMethodName(), filename, tr, annotation.warmupExecutions(),
               annotation.repetitions(), annotation.logFullData()));
         throw t;
      }
      tr.finalizeCollection();
      saveData(SaveableTestData.createFineTestData(tr.getMethodName(), filename, tr, annotation.warmupExecutions(),
            annotation.repetitions(), annotation.logFullData()));
   }

   public void setMethodName(final String methodName) {
      tr.setMethodName(methodName);
   }
}
