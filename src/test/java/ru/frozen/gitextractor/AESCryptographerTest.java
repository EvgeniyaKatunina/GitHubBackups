package ru.frozen.gitextractor;

import org.junit.Assert;
import org.junit.Test;

public class AESCryptographerTest {

    @Test
    public void testCryptographer() {
        String pass = "pass";
        AESCryptographer cryptographer = new AESCryptographer();
        String encryptedPass = cryptographer.encrypt(pass);
        String decryptedPass = cryptographer.decrypt(encryptedPass);
        Assert.assertEquals(decryptedPass, pass);
    }
}
