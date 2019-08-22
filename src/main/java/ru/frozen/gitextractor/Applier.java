package ru.frozen.gitextractor;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;

import org.eclipse.egit.github.core.RepositoryContents;

public interface Applier {

	void apply(RepositoryContents e) throws IOException;

	Update applyProperties(String Sha, String password, Cryptographer cryptographer) throws IOException;

	Update checkForUpdate(File target, Cryptographer cryptographer) throws IOException;

	class Update {
		String lastCommitSha;
		String pass;
		ZonedDateTime time;

		Update(String sha, String pass, ZonedDateTime time) {
			this.lastCommitSha = sha;
			this.pass = pass;
			this.time = time;
		}
	}
}
