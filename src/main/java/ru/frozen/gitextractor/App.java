package ru.frozen.gitextractor;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This is the test application for extracting a content from GitHub.
 */
public class App {

    private static final Logger log = LogManager.getLogger(App.class);

    private static final String GITHUB_API_URL = "api.github.com";
    private static final String FILE_SYSTEM_BACKUP = "fileSystem";
    private static final String PASSWORD_PROMPT = "Enter password:";

    private static final String GITHUB_EXTRACT_ERROR = "Failed to extract from github.";
    private static final String CFG_FILE_ERROR = "Error reading configuration file.";
    private static final String CFG_FILE_CONTENT_ERROR =
            "Configuration file must contain line with: repositoryName " + "userName updateTimeMinutes backupType " + "pathToBackUp.";
    private static final String CFG_FILE_MINUTES_ERROR = "Error parsing updateTimeMinutes.";
    private static final String APP_CONSOLE_LAUNCH_ERROR = "This application must be launched from console.";

    public static void main(String[] args) {
        String[] argsLine;
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            argsLine = br.readLine().split(" ");
        } catch (IOException e) {
            log.error(CFG_FILE_ERROR, e);
            throw new RuntimeException(e);
        }
        String reponame;
        String user;
        long backupPeriodMinutes;
        String backupType;
        String targetFolder;
        try {
            reponame = argsLine[0];
            user = argsLine[1];
            backupPeriodMinutes = Long.parseLong(argsLine[2]);
            backupType = argsLine[3];
            targetFolder = argsLine[4];
        } catch (IndexOutOfBoundsException e) {
            log.error(CFG_FILE_CONTENT_ERROR, e);
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            log.error(CFG_FILE_MINUTES_ERROR, e);
            throw  new RuntimeException(e);
        }
        if (backupType.equals(FILE_SYSTEM_BACKUP)) {
            Console console = System.console();
            if (console == null) {
                RuntimeException e = new RuntimeException(APP_CONSOLE_LAUNCH_ERROR);
                log.error(APP_CONSOLE_LAUNCH_ERROR, e);
                throw e;
            }
            char[] password = console.readPassword(PASSWORD_PROMPT);
            try {
                GitHubExtractor extractor = new GitHubExtractor(GITHUB_API_URL, user, new String(password));
                extractor.extract(reponame, new FileSystemApplier(targetFolder + getSnapshotName()));
                final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                final Runnable applier = () -> {
                    try {
                        extractor.extract(reponame, new FileSystemApplier(targetFolder + getSnapshotName()));
                    } catch (IOException e) {
                        log.error(GITHUB_EXTRACT_ERROR, e);
                    }
                };
                final ScheduledFuture<?> applierHandle = scheduler.scheduleAtFixedRate(applier, backupPeriodMinutes,
                        backupPeriodMinutes, MINUTES);
                scheduler.schedule(() -> {
                    applierHandle.cancel(true);
                }, backupPeriodMinutes, MINUTES);
            } catch (IOException e) {
                log.error(GITHUB_EXTRACT_ERROR, e);
            }
        }
    }

    private static String getSnapshotName() {
        return "/snapshot" + ZonedDateTime.now().
                format(DateTimeFormatter.RFC_1123_DATE_TIME).replaceAll(":", "");
    }
}
