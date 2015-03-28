/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import rl.sqltrainer.application.SQLTrainer;
import rl.sqltrainer.domain.Exercise;
import rl.sqltrainer.domain.Exercise.Type;
import rl.sqltrainer.domain.ExerciseSet;
import rl.sqltrainer.domain.Feedback;
import rl.sqltrainer.gui.SQLTrainerModel.EventType;
import rl.sqltrainer.persistence.AsciiFileHandler;
import rl.util.exceptions.EncryptionException;
import rl.util.exceptions.ErrorHandler;
import rl.util.exceptions.PersistenceException;
import rl.util.gui.TextEditorDialog;
import rl.util.persistence.PropertyManager;

/**
 * This controller is responsible for the execution of those commands, which
 * change the state of the application. It is also responsible for catching
 * exceptions, coming up from below.
 * 
 * @author Ruediger Lunde
 */
class SQLTrainerController implements ActionListener {

	SQLTrainer application;
	private SQLTrainerModel model;
	private SQLTrainerFrame view;
	JFileChooser exerciseFileChooser;
	private TextEditorDialog textEditor;
	private boolean connDataChanged;

	/** Standard constructor */
	SQLTrainerController(SQLTrainer app) {
		application = app;
		connDataChanged = true;
	}

	public void setModel(SQLTrainerModel model) {
		this.model = model;
	}

