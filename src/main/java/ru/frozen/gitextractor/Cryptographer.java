package ru.frozen.gitextractor;

public interface Cryptographer {

    String encrypt(String value);

    String decrypt(String encrypted);
}
