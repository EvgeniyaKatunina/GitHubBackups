package ru.frozen.gitextractor;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryContents;

public interface Applier {

	void apply(RepositoryContents e) throws IOException;

	void applyProperties(String Sha) throws IOException;
}
