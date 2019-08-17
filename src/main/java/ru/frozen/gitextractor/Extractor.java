package ru.frozen.gitextractor;

import java.io.IOException;

public interface Extractor {

	void extract(String repoName, Applier applier) throws IOException;
	
}
