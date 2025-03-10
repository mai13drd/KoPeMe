package de.dagere.kopeme.junit3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.Finishable;
import de.dagere.kopeme.OutputStreamUtil;
import de.dagere.kopeme.PerformanceTestUtils;
import de.dagere.kopeme.TimeBoundExecution;
import de.dagere.kopeme.TimeBoundExecution.Type;
import de.dagere.kopeme.annotations.AnnotationDefaults;
import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.annotations.PerformanceTestingClass;
import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.RunConfiguration;
import de.dagere.kopeme.datastorage.SaveableTestData;
import de.dagere.kopeme.datastorage.SaveableTestData.TestErrorTestData;
import de.dagere.kopeme.kieker.KoPeMeKiekerSupport;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Base class for KoPeMe-JUnit3-Testcases.
 * 
 * @author reichelt
 *
 */
public abstract class KoPeMeTestcase extends TestCase {

   private static final Logger LOG = LogManager.getLogger(KoPeMeTestcase.class);

   private final PerformanceTest annoTestcase = AnnotationDefaults.of(PerformanceTest.class);
   private final PerformanceTestingClass annoTestClass = AnnotationDefaults.of(PerformanceTestingClass.class);

   private final boolean needToStopHart = false;
   private boolean isFinished;

   /**
    * Initializes the testcase.
    */
   public KoPeMeTestcase() {
   }

   /**
    * Initializes the testcase with its name.
    * 
    * @param name Name of the testcase
    */
   public KoPeMeTestcase(final String name) {
      super(name);
   }

   /**
    * Returns the count of warmup executions, default is 1.
    * 
    * @return Warmup executions
    */
   protected int getWarmup() {
      return annoTestcase.warmup();
   }

   /**
    * Returns the count of repetitions, default is 1.
    * 
    * @return Repetitions executions
    */
   protected int getRepetitions() {
      return annoTestcase.repetitions();
   }

   /**
    * Returns the count of real executions.
    * 
    * @return real executions
    */
   protected int getIterations() {
      return annoTestcase.iterations();
   }

   /**
    * Returns weather full data should be logged.
    * 
    * @return Weather full data should be logged
    */
   protected boolean logFullData() {
      return annoTestcase.logFullData();
   }
   
   protected boolean showStart() {
      return false;
   }
   
   protected boolean redirectToTemp() {
      return false;
   }
   
   protected boolean redirectToNull() {
      return false;
   }

   /**
    * Returns the time all testcase executions may take *in sum* in ms. -1 means unbounded; Standard is set to 120 s.
    * 
    * @return Maximal time of all test executions
    */
   protected long getMaximalTime() {
      return annoTestClass.overallTimeout();
   }

   /**
    * Gets the list of datacollectors for the current execution.
    * 
    * @return List of Datacollectors
    */
   protected DataCollectorList getDataCollectors() {
      return DataCollectorList.STANDARD;
   }

   /**
    * Should kieker monitoring be used.
    * 
    * @return
    */
   protected boolean useKieker() {
      return annoTestcase.useKieker();
   }

   @Override
   public void runBare() throws InterruptedException, IOException {
      LOG.trace("Initialize JUnit-3-KoPeMe-Testcase");

      final int warmupExecutions = getWarmup(), executionTimes = getIterations();
      final boolean fullData = logFullData();
      final long timeoutTime = getMaximalTime();

      final String testClassName = this.getClass().getName();
      final DataCollectorList datacollectors = getDataCollectors();
      final TestResult finalResult = new TestResult(testClassName, executionTimes, datacollectors, false);

      KoPeMeKiekerSupport.INSTANCE.useKieker(useKieker(), testClassName, getName());

      final Finishable finishable = new Finishable() {

         @Override
         public void run() {
            try {
               runTestCase(finalResult, warmupExecutions, executionTimes, fullData);
            } catch (final AssertionFailedError | IllegalAccessException | InvocationTargetException e) {
               e.printStackTrace();
            } catch (final Throwable e) {
               e.printStackTrace();
            }
            LOG.debug("Test-call finished");
         }

         @Override
         public void setFinished(final boolean isFinished) {
            KoPeMeTestcase.this.isFinished = isFinished;
         }

         @Override
         public boolean isFinished() {
            return isFinished;
         }
      };
      RunConfiguration configuration = new RunConfiguration(getWarmup(), getRepetitions(), showStart(), redirectToTemp(), redirectToNull(), logFullData());
      final TimeBoundExecution tbe = new TimeBoundExecution(finishable, timeoutTime, Type.METHOD, useKieker());
      try {
         final boolean finished = tbe.execute();
         if (!finished) {
            final TestErrorTestData errorTestData = SaveableTestData.createErrorTestData(getName(), getClass().getName(), finalResult, configuration);
            LOG.debug("Data created");
            PerformanceTestUtils.saveData(errorTestData);
            fail("Test took too long.");
         } else {
            PerformanceTestUtils.saveData(SaveableTestData.createFineTestData(getName(), getClass().getName(), finalResult, configuration));
         }
      } catch (final Exception e) {
         e.printStackTrace();
         fail(e.getLocalizedMessage());
      }

   }

