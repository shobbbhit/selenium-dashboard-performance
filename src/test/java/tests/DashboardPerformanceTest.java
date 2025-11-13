package tests;


import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.TimeoutException;
import base.PerformanceBaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.LoginPage;

public class DashboardPerformanceTest extends PerformanceBaseTest {

    private final String baseUrl = "https://dev.sieceducation.in/";
    private final String username = "usa@siecindia.com";
    private final String password = "%DhVC9z0VW)$2";

    @Test(priority = 1)
    public void loginAndLoadDashboard() {
        testLogger.info("Opening login page: " + baseUrl);

        long navLoad = measure("Open login page", () -> {
            driver.get(baseUrl);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        });

        LoginPage login = new LoginPage(driver, wait);
        long loginTime = measure("Login and wait for dashboard", () -> {
            login.login(username, password);
            // Wait for main dashboard header element
            System.out.println("🔎 Waiting for dashboard chart element to load...");

        try {
            new WebDriverWait(driver, Duration.ofSeconds(45))
                .until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h5[contains(normalize-space(), 'Applicants Acceptances Visa Ratio')]")));

            System.out.println("✅ Dashboard chart found successfully!");

        } catch (TimeoutException e) {
            System.out.println("❌ Dashboard chart element not found within timeout.");
            takeScreenshot("dashboard_load_failed");
            throw e; // rethrow so the test still fails, but we get a screenshot
        }

        });

        takeScreenshot("post_login");
        testLogger.info("Page loadTime (performance API): " + getPageLoadTimeMs() + "ms");
        testLogger.info("DOM complete (ms): " + getDomCompleteMs());
    }

    @Test(priority = 2, dependsOnMethods = "loginAndLoadDashboard")
    public void validateAndMeasureDashboardComponents() {
        DashboardPage dash = new DashboardPage(driver, wait);

        long filterOpen = measure("Open filter panel", () -> dash.openFilters());
        takeScreenshot("filter_open");

        long applyFilter = measure("Apply filters & reload data", () -> dash.applyExampleFilters());

        long kpiCheck = measure("Verify KPI cards are present & numeric", () -> {
            boolean ok = dash.kpisPresentAndNumeric();
            if (!ok) throw new RuntimeException("KPI cards invalid or empty");
        });
        takeScreenshot("kpis");

        long tableLoad = measure("Country table load & rows", () -> {
            boolean ok = dash.tableHasRows();
            if (!ok) throw new RuntimeException("Table has zero rows");
        });
        takeScreenshot("table_rows");

        long graphRender = measure("Graph rendering", () -> {
            boolean ok = dash.graphRendered();
            if (!ok) throw new RuntimeException("Graph not rendered/empty");
        });
        takeScreenshot("graph");

        long prepost = measure("Pre/Post application counts", () -> {
            boolean ok = dash.prePostCountsNonZero();
            if (!ok) throw new RuntimeException("Pre/Post application counts are zero or missing");
        });

        testLogger.info("Page load time (performance API): " + getPageLoadTimeMs() + " ms");
        testLogger.info("DOM processing time: " + getDomCompleteMs() + " ms");

        saveNetworkHar();
        takeScreenshot("final_state");

        long max = Math.max(Math.max(filterOpen, applyFilter), Math.max(tableLoad, graphRender));
        if (max > DEFAULT_THRESHOLD_MS) {
            testLogger.fail("One or more major operations exceeded threshold (" + DEFAULT_THRESHOLD_MS + "ms). Max observed: " + max + "ms");
            Assert.fail("Performance threshold exceeded. Max timing: " + max + " ms");
        } else {
            testLogger.pass("All major operations within threshold. Max observed: " + max + "ms");
        }
    }
}
