package ru.frozen.gitextractor;

import java.io.Console;
import java.io.IOException;
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
    private static final String USAGE = "USAGE: .\\App repositoryName userName updateTimeINMinutes backupType " +
            "pathToBackUp";
    private static final String FILE_SYSTEM_BACKUP = "fileSystem";
    private static final String PASSWORD_PROMPT = "Enter password:";
    private static final String GITHUB_EXTRACT_FAIL_MSG = "Failed to extract from github.";

    public static void main(String[] args) {
        try {
            String user = args[1];
            String reponame = args[0];
            String targetFolder = args[4];
            if (args[3].equals(FILE_SYSTEM_BACKUP)) {
                Console console = System.console();
                char[] password = console.readPassword(PASSWORD_PROMPT);
                GitHubExtractor extractor = new GitHubExtractor(GITHUB_API_URL, user, new String(password));
                extractor.extract(reponame,
                        new FileSystemApplier(targetFolder + getSnapshotName()));
                final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                final Runnable applier = () -> {
                    try {
                        extractor.extract(reponame,
                                new FileSystemApplier(targetFolder + getSnapshotName()));
                    } catch (IOException e) {
                        log.error(GITHUB_EXTRACT_FAIL_MSG, e);
                    }
                };
                long delayInMinutes = Long.parseLong(args[2]);
                final ScheduledFuture<?> applierHandle = scheduler.scheduleAtFixedRate(applier, delayInMinutes,
                        delayInMinutes, MINUTES);
                scheduler.schedule(() -> {
                    applierHandle.cancel(true);
                }, delayInMinutes, MINUTES);
            } else {
                System.out.println(USAGE);
            }
        } catch (IOException e) {
            log.error(GITHUB_EXTRACT_FAIL_MSG, e);
        } catch (IndexOutOfBoundsException e) {
            System.out.println(USAGE);
        }
    }

    private static String getSnapshotName() {
        return "/snapshot" + ZonedDateTime.now().
                format(DateTimeFormatter.RFC_1123_DATE_TIME).replaceAll(":", "");
    }
}
