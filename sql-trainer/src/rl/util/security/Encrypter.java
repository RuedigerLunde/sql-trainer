/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.security;

import rl.util.exceptions.EncryptionException;

/**
 * Singleton class, providing a unique access point for encryption and decryption
 * of strings. 
 * @author Ruediger Lunde
 *
 */
public abstract class Encrypter {
	/** Maintains the encrypter in use. */
	private static Encrypter instance;
	
	/**
	 * Uses reflection to look for a concrete implementation of the
	 * encryption interface. If no suitable implementation is found, an
	 * <code>EncryptionException</code> is thrown. In this prototypical
	 * implementation, the name of the class to be used is hard-coded.
	 */
	public static Encrypter getInstance() throws EncryptionException {
		if (instance == null) {
			try {
				Class<?> eclass = Class.forName("rl.util.security.DesEncrypter");
				instance = (Encrypter) eclass.newInstance();
			} catch (Exception e) {
	    		throw new EncryptionException("Encrypter creation failed.", e);
	    	}
		}
		return instance;
	}
	/** Abstract interface method, which has to be overridden by implementations. */
	public abstract String encrypt(String str) throws EncryptionException;
	/** Abstract interface method, which has to be overridden by implementations. */
	public abstract String decrypt(String str) throws EncryptionException;
}
