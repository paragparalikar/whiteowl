package com.whiteowl.core.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Crypto {

	public static String encrypt(String plainText, String key)
            throws Exception {
        byte[] plainTextByte = plainText.getBytes();
        final Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, toSecretKey(key));
        byte[] encryptedByte = cipher.doFinal(plainTextByte);
        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    public static String decrypt(String encryptedText, String key)
            throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encryptedTextByte = decoder.decode(encryptedText);
        final Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, toSecretKey(key));
        byte[] decryptedByte = cipher.doFinal(encryptedTextByte);
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }
    
    public static String generateKey() throws Exception {
    	final SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
    	return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    public static SecretKey toSecretKey(String text) throws Exception {
    	final byte[] decodedKey = Base64.getDecoder().decode(text);
    	return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
	
    public static void main(String[] args) throws Exception {
    	final String key = "UXytRK5s40Sett3rj+h2dA==";
    	final String pin = "3DA57RQMUXVOBKT5E5OM75CS5RZXPXWT";
    	final String encryptedPin = Crypto.encrypt(pin, key);
    	System.out.println(encryptedPin);
    }
}
