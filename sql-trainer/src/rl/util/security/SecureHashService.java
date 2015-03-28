/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.security;

import java.security.MessageDigest;

import rl.util.exceptions.EncryptionException;
import sun.misc.BASE64Encoder;

/**
 * Singleton class, which supports hash value generation for given strings. It
 * is useful for checking passwords or the integrity for files.
 * 
 * @author Ruediger Lunde
 * 
 */
public final class SecureHashService {
	private static SecureHashService instance;

	/** Uses lazy instantiation to provide a unique instance. */
	public static SecureHashService getInstance() {
		if (instance == null) {
			instance = new SecureHashService();
		}
		return instance;
	}

	/** Default constructor - not accessible from outside! */
	private SecureHashService() {
	}

	/** Uses MD5 to compute a hash value for the given text. */
	public String encrypt(String plaintext) throws EncryptionException {
		String hash;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5"/* "SHA" */);
			md.update(plaintext.getBytes("UTF-8"));

			byte raw[] = md.digest();
			hash = (new BASE64Encoder()).encode(raw);
		} catch (Exception e) {
			throw new EncryptionException("Hash code generation failed.", e);
		}
		return hash;
	}
}
