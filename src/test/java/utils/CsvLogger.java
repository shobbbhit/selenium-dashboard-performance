package utils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class CsvLogger {

    public static void initCsv(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                Files.createDirectories(Paths.get(f.getParent()));
                try (FileWriter fw = new FileWriter(f, true)) {
                    fw.append("label,duration_ms,timestamp\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void appendRow(String path, String[] row) {
        try (FileWriter fw = new FileWriter(path, true)) {
            fw.append(String.join(",", Arrays.asList(row)));
            fw.append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
