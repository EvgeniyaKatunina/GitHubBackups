package ru.frozen.gitextractor;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the test application for extracting a content from GitHub.
 *
 */
public class App {
	
	private static final Logger log = LogManager.getLogger(App.class);
	
    public static void main(String[] args) {
    	try {
    		String url = "api.github.com";
    		String user = "userName";
    		String pass = "passName";
    		String reponame = "repoName";
    		String targetfolder = "myFolder";
			new GitHubExtractor(url, user, pass).extract(reponame, new FileSystemApplier(targetfolder));
		} catch (IOException e) {
			log.error("Failed to extract from github.", e);
		}

    }
    
}
