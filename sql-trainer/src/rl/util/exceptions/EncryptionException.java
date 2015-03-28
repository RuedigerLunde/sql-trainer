/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.exceptions;

/**
 * Exception class, covering all kinds of exceptions which
 * have to do with encryption.
 * @author Ruediger Lunde
 */
@SuppressWarnings("serial")
public class EncryptionException extends Exception {
	
	public EncryptionException(String message) {
		super(message);
	}
	
	public EncryptionException(String message, Throwable cause) {
		super(message, cause);
	}
}
