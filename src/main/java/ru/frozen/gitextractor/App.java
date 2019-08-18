package ru.frozen.gitextractor;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.concurrent.TimeUnit.*;

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
            "Configuration file must contain line with: repositoryName " + "userName updateTime updateTimeUnit " +
                    "backupType " + "pathToBackUp.";
    private static final String CFG_FILE_MINUTES_ERROR = "Error parsing updateTimeMinutes.";
    private static final String APP_CONSOLE_LAUNCH_ERROR = "This application must be launched from console.";
    private static final String BACKUP_PERIOD_UNIT_ERROR =
            "UpdateTimeUnit must be " + MINUTES.name() + ", " + HOURS.name() + ", " + SECONDS.name() + " or " + DAYS.name() + ".";

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
        long backupPeriod;
        TimeUnit timeUnit;
        String backupType;
        String targetFolder;
        try {
            reponame = argsLine[0];
            user = argsLine[1];
            backupPeriod = Long.parseLong(argsLine[2]);
            String timeUnitArg = argsLine[3];
            if (timeUnitArg.equals(SECONDS.name())) {
                timeUnit = SECONDS;
            } else if (timeUnitArg.equals(MINUTES.name())) {
                timeUnit = MINUTES;
            } else if (timeUnitArg.equals(HOURS.name())) {
                timeUnit = HOURS;
            } else if (timeUnitArg.equals(DAYS.name())) {
                timeUnit = DAYS;
            } else {
                RuntimeException e = new RuntimeException(BACKUP_PERIOD_UNIT_ERROR);
                log.error(BACKUP_PERIOD_UNIT_ERROR, e);
                throw e;
            }
            backupType = argsLine[4];
            targetFolder = argsLine[5];
        } catch (IndexOutOfBoundsException e) {
            log.error(CFG_FILE_CONTENT_ERROR, e);
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            log.error(CFG_FILE_MINUTES_ERROR, e);
            throw new RuntimeException(e);
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
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                };
                final ScheduledFuture<?> applierHandle = scheduler.scheduleAtFixedRate(applier, backupPeriod,
                        backupPeriod, timeUnit);
                scheduler.schedule(() -> {
                    applierHandle.cancel(true);
                }, backupPeriod, timeUnit);
            } catch (IOException e) {
                log.error(GITHUB_EXTRACT_ERROR, e);
            }
        }
    }

    private static String getSnapshotName() {
        return "/snapshot " + ZonedDateTime.now().
                format(DateTimeFormatter.RFC_1123_DATE_TIME).replaceAll(":", "");
    }
}
