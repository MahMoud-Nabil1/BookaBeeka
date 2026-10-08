package com.system.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class BookaBeekaApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(BookaBeekaApplication.class, args);
    }

    /**
     * Automatically loads .env file into JVM System properties if present.
     * System environment variables (e.g. from production deployments) take precedence.
     */
    private static void loadDotenv() {
        Path[] possiblePaths = new Path[]{
                Paths.get(".env"),
                Paths.get("Backend", ".env"),
                Paths.get("..", ".env")
        };

        for (Path path : possiblePaths) {
            File file = path.toFile();
            if (file.exists() && file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();

                        // Strip optional surrounding quotes ("..." or '...')
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }

                        // Set property if not already set by OS environment or command line
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                } catch (Exception ignored) {
                }
                break; // Found and loaded .env file
            }
        }
    }
}
