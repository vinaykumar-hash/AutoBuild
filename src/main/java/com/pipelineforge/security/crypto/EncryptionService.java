package com.pipelineforge.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;

	private final EncryptionProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public EncryptionService(EncryptionProperties properties) {
		this.properties = properties;
	}

	public String encrypt(String value) {
		if (value == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to encrypt value", ex);
		}
	}

	public String decrypt(String value) {
		if (value == null) {
			return null;
		}
		try {
			String[] parts = value.split(":", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid encrypted value format");
			}
			byte[] iv = Base64.getDecoder().decode(parts[0]);
			byte[] cipherText = Base64.getDecoder().decode(parts[1]);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] decrypted = cipher.doFinal(cipherText);
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to decrypt value", ex);
		}
	}

	private SecretKeySpec key() {
		String secret = properties.secret();
		if (secret == null || secret.length() < 32) {
			throw new IllegalStateException("Encryption secret must be at least 32 characters");
		}
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length > 32) {
			keyBytes = Arrays.copyOf(keyBytes, 32);
		}
		return new SecretKeySpec(keyBytes, ALGORITHM);
	}
}
