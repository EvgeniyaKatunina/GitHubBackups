package ru.frozen.gitextractor;

import org.eclipse.egit.github.core.RepositoryContents;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MINUTES;

public class FileSystemApplierTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void testNullTargetFolder() throws IOException {
        new FileSystemApplier(null);
    }

    @Test(expected = IOException.class)
    public void testWrongTargetFolder() throws IOException {
        new FileSystemApplier("ABCD://Test");
    }

    @Test
    public void testProperTargetFolder() throws IOException {
        new FileSystemApplier(folder.newFolder("Test").getAbsolutePath());
        Assert.assertTrue(true);
    }

    @Test
    public void testApply() throws IOException {
        RepositoryContents testFile = new RepositoryContents().setType(RepositoryContents.TYPE_FILE).setName(
                "TestFile").setPath("TestFile");
        testFile.setContent("test");
        String path = folder.newFolder("Test").getAbsolutePath();
        new FileSystemApplier(path).apply(testFile);
        Assert.assertTrue(Files.exists(Paths.get(path + "/" + testFile.getName())));
    }

    @Test
    public void testStoreDiff() throws IOException {
        File test = folder.newFile("test.txt");
        PrintWriter pw = new PrintWriter(new FileWriter(test));
        String testLine = "test";
        pw.println(testLine);
        pw.close();
        String path = folder.getRoot().getAbsolutePath();
        new FileSystemApplier(path).storeDiff(test.toURI().toURL());
        Path diffPath = Paths.get(path + "/" + ".diff");
        Assert.assertTrue(Files.exists(diffPath));
        BufferedReader br = new BufferedReader(new FileReader(diffPath.toFile()));
        Assert.assertEquals(br.readLine(), testLine);
        br.close();
    }

    @Test
    public void testApplyProperties() throws IOException {
        String path = folder.getRoot().getAbsolutePath();
        String sha = "sha";
        String pass = "pass";
        AESCryptographer cryptographer = new AESCryptographer();
        new FileSystemApplier(path).applyProperties(sha, pass, cryptographer);
        Path properties = Paths.get(path + "/.properties");
        Assert.assertTrue(Files.exists(properties));
        BufferedReader br = new BufferedReader(new FileReader(properties.toFile()));
        Assert.assertEquals(sha, br.readLine());
        Assert.assertEquals(cryptographer.encrypt(pass), br.readLine());
        Assert.assertTrue(MINUTES.between(ZonedDateTime.parse(br.readLine()), ZonedDateTime.now()) < 1);
    }

    @Test
    public void testCheckForUpdate() throws IOException {
        String path = folder.getRoot().getAbsolutePath();
        FileSystemApplier fileSystemApplier = new FileSystemApplier(path);
        AESCryptographer aesCryptographer = new AESCryptographer();
        Assert.assertNull(fileSystemApplier.checkForUpdate(Paths.get(path).toFile(), aesCryptographer));
        File test1Props = new File(folder.newFolder("Test1"), ".properties");
        File test2Props = new File(folder.newFolder("Test2"), ".properties");
        String sha = "sha";
        String encryptedPass = aesCryptographer.encrypt("pass");
        PrintWriter pw = new PrintWriter(new FileWriter(test1Props));
        pw.println(sha);
        pw.println(encryptedPass);
        ZonedDateTime time1 = ZonedDateTime.now();
        pw.println(time1);
        pw.close();
        pw = new PrintWriter(new FileWriter(test2Props));
        pw.println(sha);
        pw.println(encryptedPass);
        ZonedDateTime time2 = ZonedDateTime.now();
        pw.println(time2);
        pw.close();
        Applier.Update update = fileSystemApplier.checkForUpdate(Paths.get(path).toFile(), aesCryptographer);
        Assert.assertEquals(time2, update.time);
    }
}
