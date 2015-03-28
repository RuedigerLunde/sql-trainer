/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.domain;

/**
 * An exercise basically consists of an ID, a type, a theme, and a question text.
 * Additionally, a user answer text as well as a solution can be stored
 * within an exercise. 
 * @author Ruediger Lunde
 */
public class Exercise {
	/** Currently, only two types are supported. */
	public static enum Type {SQL, OTHER};
	private String id;
	private Type type;
	private String theme;
	private String question;
	private String answer;
	private String solution;
	
	/**
	 * Standard constructor. Answer and solution are initialized with empty
	 * strings.
	 */
	public Exercise(String id, Type type, String theme, String question) {
		answer = "";
		solution = "";
		setData(id, type, theme, question);
	}
		
	/**
	 * Changes the main attributes of an exercise. This method has only
	 * been included for authoring purposes.
	 */
	public void setData(String id, Type type, String theme, String question) {
		this.id = id;
		this.type = type;
		this.theme = theme;
		this.question = question;
	}
	
	public String getID() {
		return id;
	}
	public Type getType() {
		return type;
	}
	public String getTheme() {
		return theme;
	}
	public String getQuestion() {
		return question;
	}
	public void setAnswer(String answer) {
		this.answer = answer;
	}
	public String getAnswer() {
		return answer;
	}
	public void setSolution(String solution) {
		this.solution = solution;
	}
	public String getSolution() {
		return solution;
	}
}
