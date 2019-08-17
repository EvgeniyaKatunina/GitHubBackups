package ru.frozen.gitextractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.egit.github.core.RepositoryContents;

public class FileSystemApplier implements Applier {

	private static final Logger log = LogManager.getLogger(FileSystemApplier.class);

	private File dest;

	public FileSystemApplier(final String destination) throws IOException {
		if (destination == null)
			throw new IllegalArgumentException("Destination target must not be equals to null.");
		this.dest = new File(destination);
		if (this.dest.exists()) {
			if (dest.listFiles().length > 0) {
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
				log.warn("File '{}' will be overritten.", file.getAbsolutePath());
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

}
