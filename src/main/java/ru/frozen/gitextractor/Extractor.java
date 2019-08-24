package ru.frozen.gitextractor;

import java.io.IOException;

public interface Extractor {

    Applier.Update extract(String repoName, Applier applier) throws IOException;

    Applier.Update update(final String repoName, final Applier applier,
                          final String lastCommitSha) throws IOException;
}
