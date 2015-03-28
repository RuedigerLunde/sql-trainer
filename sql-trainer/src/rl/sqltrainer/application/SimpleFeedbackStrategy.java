/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.application;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rl.sqltrainer.domain.ExerciseSet;
import rl.sqltrainer.domain.Feedback;

/**
 * Simple strategy for providing feedbacks for SQL statement
 * exercises.
 * @author Ruediger Lunde
 */
public class SimpleFeedbackStrategy implements FeedbackStrategy {
	/** Just compares the two tables and provides some comments. */
	@Override
	public Feedback provideFeedback(ExerciseSet exSet, int exIdx,
			List<List<String>> aData, List<List<String>> sData) {
		Feedback result = new Feedback();
		StringBuffer comments = new StringBuffer();
		int colDiff = aData.get(0).size() - sData.get(0).size();
		int rowDiff = aData.size() - sData.size();
		result.setPoints(0);
		if (colDiff != 0 || rowDiff != 0) {
			if (colDiff != 0)
				comments.append("Column Test: "
						+ (colDiff>0 ? "Greater" : "Less") + " than expected.\n");
			if (rowDiff != 0)
				comments.append("Row Test: "
						+ (rowDiff>0 ? "Greater" : "Less") + " than expected.\n");
		}
		if (aData.get(0).contains(""))
			comments.append("Header Test: Columns without name exist.\n");
		if (comments.length() == 0) {
			aData.remove(0);
			sData.remove(0);
			if (!aData.equals(sData)) {
				boolean nullFound = false;
				Comparator<List<String>> comp =
					new Comparator<List<String>>(){
					public int compare(List<String> row1, List<String> row2) {
						int result = 0;
						for (int i = 0; i < row1.size() && result == 0; i++)
							result = row1.get(i).compareTo(row2.get(i));
						return result;
					}
				};
				try {
					Collections.sort(aData, comp);
					Collections.sort(sData, comp);
				} catch (NullPointerException e) {
					nullFound = true;
				}
				if (aData.equals(sData)) {
					comments.append
					("Contents Test: Order not as expected.\n");
					result.setPoints(2);
				} else if (nullFound) {
					comments.append
					("Contents Test: Values not as expected, null values found.\n");
					result.setPoints(1);
				} else {
					comments.append
					("Contents Test: Values not as expected, "
							+ "possibliy different formatting used.\n");
					result.setPoints(1);
				}
			}
		}
		if (comments.length() == 0) {
			comments.append(";-)\n");
			result.setPoints(3);
		}
		result.setComments(comments.toString());
		return result;
	}
}
