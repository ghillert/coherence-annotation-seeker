import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class MyTestExecutionListener implements TestExecutionListener {
    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        TestExecutionListener.super.reportingEntryPublished(testIdentifier, entry);
        String stopwatchResult = entry.getKeyValuePairs().get("StopwatchExtension");
                System.out.println(testIdentifier.getParentId().get() + " >> " + stopwatchResult);
    }
}