   /**
    * Runs the whole testcase.
    * 
    * @param tr Where the results should be saved
    * @param warmupExecutions How many warmup executions should be done
    * @param executionTimes How many normal executions should be done
    * @param fullData Weather to log full data
    * @throws Throwable
    */
   private void runTestCase(final TestResult tr, final int warmupExecutions, final int executionTimes, final boolean fullData)
         throws Throwable {

      final String fullName = this.getClass().getName() + "." + getName();
      try {
         final TestResult bulkResult = new TestResult(tr.getTestcase(), executionTimes, getDataCollectors(), true);
         runMainExecution("warmup", fullName, bulkResult, warmupExecutions);
         runMainExecution("main", fullName, tr, executionTimes);
         LOG.debug("Finalizing..");
         tr.finalizeCollection();
      } catch (final AssertionFailedError t) {
         LOG.error("An error occurred; saving data and finishing");
         tr.finalizeCollection(t);
         // PerformanceTestUtils.saveData(SaveableTestData.createAssertFailedTestData(getName(), getClass().getName(), tr, true));
         throw t;
      } catch (final Throwable t) {
         LOG.error("An error occurred; saving data and finishing");
         tr.finalizeCollection(t);
         // PerformanceTestUtils.saveData(SaveableTestData.createErrorTestData(getName(), getClass().getName(), tr, true));
         throw t;
      }
   }

   /**
    * Runs the main execution of the test, i.e.useKieker the execution where performance measures are counted.
    * 
    * @param testCase Runnable that should be run
    * @param name Name of the test
    * @param tr Where the results should be saved
    * @param executionTimes How often the test should be executed
    * @throws Throwable
    */
   protected void runMainExecution(final String executionTypName, final String name, final TestResult tr, final int executionTimes) throws Throwable {
      System.gc();
      final String firstPart = "--- Starting " + executionTypName + " execution " + name + " ";
      final String firstPartStop = "--- Stopping " + executionTypName + " execution ";
      final String endPart = "/" + executionTimes + " ---";
      final int repetitions = getRepetitions();
      tr.beforeRun();
      int execution = 1;
      try {
         if (redirectToTemp()) {
            redirectToTempFile();
         } else if (redirectToNull()) {
            OutputStreamUtil.redirectToNullStream();
         }
         for (execution = 1; execution <= executionTimes; execution++) {
            if (showStart()) {
               LOG.debug(firstPart + execution + endPart);
            }
            tr.startCollection();
            runAllRepetitions(repetitions);
            tr.stopCollection();
//            tr.getValue(TimeDataCollector.class.getName());
            tr.setRealExecutions(execution);
            if (showStart()) {
               LOG.debug(firstPartStop + execution + endPart);
            }
            checkFinished();
         }
      } finally {
         OutputStreamUtil.resetStreams();
      }
      System.gc();
      Thread.sleep(1);
      LOG.debug("Executions: " + (execution - 1));
      tr.setRealExecutions(execution - 1);
   }
   
   private void redirectToTempFile() throws IOException, FileNotFoundException {
      File tempFile = Files.createTempFile("kopeme", ".txt").toFile();
      PrintStream stream = new PrintStream(tempFile);
      System.setOut(stream);
      System.setErr(stream);
   }

   private void checkFinished() throws InterruptedException {
      if (isFinished) {
         LOG.debug("Exiting finished thread: {}.", Thread.currentThread().getName());
         throw new InterruptedException("Test timed out.");
      }
      final boolean interrupted = Thread.interrupted();
      LOG.trace("Interrupt state: {}", interrupted);
      if (interrupted) {
         LOG.debug("Exiting thread.");
         throw new InterruptedException("Test was interrupted and eventually timed out.");
      }
   }

   private void runAllRepetitions(final int repetitions) throws Exception, Throwable {
      for (int repetion = 0; repetion < repetitions; repetion++) {
         setUp();
         KoPeMeTestcase.super.runTest();
         tearDown();
         if (Thread.currentThread().isInterrupted()) {
            break;
         }
      }
   }
}
