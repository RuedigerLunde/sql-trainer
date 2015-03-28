/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.application;

import java.util.List;

import rl.sqltrainer.domain.ExerciseSet;
import rl.sqltrainer.domain.Feedback;

/**
 * Defines an interface for feedback providers.
 * @author Ruediger Lunde
 */
public interface FeedbackStrategy {
	/**
	 * Returns a feedback for a given answer.
	 * @param exSet An exercise set.
	 * @param exIdx Index of the exercise within the specified exercise set
	 *        which contains the answer.
	 * @param aData Table corresponding to the given answer
	 *        (only for exercises of type SQL).
	 * @param sData Table corresponding to the exercise solution
	 *        (only for exercises of type SQL).
	 */
	public Feedback provideFeedback(ExerciseSet exSet, int exIdx,
			List<List<String>> aData, List<List<String>> sData);
}
