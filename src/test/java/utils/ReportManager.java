package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class ReportManager {
    private static ExtentReports extent;

    public static ExtentReports getExtentReport() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("test-output/extent-report.html");
            spark.config().setReportName("Dashboard Performance Report");
            spark.config().setDocumentTitle("SIEC Dashboard Performance Test");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", "DEV");
            extent.setSystemInfo("Tester", "Automated Selenium Framework");
        }
        return extent;
    }
}