	public void setFrame(SQLTrainerFrame frame) {
		this.view = frame;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == Commands.LOAD_EXERCISE_SET_CMD) {
			view.clearLog();
			clearTables();
			loadExerciseSet();
		} else if (e.getActionCommand() == Commands.SAVE_EXERCISE_SET_CMD) {
			saveExerciseSet();
		} else if (e.getActionCommand() == Commands.EXIT_CMD) {
			view.storeSessionAndExit();
		} else if (e.getActionCommand() == Commands.PREV_EXERCISE_CMD) {
			view.exerciseCombo.setSelectedIndex(view.exerciseCombo
					.getSelectedIndex() - 1);
		} else if (e.getActionCommand() == Commands.NEXT_EXERCISE_CMD) {
			view.exerciseCombo.setSelectedIndex(view.exerciseCombo
					.getSelectedIndex() + 1);
		} else if (e.getActionCommand() == Commands.EXECUTE_CMD) {
			executeStatement(false);
			view.mainTabbedPane.setSelectedComponent(view.resultTableTab);
		} else if (e.getActionCommand() == Commands.FEEDBACK_CMD) {
			executeStatement(true);
			view.neTabbedPane.setSelectedComponent(view.feedbackTab);
		} else if (e.getActionCommand() == Commands.LOCK_SOLUTIONS_CMD) {
			lockSolutions();
		} else if (e.getActionCommand() == Commands.NEW_EXERCISE_SET_CMD) {
			newExerciseSet();
		} else if (e.getActionCommand() == Commands.EDIT_EXERCISE_SET_CMD) {
			editExerciseSet();
		} else if (e.getActionCommand() == Commands.MODIFY_EXERCISE_CMD) {
			editExercise(false);
		} else if (e.getActionCommand() == Commands.INSERT_EXERCISE_CMD) {
			editExercise(true);
		} else if (e.getActionCommand() == Commands.DELETE_EXERCISE_CMD) {
			deleteExercise();
		} else if (e.getActionCommand() == Commands.MOVE_UP_EXERCISE_CMD) {
			moveExercise(true);
		} else if (e.getActionCommand() == Commands.MOVE_DOWN_EXERCISE_CMD) {
			moveExercise(false);
		} else if (e.getActionCommand() == Commands.ENABLE_AUTHORING_CMD) {
			view.updateEnableState();
		} else if (e.getActionCommand() == Commands.USE_PLATFORM_LAF_CMD) {
			view.usePlatformLookAndFeel(((AbstractButton) e.getSource())
					.isSelected());
		} else if (e.getActionCommand() == Commands.INCREASE_FONT_CMD) {
			view.setFontScale(view.getFontScale() + 0.2f);
		} else if (e.getActionCommand() == Commands.DECREASE_FONT_CMD) {
			view.setFontScale(view.getFontScale() - 0.2f);
		} else if (e.getActionCommand() == Commands.SHOW_ABOUT_CMD) {
			showAboutDialog();
		} else if (e.getActionCommand() == Commands.SELECT_EXERCISE_CMD) {
			changeExerciseSelection(Math.max(-1,
					view.exerciseCombo.getSelectedIndex() - 1));
		} else if (e.getActionCommand() == Commands.SELECT_DB_CMD) {
			changeDB((String) view.dbCombo.getSelectedItem());
		}
	}

	public boolean updateCurrExerciseData(boolean confirmUnsaved) {
		boolean result = true;
		application.updateCurrExercise(view.answerArea.getText(),
				view.solutionArea.getText());
		if (confirmUnsaved
				&& application.hasUnsavedChanges()
				&& JOptionPane.showConfirmDialog(view,
						"Unsaved changes exist, continue anyway?", "Confirm",
						JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
			result = false;
		}
		return result;
	}

	private File getCorrespondingHtmlFile(File xmlFile) {
		File htmlFile = null;
		String fName = xmlFile.getPath();
		if (fName.contains(".xml"))
			htmlFile = new File(fName.replace(".xml", ".html"));
		return htmlFile;
	}

	private void showInViewer(File htmlFile) throws IOException {
		if (htmlFile != null) {
			String text = new AsciiFileHandler().readFile(htmlFile);
			text = text.replace("http-equiv=\"Content-Type\"", ""); // bug in
																	// JEditorPane!
			view.htmlViewerArea.setText(text);
			view.htmlViewerArea.setSelectionStart(0);
			view.htmlViewerArea.setSelectionEnd(1);
		} else {
			view.htmlViewerArea.setText("");
		}
	}

	public void loadExerciseSet() {
		File xmlFile = null;
		boolean changed = false;
		try {
			if (exerciseFileChooser == null)
				exerciseFileChooser = createFileChooser();
			if (updateCurrExerciseData(true)
					&& exerciseFileChooser.showDialog(view, "Load Exercises") == JFileChooser.APPROVE_OPTION) {
				changed = true;
				xmlFile = exerciseFileChooser.getSelectedFile();
				String path = xmlFile.getPath();
				if (!path.contains("."))
					xmlFile = new File(path + ".xml");
				application.loadExerciseSet(xmlFile);
				File htmlFile = getCorrespondingHtmlFile(xmlFile);
				if (htmlFile != null && htmlFile.exists())
					showInViewer(htmlFile);
			}
		} catch (PersistenceException e) {
			RuntimeException re = new RuntimeException(
					"Could not load exercise set.", e);
			ErrorHandler.getInstance().handleError(re);
		} catch (IOException e) {
			RuntimeException re = new RuntimeException(
					"Loading file into HTML viewer failed.", e);
			ErrorHandler.getInstance().handleError(re);
		} catch (Exception e) {
			RuntimeException re = new RuntimeException(
					"While loading exercise set...", e);
			ErrorHandler.getInstance().handleError(re);
		}
		if (changed)
			afterExerciseSetChange(-1);
	}

	public void saveExerciseSet() {
		File xmlFile = null;
		try {
			if (exerciseFileChooser == null)
				exerciseFileChooser = createFileChooser();
			if (exerciseFileChooser.showDialog(view, "Save Exercises") == JFileChooser.APPROVE_OPTION) {
				xmlFile = exerciseFileChooser.getSelectedFile();
				String path = xmlFile.getPath();
				if (!path.contains("."))
					xmlFile = new File(path + ".xml");
				if (!xmlFile.exists()
						|| JOptionPane.showConfirmDialog(view,
								"File exists, overwrite?", "Confirm",
								JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
					updateCurrExerciseData(false);
					File htmlFile = getCorrespondingHtmlFile(xmlFile);
					application.saveExerciseSet(xmlFile, htmlFile);
					showInViewer(htmlFile);
					if (htmlFile != null) {
						view.mainTabbedPane
								.setSelectedComponent(view.htmlViewerTab);
					}
				}
			}
		} catch (PersistenceException e) {
			RuntimeException re = new RuntimeException(
					"Could not save exercise set.", e);
			ErrorHandler.getInstance().handleError(re);
		} catch (IOException e) {
			RuntimeException re = new RuntimeException(
					"Loading file into HTML viewer failed.", e);
			ErrorHandler.getInstance().handleError(re);
		} catch (Exception e) {
			RuntimeException re = new RuntimeException(
					"While saving exercise set...", e);
			ErrorHandler.getInstance().handleError(re);
		}
	}

	private JFileChooser createFileChooser() {
		JFileChooser result;
		String dirName = PropertyManager.getInstance().getStringValue(
						"directory.exercises", "exercises");
		File dir = new File(dirName);
		// combine relative path names with application data path
		if (!dir.isAbsolute())
			dir = new File(PropertyManager.getAppDataDirectory(), dirName);
		result = new JFileChooser(dir);
		FileFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
				"Exercise Sets (xml)", "xml");
		result.setFileFilter(filter);
		view.registerComponent(result);
		return result;
	}

	private void afterExerciseSetChange(int exerciseIdx) {
		if (application.getExerciseSet() != null) {
			view.mainTabbedPane.setSelectedComponent(view.schemaTab);
			while (application.getExerciseSet().getEditor().isEmpty()) {
				String editor = JOptionPane.showInputDialog(view, "Your name:",
						"Input", JOptionPane.QUESTION_MESSAGE);
				if (editor != null)
					application.getExerciseSet().setEditor(editor);
			}
		}
		model.fireModelEvent(EventType.EXERCISE_SET_CHANGED);
		application.setCurrExercise(exerciseIdx);
		model.fireModelEvent(EventType.CURR_EXERCISE_CHANGED);
	}

	// only to be called by the view!
	public void changeExerciseSelection(int exerciseIdx) {
		updateCurrExerciseData(false);
		application.setCurrExercise(exerciseIdx);
		model.fireModelEvent(EventType.CURR_EXERCISE_CHANGED);
	}

	/**
	 * Updates connection data and loads a new schema image if necessary.
	 */
	public void changeDB(String newDBName) {
		try {
			if (application.setDB(newDBName)) {
				connDataChanged = true;
				model.fireModelEvent(EventType.DB_INFO_CHANGED);
			}
		} catch (javax.imageio.IIOException e) {
			RuntimeException re = new RuntimeException(
					"Reading database information failed.", e);
			ErrorHandler.getInstance().handleError(re);
		} catch (Exception e) {
			RuntimeException re = new RuntimeException(
					"While changing database selection...", e);
			ErrorHandler.getInstance().handleError(re);
		}
	}

	/**
	 * Lets the user confirm the connection data details if necessary, causes
	 * statement execution on the database server, updates the model with the
	 * results and handles exceptions.
	 */
	void executeStatement(boolean forFeedback) {
		updateCurrExerciseData(false);
		try {
			boolean doIt = true;
			if (connDataChanged) {
				String[] connData = application.getDBConnectionData();
				JLabel label1 = new JLabel("Server:");
				JTextField jsf = new JTextField(connData[0]);
				JLabel label2 = new JLabel("Database:");
				JTextField jdf = new JTextField(connData[1]);
				JLabel label3 = new JLabel("Login:");
				JTextField jlf = new JTextField(connData[2]);
				JLabel label4 = new JLabel("Password:");
				JPasswordField jpf = new JPasswordField(connData[3]);
				int result = JOptionPane.showConfirmDialog(view, new Object[] {
						label1, jsf, label2, jdf, label3, jlf, label4, jpf },
						"Connect to Server", JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION) {
					application.setDBConnectionData(jsf.getText(),
							jdf.getText(), jlf.getText(),
							new String(jpf.getPassword()));
				} else {
					doIt = false;
				}
			}
			if (doIt) {
				updateCurrExerciseData(false);
				if (forFeedback) {
					view.feedbackArea.setText("");
					Feedback feedback = application.provideFeedback();
					view.feedbackArea.setText(feedback.toString());
					view.updateEnableState();
					if (!feedback.getComments().isEmpty())
						connDataChanged = false;
				} else {
					JTextComponent comp = view.answerArea;
					if (view.neTabbedPane.getSelectedComponent() == view.solutionTab)
						comp = view.solutionArea;
					String statement = comp.getSelectedText();
					if (statement == null)
						statement = comp.getText();
					application.executeStatement(statement);
					model.fireModelEvent(EventType.RESULT_TABLE_CHANGED);
					connDataChanged = false;
				}
			}
		} catch (ClassNotFoundException ex) {
			RuntimeException re = new RuntimeException(
					"Unable to load JDBC driver ...", ex);
			ErrorHandler.getInstance().handleError(re);
			clearTables();
		} catch (SQLException ex) {
			RuntimeException re = new RuntimeException(
					"Communication with database failed.", ex);
			ErrorHandler.getInstance().handleError(re);
			clearTables();
		} catch (Exception ex) {
			RuntimeException re = new RuntimeException(
					"While executing an sql statement...", ex);
			ErrorHandler.getInstance().handleError(re);
			clearTables();
		}
	}

	public void lockSolutions() {
		String errorMessage = "";
		try {
			JPasswordField pwf = new JPasswordField();
			if (!model.getCurrExerciseSet().getPasswd().isEmpty()) {
				errorMessage = "Could not unlock solution information.";
				JOptionPane
						.showMessageDialog(
								view,
								new Object[] {
										"Password needed to unlock solution information:",
										pwf }, "Password",
								JOptionPane.QUESTION_MESSAGE);
				model.getCurrExerciseSet()
						.unlock(new String(pwf.getPassword()));
			} else {
				errorMessage = "Could not lock solution information.";
				JOptionPane
						.showMessageDialog(
								view,
								new Object[] {
										"Provide a password to lock solution information:",
										pwf }, "Password",
								JOptionPane.QUESTION_MESSAGE);
				if (pwf.getPassword().length > 0)
					model.getCurrExerciseSet().lock(
							new String(pwf.getPassword()));
			}
		} catch (EncryptionException e) {
			RuntimeException re = new RuntimeException(errorMessage, e);
			ErrorHandler.getInstance().handleError(re);
		}
		model.fireModelEvent(EventType.CURR_EXERCISE_CHANGED);
		view.updateEnableState();
	}

	public void clearTables() {
		application.clearTableData();
		model.fireModelEvent(EventType.RESULT_TABLE_CHANGED);
	}

	public void showAboutDialog() {
		JOptionPane.showMessageDialog(view, new Object[] { "SQL Trainer",
				"Version: 5.0", "Author: Ruediger Lunde" }, "About",
				JOptionPane.INFORMATION_MESSAGE);

	}

	// ///////////////////////////////////////////////////////////////
	// authoring...

	public void newExerciseSet() {
		if (updateCurrExerciseData(true)) {
			exerciseFileChooser = null;
			application.newExerciseSet();
			if (!editExerciseSet()) {
				application.closeExerciseSet();
				afterExerciseSetChange(-1);
			}
		}
	}

	// returns true if dialog was exited with OK.
	public boolean editExerciseSet() {
		boolean result = false;
		ExerciseSet exercises = application.getExerciseSet();
		String[] labels = new String[] { "Course", "Lecturer",
				"Exercise Set ID", "Database", "Introduction" };
		String[] values = new String[] { exercises.getCourse(),
				exercises.getLecturer(), exercises.getID(), exercises.getDB(),
				exercises.getIntro() };
		if (textEditor == null) {
			textEditor = new TextEditorDialog();
			textEditor.setSize(600, 400);
			view.registerComponent(textEditor);
		}
		if (textEditor.show("Edit Exercise Set Data", labels, values)) {
			application.modifyExerciseSetData(values[0], values[1], values[2],
					values[3], values[4]);
			result = true;
			afterExerciseSetChange(-1);
		}
		return result;
	}

	public void editExercise(boolean newExercise) {
		if (newExercise) {
			updateCurrExerciseData(false);
			application.insertExercise(new Exercise("", Type.SQL, "", ""));
		}
		Exercise exercise = application.getCurrExercise();
		String[] labels = new String[] { "ID", "Type", "Theme", "Question" };
		String[] values = new String[] { exercise.getID(),
				(exercise.getType() == Exercise.Type.SQL ? "sql" : ""),
				exercise.getTheme(), exercise.getQuestion() };
		if (textEditor == null) {
			textEditor = new TextEditorDialog();
			textEditor.setSize(600, 400);
			view.registerComponent(textEditor);
		}
		if (textEditor.show("Edit Exercise Data", labels, values)
				|| newExercise) {
			application.modifyCurrExerciseData(values[0], values[1], values[2],
					values[3]);
		}
		afterExerciseSetChange(application.getCurrExerciseIdx());
	}

	public void deleteExercise() {
		application.deleteCurrExercise();
		afterExerciseSetChange(application.getCurrExerciseIdx());
	}

	public void moveExercise(boolean up) {
		updateCurrExerciseData(false);
		application.moveCurrExercise(up);
		afterExerciseSetChange(application.getCurrExerciseIdx());
	}
}