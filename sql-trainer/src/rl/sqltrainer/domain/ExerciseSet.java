/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.domain;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import rl.util.exceptions.EncryptionException;
import rl.util.security.Encrypter;
import rl.util.security.SecureHashService;

/**
 * Represents a set of exercises. Besides administrative data, an introduction
 * text, and the exercises itself, an exercise set maintains editing
 * information and hash values for solution encryption.
 * @author Ruediger Lunde
 */
public class ExerciseSet {
	private String course = "";
	private String lecturer = "";
	private String id = "";
	private String db = "";
	private String editor;
	private String lastEdit;
	/**
	 * Hash value to check the success of decryption while unlocking solution
	 * texts. Contains an empty string if solution texts are currently not
	 * protected.
	 */
	private String solHash;
	/**
	 * Hash value for the password currently used to protect solution texts.
	 * The empty string indicates, that no solution protection is used.
	 */
	private String passwd;
	
	private String introduction = "";
	private List<Exercise> exercises;
	
	/** Standard constructor. */
	public ExerciseSet(String editor, String lastEdit, String solHash, String passwd) {
		this.editor = editor;
		this.lastEdit = lastEdit;
		this.solHash = solHash;
		this.passwd = passwd;
		this.exercises = new ArrayList<Exercise>();
	}
	
	/** Modifies the main attributes of an exercise set. */
	public void setData(String course, String lecturer, String id, String db) {
		this.course = course;
		this.lecturer = lecturer;
		this.id = id;
		this.db = db;
	}
	
	public String getCourse() {
		return course;
	}
	public String getLecturer() {
		return lecturer;
	}
	public String getID() {
		return id;
	}
	public String getDB() {
		return db;
	}
	public void setEditor(String editor) {
		this.editor = editor;
	}
	public String getEditor() {
		return editor;
	}
	public String getLastEdit() {
		return lastEdit;
	}
	public String getSolHash() {
		return solHash;
	}
	public String getPasswd() {
		return passwd;
	}
	/**
	 * Encrypts all solution texts and stores a hash value of the provided
	 * password with the exercise set.
	 * @throws EncryptionException
	 */
	public void lock(String passwd) throws EncryptionException {
		if (this.passwd.isEmpty()) {
			String passwdHash = SecureHashService.getInstance().encrypt(passwd);
			changeEncryptionStatus(true);
			this.passwd = passwdHash;
		}
	}
	/**
	 * Checks whether the hash value of the provided password equals the hash
	 * value stored for the password within the exercise set, and tries 
	 * to decrypt the solution texts afterwards if the password proved to be
	 * correct.
	 * @throws EncryptionException
	 */
	public void unlock(String passwd) throws EncryptionException {
		if (!passwd.isEmpty()) {
			String hashVal = SecureHashService.getInstance().encrypt(passwd);
			if (this.passwd.equals(hashVal)) {
				changeEncryptionStatus(false);
				this.passwd = "";
			} else {
				throw new EncryptionException("Password incorrect.", null);
			}
		}
	}
	/**
	 * Modifies the contents of the solution strings and the solution hash
	 * value as specified.
	 * If <code>encrypt</code> is true, the method computes a hash value for
	 * the unencrypted solutions, stores it with the exercise set and encrypts
	 * the solutions. Otherwise, it tries to decrypt the solutions
	 * and checks success by means of the stored solution hash value. 
	 * @throws EncryptionException
	 */
	private void changeEncryptionStatus(boolean encrypt) throws EncryptionException {
		List<String> solutions = new ArrayList<String>();
		StringBuffer solutionString = new StringBuffer();
		for (Exercise exercise : exercises) {
			String sol = exercise.getSolution();
			if (encrypt) {
				solutionString.append(sol);
				sol = Encrypter.getInstance().encrypt(sol);
			} else {
				sol = Encrypter.getInstance().decrypt(sol);
				solutionString.append(sol);
			}
			solutions.add(sol);
		}
		String newSolHash =
			SecureHashService.getInstance().encrypt(solutionString.toString());
		if (encrypt) {
			solHash = newSolHash;
		} else if (solHash.equals(newSolHash)) {
			solHash = "";
		} else {
			throw new EncryptionException
			("Decription failed, possibly wrong algorithm or wrong key.", null);
		}
		// change exercise set only if successful completion is guaranteed.
		for (int i = 0; i < solutions.size(); i++) {
			exercises.get(i).setSolution(solutions.get(i));
		}
	}
	
	/**
	 * Tries to get an unencrypted version of the solution which is maintained
	 * for the exercise with the specified index.
	 * @throws EncryptionException
	 */
	public String getDecryptedSolution(int exerciseIdx) throws EncryptionException {
		String result = exercises.get(exerciseIdx).getSolution();
		if (!passwd.isEmpty())
			result = Encrypter.getInstance().decrypt(result);
		return result;
	}
	public void setIntro(String intro) {
		this.introduction = intro;
	}
	public String getIntro() {
		return introduction;
	}
	public void addExercise(Exercise ex) {
		exercises.add(ex);
	}
	public Exercise getExercise(int idx) {
		return exercises.get(idx);
	}
	/** Returns the number of exercises included in this exercise set. */
	public int size() {
		return exercises.size();
	}
	
	/** Checks whether a non-empty answer is maintained for one of the exercises. */
	public boolean containsAnswers() {
		for (Exercise ex : exercises)
			if (!ex.getAnswer().isEmpty())
				return true;
		return false;
	}
	
	/** Sets the answer of the specified exercise and updates last edit info. */
	public void setAnswer(int exerciseIdx, String answer) {
		Exercise ex = getExercise(exerciseIdx);
		ex.setAnswer(answer);
		String dateTime = 
			getDatePart(Calendar.DAY_OF_MONTH) +
			"." + getDatePart(Calendar.MONTH) +
            "." + getDatePart(Calendar.YEAR) +
			", " + getDatePart(Calendar.HOUR_OF_DAY) + ":" +
			getDatePart(Calendar.MINUTE);
		lastEdit = dateTime;
	}
	
	/** Helper method for date and time formatting. */
	private String getDatePart(int what) {
		Calendar cal = Calendar.getInstance();
		int number = cal.get(what);
		if (what == Calendar.MONTH)
			number++;
		DecimalFormat f = new DecimalFormat("00");
		return f.format(number);
	}
	
	/**
	 * Creates a set of labels, suitable for exercise selection.
	 * Repeated themes are omitted.
	 */
	public List<String> getExerciseLabels() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("Intro Exercise Set " + id);
		String oldTheme = null;
		for (Exercise e : exercises) {
			String theme = e.getTheme();
			String tt = "";
			if (!theme.isEmpty() && !theme.equals(oldTheme)) {
				tt = " (" + theme + ")";
				oldTheme = theme;
			}
			result.add("Exercise " + id + "." + e.getID() + tt);
		}
		return result;
	}

	/** Use only in authoring functions! */
	public List<Exercise> getExercises() {
		return exercises;
	}
}
