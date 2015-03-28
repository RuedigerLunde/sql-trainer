/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.domain;

/**
 * A feedback refers to a given answer and consists of a string with
 * comments and optionally a quantitative measure of correctness.
 * @author Ruediger Lunde
 */
public class Feedback {
	/** Contains comments, separated by new-line. */
	private String comments;
	/** Value -1 denotes no value available. */
	private int points;
	
	public Feedback() {
		comments = "";
		points = -1;
	}
	
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		this.points = points;
	}
	
	public String toString() {
		if (points > 0)
			return comments + "\nPoints: " + points;
		else
			return comments;
	}
}
