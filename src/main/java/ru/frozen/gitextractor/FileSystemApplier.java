package ru.frozen.gitextractor;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;
import org.eclipse.egit.github.core.RepositoryContents;

public class FileSystemApplier implements Applier {

    private static final Logger log = LogManager.getLogger(FileSystemApplier.class);

    private File dest;

    public FileSystemApplier(String destination) throws IOException {
        if (destination == null) throw new IllegalArgumentException("Destination target must not be equals to null.");
        this.dest = new File(destination);
        if (this.dest.exists()) {
            File [] files = dest.listFiles();
            if (files != null && files.length > 0) {
                log.warn("The destination folder '{}' is not empty.", destination);
            }
        } else {
            if (!dest.mkdirs()) {
                throw new IOException(String.format("Cannot create destination folder '%s'.", destination));
            }
        }
    }

    @Override
    public void apply(RepositoryContents e) throws IOException {
        log.info("Applying {}.", e.getName());
        File file = new File(dest, e.getPath());
        if (RepositoryContents.TYPE_FILE.equals(e.getType())) {
            if (file.exists()) {
                log.warn("File '{}' will be ovewritten.", file.getAbsolutePath());
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            Path path = Paths.get(file.getAbsolutePath());
            Files.write(path, Base64.getMimeDecoder().decode(e.getContent()));
        } else if (RepositoryContents.TYPE_DIR.equals(e.getType())) {
            if (!file.mkdirs()) {
                log.warn("Cannot create destination folder '{}'.", file.getAbsolutePath());
            }
        }
    }

    @Override
    public void storeDiff(URL url) throws IOException {
        String fileName = ".diff";
        log.info("Applying {}.", fileName);
        InputStream is = new URL(url.toString()).openStream();
        String diff = IOUtils.toString(new InputStreamReader(is));
        File file = new File(dest, fileName);
        PrintWriter pw = new PrintWriter(new FileWriter(file));
        if (diff == null || diff.isEmpty()) {
            pw.println("There are no changes.");
        } else {
            pw.println(diff);
        }
        pw.close();
    }

    @Override
    public Update applyProperties(String sha, String password, Cryptographer cryptographer) throws IOException {
        String fileName = ".properties";
        log.info("Applying {}.", fileName);
        File file = new File(dest, fileName);
        if (file.exists()) {
            log.warn("File '{}' will be ovewritten.", file.getAbsolutePath());
        } else {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        PrintWriter pw = new PrintWriter(file.getAbsolutePath());
        pw.println(sha);
        pw.println(cryptographer.encrypt(password));
        ZonedDateTime now = ZonedDateTime.now();
        pw.println(now);
        pw.close();
        return new Update(sha, password, now);
    }

    Update lastUpdate;

    @Override
    public Update checkForUpdate(File target, Cryptographer cryptographer) {
        lastUpdate = null;
        File[] files = target.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    Files.walkFileTree(f.toPath(), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            File f = new File(dir.toString());
                            File[] properties = f.listFiles(x -> x.getName().equals(".properties"));
                            if (properties != null && properties.length > 0) {
                                BufferedReader br = new BufferedReader(new FileReader(properties[0].getAbsolutePath()));
                                String commitSha = br.readLine();
                                String encryptedPass = br.readLine();
                                ZonedDateTime zonedDateTime = ZonedDateTime.parse(br.readLine());
                                if (lastUpdate == null || zonedDateTime.compareTo(lastUpdate.time) > 0) {
                                    lastUpdate = new Update(commitSha, encryptedPass, zonedDateTime);
                                }
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return null;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            return null;
                        }
                    });
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }


        if (lastUpdate != null) {
            lastUpdate.pass = cryptographer.decrypt(lastUpdate.pass);
        }
        return lastUpdate;
    }

}
