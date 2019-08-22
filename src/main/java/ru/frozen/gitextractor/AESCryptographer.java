package ru.frozen.gitextractor;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class AESCryptographer implements Cryptographer {

    private static final Logger log = LogManager.getLogger(AESCryptographer.class);
    private static final String key = "aesEncryptionKey";
    private static final String vector = "encryptionIntVec";
    /*private static final String key;
    private static final String vector;*/

    //private static final int KEY_LENGTH = 16;
    private static final String ENCRYPTION_ERROR = "Encryption failed.";
    private static final String DECRYPTION_ERROR = "Decryption failed.";

   /* static {
        Random r = new Random();
        StringBuilder sbKey = new StringBuilder();
        StringBuilder sbVector = new StringBuilder();
        for (int i = 0; i < KEY_LENGTH; i++) {
            sbKey.append((char) (r.nextInt(26) + 'a'));
            sbVector.append((char) (r.nextInt(26) + 'a'));
        }
        key = sbKey.toString();
        vector = sbVector.toString();
    }*/


    @Override
    public String encrypt(String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(vector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeBase64String(encrypted);
        } catch (Exception e) {
            log.error(ENCRYPTION_ERROR, e);
        }
        return null;
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(vector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception e) {
            log.error(DECRYPTION_ERROR, e);
        }

        return null;
    }
}
