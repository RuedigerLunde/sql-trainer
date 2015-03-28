/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import rl.util.exceptions.EncryptionException;

/**
 * Implementation of an {@link rl.util.security.Encrypter}, which uses the DES
 * algorithm.
 * 
 * @author Ruediger Lunde
 */
public class DesEncrypter extends Encrypter {
	Cipher ecipher;
	Cipher dcipher;

	/** Standard constructor. */
	DesEncrypter() throws InvalidKeyException, NoSuchAlgorithmException,
			InvalidKeySpecException, NoSuchPaddingException {
		SecretKey key = createKey();
		ecipher = Cipher.getInstance("DES");
		dcipher = Cipher.getInstance("DES");
		ecipher.init(Cipher.ENCRYPT_MODE, key);
		dcipher.init(Cipher.DECRYPT_MODE, key);
	}

	/** Encrypts the given string, using the DES algorihm. */
	public String encrypt(String str) throws EncryptionException {
		String result = null;
		try {
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");
			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);
			// Encode bytes to base64 to get a string
			result = new sun.misc.BASE64Encoder().encode(enc);
		} catch (Exception e) {
			throw new EncryptionException("String encryption failed.", e);
		}
		return result;
	}

	/** Decrypts the given string, using the DES algorihm. */
	public String decrypt(String str) throws EncryptionException {
		String result = null;
		try {
			// Decode base64 to get bytes
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);
			// Decode using utf-8
			result = new String(utf8, "UTF8");
		} catch (Exception e) {
			throw new EncryptionException("String decryption failed.", e);
		}
		return result;
	}

	/** Provides a save key for encryption. */
	protected SecretKey createKey() throws InvalidKeyException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKey result;
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		DESKeySpec spec = new DESKeySpec(new byte[] { -48, -123, 100, -23, -75,
				122, -99, -62 });
		// (new byte[] {55, 82, 103, 100, 37, 109, -33, 112});
		// (new byte[] {104, 107, 98, 109, 1, -83, -104, -82});
		// (new byte[] {100, -74, 13, -3, 19, -101, -53, 44});
		result = keyFactory.generateSecret(spec);
		return result;
	}

	// for testing...
	// public static void main(String[] args) {
	// try {
	// //SecretKey key = KeyGenerator.getInstance("DES").generateKey();
	//
	// // Create encrypter/decrypter class
	// DesEncrypter encrypter = new DesEncrypter();
	//
	// // Encrypt
	// String encrypted = encrypter.encrypt("Don't tell anybody!");
	//
	// // Decrypt
	// String decrypted = encrypter.decrypt(encrypted);
	// int i;
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	// Key generator ;-)
	// public static void main(String[] args) {
	// try {
	// SecretKey key = KeyGenerator.getInstance("DES").generateKey();
	// for (byte b : key.getEncoded())
	// System.out.print(b + ", ");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
}
