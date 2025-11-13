package base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v130.network.Network;
import org.openqa.selenium.devtools.v130.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v130.network.model.ResponseReceived;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import utils.CsvLogger;
import utils.NetworkEntry;
import utils.ReportManager;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PerformanceBaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected DevTools devTools;
    protected Queue<NetworkEntry> networkEntries = new ConcurrentLinkedQueue<>();

    // Reporting
    protected static ExtentReports extent = ReportManager.getExtentReport();
    protected ExtentTest testLogger;

    // Output paths
    protected static final String OUTPUT_DIR = "test-output";
    protected static final String HAR_OUTPUT = OUTPUT_DIR + "/network_har.json";
    protected static final String CSV_OUTPUT = OUTPUT_DIR + "/timings.csv";
    protected static final long DEFAULT_THRESHOLD_MS = 5000; // 5 seconds

    @BeforeClass
    public void setupBase() throws Exception {
        Files.createDirectories(new File(OUTPUT_DIR).toPath());

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
options.addArguments("--disable-notifications");
options.addArguments("--disable-cache");
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
options.addArguments("--remote-allow-origins=*");

// If running in CI (GitHub Actions), run headless
if (System.getenv("CI") != null) {
    options.addArguments("--headless=new");
    options.addArguments("--window-size=1920,1080");
}

driver = new ChromeDriver(options);


        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));

        testLogger = extent.createTest(this.getClass().getSimpleName());
        testLogger.info("Initializing ChromeDriver and DevTools...");

        // ✅ Safe DevTools initialization with fallback
        try {
            devTools = ((ChromeDriver) driver).getDevTools();
            devTools.createSession();
            devTools.send(Network.enable(java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()));
            System.out.println("✅ DevTools session started successfully.");

            // Add network listeners
            devTools.addListener(Network.requestWillBeSent(), (RequestWillBeSent ev) -> {
                try {
                    NetworkEntry entry = NetworkEntry.fromRequestEvent(
                            ev.getRequestId().toString(),
                            ev.getRequest().getUrl(),
                            ev.getRequest().getMethod(),
                            System.currentTimeMillis(),
                            ev.getRequest().getHeaders().toString());
                    networkEntries.add(entry);
                } catch (Exception ex) {
                    System.out.println("⚠️ Failed to record request: " + ex.getMessage());
                }
            });

            devTools.addListener(Network.responseReceived(), (ResponseReceived ev) -> {
                try {
                    for (NetworkEntry e : networkEntries) {
                        if (e.getRequestId().equals(ev.getRequestId().toString())) {
                            e.setResponseStatus((int) ev.getResponse().getStatus());
                            e.setResponseUrl(ev.getResponse().getUrl());
                            e.setMimeType(ev.getResponse().getMimeType());
                            e.setResponseHeaders(ev.getResponse().getHeaders().toString());
                            break;
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("⚠️ Failed to record response: " + ex.getMessage());
                }
            });

        } catch (Exception e) {
            System.out.println("⚠️ Chrome CDP version not supported yet: " + e.getMessage());
            System.out.println("⚠️ Running without full CDP network metrics (will auto-fix when Selenium adds v142).");
            testLogger.warning("⚠️ Chrome DevTools not initialized — limited metrics due to version mismatch.");
            devTools = null;
        }

        CsvLogger.initCsv(CSV_OUTPUT);
    }

    protected long measure(String label, Runnable action) {
        long start = System.currentTimeMillis();
        action.run();
        long end = System.currentTimeMillis();
        long duration = end - start;

        String msg = label + " took " + duration + " ms";
        System.out.println("⏱ " + msg);
        testLogger.info(msg);
        CsvLogger.appendRow(CSV_OUTPUT, new String[]{label, String.valueOf(duration), String.valueOf(System.currentTimeMillis())});

        if (duration > DEFAULT_THRESHOLD_MS)
            testLogger.warning("⚠️ Threshold exceeded for " + label + " (" + duration + " ms)");

        return duration;
    }

    protected long getPageLoadTimeMs() {
        Object res = ((JavascriptExecutor) driver)
                .executeScript("return window.performance.timing.loadEventEnd - window.performance.timing.navigationStart;");
        return res == null ? -1 : ((Number) res).longValue();
    }

    protected long getDomCompleteMs() {
        Object res = ((JavascriptExecutor) driver)
                .executeScript("return window.performance.timing.domComplete - window.performance.timing.domLoading;");
        return res == null ? -1 : ((Number) res).longValue();
    }

    public String takeScreenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String path = OUTPUT_DIR + "/" + name + ".png";
            File dest = new File(path);
            Files.copy(src.toPath(), dest.toPath());
            testLogger.addScreenCaptureFromPath(path);
            return path;
        } catch (Exception e) {
            testLogger.warning("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    protected void saveNetworkHar() {
        try (FileWriter fw = new FileWriter(HAR_OUTPUT)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            fw.write(gson.toJson(networkEntries));
            testLogger.info("Network HAR saved: " + HAR_OUTPUT);
        } catch (Exception e) {
            testLogger.warning("Failed to save HAR: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void teardownBase() {
        try {
            saveNetworkHar();
            extent.flush();
        } catch (Exception ignored) {}
        if (driver != null) driver.quit();
    }
}
