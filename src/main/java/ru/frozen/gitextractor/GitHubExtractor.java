package ru.frozen.gitextractor;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;

public class GitHubExtractor implements Extractor {

    private static final Logger log = LogManager.getLogger(App.class);

    private String url;
    private String username;
    private String password;
    private GitHubClient client;
    private ContentsService contentService;
    private RepositoryService repositoryService;
    private CommitService commitService;

    public GitHubExtractor(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private synchronized void init() {
        client = new GitHubClient(url);
        client.setCredentials(username, password);
        contentService = new ContentsService(client);
        repositoryService = new RepositoryService(client);
        commitService = new CommitService(client);
    }

    @Override
    public void extract(final String repoName, final Applier applier) throws IOException {
        init();
        Repository repository = repositoryService.getRepository(username, repoName);
        extract(repository, contentService.getContents(repository), applier);
        List<RepositoryCommit> commits = commitService.getCommits(repository);
        String headSha = commits.get(0).getSha();
        applier.applyProperties(headSha);
    }

    private void extract(final Repository repository, final List<RepositoryContents> list, final Applier applier) {
        list.stream().forEach(e -> {
            if (RepositoryContents.TYPE_FILE.equals(e.getType())) {
                try {
                    contentService.getContents(repository, e.getPath()).stream().forEach(c -> {
                        try {
                            applier.apply(c);
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    });
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            } else if (RepositoryContents.TYPE_DIR.equals(e.getType())) {
                try {
                    extract(repository, contentService.getContents(repository, e.getPath()), applier);
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }
        });
    }

}
