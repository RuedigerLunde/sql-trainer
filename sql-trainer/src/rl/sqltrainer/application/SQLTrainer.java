/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.application;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import rl.sqltrainer.domain.Database;
import rl.sqltrainer.domain.Exercise;
import rl.sqltrainer.domain.ExerciseSet;
import rl.sqltrainer.domain.Feedback;
import rl.sqltrainer.domain.Exercise.Type;
import rl.sqltrainer.persistence.AsciiFileHandler;
import rl.sqltrainer.persistence.DBConnectionHandler;
import rl.sqltrainer.persistence.XMLFileHandler;
import rl.util.exceptions.EncryptionException;
import rl.util.exceptions.ErrorHandler;
import rl.util.exceptions.PersistenceException;
import rl.util.persistence.PropertyManager;

/**
 * Facade, hiding domain layer details from the GUI layer. The complete
 * application can (and should) be controlled by an instance of this class.
 * @author Ruediger Lunde
 */
public class SQLTrainer {
	private final File dbInfoPath;
	private final File dtdPath;
	
	private DBConnectionHandler dbAccessor;
	private XMLFileHandler xmlFileHandler;
	private FeedbackStrategy feedbackStrategy;
	
	private final List<Database> databases;
	private int currDatabaseIdx;
	private Image currDatabaseSchema;
	private String currDatabaseDescription;
	
	private ExerciseSet currExerciseSet;
	private int currExerciseIdx;
	private boolean hasUnsavedChanges;
	final private List<List<String>> resultTableData;
	
	/**
	 * Standard constructor. Default connection data and database information
	 * is provided by the property manager.
	 */
	public SQLTrainer() {
		dbInfoPath = PropertyManager.getAppDataDirectory("dbinfo");
		dtdPath = PropertyManager.getAppDataDirectory("xml");
		
		
		XMLFileHandler.setDtdPath(dtdPath);
		PropertyManager pm = PropertyManager.getInstance();
		dbAccessor = new DBConnectionHandler();
		if (pm.hasValue("db.jdbc.driver")) {
			dbAccessor.setJdbcDriverData(
					pm.getStringValue("db.jdbc.driver", "??"),
					pm.getStringValue("db.jdbc.url", "??"));
		}
		xmlFileHandler = new XMLFileHandler();
		feedbackStrategy = new SimpleFeedbackStrategy();
		databases = new ArrayList<Database>();
		List<String> logNames = pm.getListValue("db.names.logical", null);
		List<String> physNames = pm.getListValue("db.names.physical", null);
		if (logNames != null && physNames != null) {
			for (int i = 0; i < logNames.size(); i++) {
				String logName = logNames.get(i);
				File schema = new File(dbInfoPath, logName + ".png");
				File desc = new File(dbInfoPath, logName + ".html");
				databases.add(new Database(logName, physNames.get(i),
						(schema.exists() ? schema : null),
						(desc.exists() ? desc : null)));			
			}
		}
		currDatabaseIdx = -1;
		currExerciseIdx = -1;
		hasUnsavedChanges = false;
		dbAccessor.setConnectionData(
				pm.getStringValue("db.conn.server", ""),
				"",
				pm.getStringValue("db.conn.user", ""),
				pm.getStringValue("db.conn.passwd", ""));
		resultTableData  = new ArrayList<List<String>>();
	}
	
	/** Returns four strings: server name, database name, user name, and password. */
	public String[] getDBConnectionData() {
		return dbAccessor.getConnectionData();
	}
	
	/** Changes the connection data as specified. */
	public void setDBConnectionData(String server, String db, String user, String passwd) {
		PropertyManager pm = PropertyManager.getInstance();
		pm.setValue("db.conn.server", server);
		pm.setValue("db.conn.user", user);
		pm.setValue("db.conn.passwd", passwd);
		dbAccessor.setConnectionData(server, db, user, passwd);
	}
	
	/** Returns the logical names of all databases known by the SQLTrainer. */
	public List<String> getLogicalDBNames() {
		List<String> result = new ArrayList<String>();
		for (Database db : databases)
			result.add(db.getLogicalName());
		return result;
	}
	
	/**
	 * Sets the the specified database as current database, updates connection
	 * data if necessary and returns true if the new database differs from the
	 * previous selection.
	 */
	public boolean setDB(String logicalName) throws IOException {
		boolean result = false;
		int idx = getLogicalDBNames().indexOf(logicalName);
		if (idx < 0) {
			ErrorHandler.getInstance().handleWarning(new RuntimeException
			("Database " + logicalName + " not found."));
		} else if (idx != currDatabaseIdx) {
			Database currDB = databases.get(idx);
			String physDBName = currDB.getPhysicalName();
			currDatabaseIdx = idx;
			result = true;
			String[] connData = dbAccessor.getConnectionData();
			if (!connData[1].equals(physDBName))
				dbAccessor.setConnectionData
				(connData[0], physDBName, connData[2], connData[3]);
			if (currDB.getSchema() != null)
				currDatabaseSchema = ImageIO.read(currDB.getSchema());
			if (currDB.getDescription() != null)
				currDatabaseDescription = new AsciiFileHandler().readFile
				(currDB.getDescription());
		}
		return result;
	}
	
	public Image getDBSchemaImage() {
		return currDatabaseSchema;
	}
	
	public String getDBDescription() {
		return currDatabaseDescription;
	}
	
	public void loadExerciseSet(File file) throws PersistenceException {
		currExerciseSet = null;
		hasUnsavedChanges = false;
		currExerciseIdx = -1;
		currExerciseSet = xmlFileHandler.loadExerciseSet(file);
	}
	
	public void saveExerciseSet(File xmlFile, File htmlFile)
	throws PersistenceException {
		if (currExerciseSet != null) {
			xmlFileHandler.saveExerciseSet(xmlFile, currExerciseSet);
			hasUnsavedChanges = false;
			if (htmlFile != null)
				xmlFileHandler.transformExerciseSetToHTML
				(xmlFile, htmlFile, currExerciseSet.getPasswd().isEmpty());
		}
	}
	
	public void closeExerciseSet() {
		currExerciseSet = null;
		currExerciseIdx = -1;
		clearTableData();
	}
	
	public boolean hasUnsavedChanges() {
		return hasUnsavedChanges;
	}
	
	public ExerciseSet getExerciseSet() {
		return currExerciseSet;
	}
	
	public void setCurrExercise(int idx) {
		currExerciseIdx = idx;
	}
	
	public Exercise getCurrExercise() {
		if (currExerciseIdx != -1)
			return currExerciseSet.getExercise(currExerciseIdx);
		else
			return null;
	}
	
	public int getCurrExerciseIdx() {
		return currExerciseIdx;
	}
	
	/**
	 * Replaces the maintained answer and solution texts for the current
	 * exercise if the new values differ to the old ones.
	 * @param answer Possibly null.
	 * @param solution Possibly null.
	 */
	public void updateCurrExercise(String answer, String solution) {
		if (currExerciseIdx != -1) {
			if (answer != null && !getCurrExercise().getAnswer().equals(answer)) {
				currExerciseSet.setAnswer(currExerciseIdx, answer);
				hasUnsavedChanges = true;
			}
			if (solution != null && !getCurrExercise().getSolution().equals(solution)) {
				getCurrExercise().setSolution(solution);
				hasUnsavedChanges = true;
			}
		}
	}
	
	
	/////////////////////////////////////////////////////////////////
	// Special methods for doing SQL exercises.
	
	/**
	 * Executes the given SQL statement on the database server and updates
	 * the result table data.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public void executeStatement(String statement)
	throws SQLException, ClassNotFoundException {
		resultTableData.clear();
		List<List<String>> data = dbAccessor.executeStatement(statement);
		resultTableData.addAll(data);
	}
	
	/**
	 * Returns a list of lists of values. The first list contains the
	 * column names, all other lists contain rows with data set values.
	 * The list of lists can be empty!
	 */
	public List<List<String>> getResultTableData() {
		return resultTableData;
	}
	
	/** Removes all data from the result table. */
	public void clearTableData() {
		resultTableData.clear();
	}
	
	/**
	 * Creates a feedback for the current exercise or for the whole
	 * exercise set if <code>currExerciseIdx</code> is -1.
	 */
	public Feedback provideFeedback()
	throws SQLException, ClassNotFoundException, EncryptionException {
		if (currExerciseIdx != -1) {
			return provideFeedback(currExerciseIdx);
		} else {
			Feedback result = new Feedback();
			ArrayList<StringBuffer> exPoints = new ArrayList<StringBuffer>();
			int sumPoints = 0;
			for (int exIdx = 0; exIdx < currExerciseSet.size(); exIdx++) {
				Exercise ex = currExerciseSet.getExercise(exIdx);
				try {
					if (ex.getType() == Type.SQL && !ex.getAnswer().isEmpty()) {
						Feedback f = provideFeedback(exIdx);
						while (f.getPoints()+1 > exPoints.size())
							exPoints.add(new StringBuffer());
						StringBuffer sb = exPoints.get(f.getPoints());
						if (sb.length() > 0)
							sb.append(", ");
						sb.append(ex.getID());
						sumPoints += f.getPoints();
					}
				} catch (Exception e) {
					throw new RuntimeException
					("While generating feedback for exercise "
							+ ex.getID() + " ...", e);
				}
			}
			StringBuffer comments = new StringBuffer();
			for (int i = exPoints.size()-1; i > 0; i--) {
				if (exPoints.get(i).length() > 0)
					comments.append
					("Exercises with " + i + (i!=1 ? " points: " : " point: ")
							+ exPoints.get(i).toString() + "\n");
			}
			result.setComments(comments.toString());
			result.setPoints(sumPoints);
			return result;
		}
	}
	
	/**
	 * Executes the answer of the specified exercise (an SQL statement) on the
	 * database server and also the solution of the specified exercise
	 * and asks the used feedback strategy for a feedback.
	 */
	public Feedback provideFeedback(int exerciseIdx)
	throws SQLException, ClassNotFoundException, EncryptionException {
		String answer = currExerciseSet.getExercise(exerciseIdx).getAnswer();
		String solution = currExerciseSet.getDecryptedSolution(exerciseIdx);
		List<List<String>> aData = dbAccessor.executeStatement(answer);
		List<List<String>> sData = dbAccessor.executeStatement(solution);
		
		return feedbackStrategy.provideFeedback
		(currExerciseSet, exerciseIdx, aData, sData);
	}
	
	
	/////////////////////////////////////////////////////////////////
	// Authoring methods...
	
	public void newExerciseSet() {
		currExerciseSet = new ExerciseSet("", "", "", "");
		hasUnsavedChanges = false;
		currExerciseIdx = -1;
	}
	
	public void modifyExerciseSetData(String course, String lecturer,
			String id, String db, String intro) {
		currExerciseSet.setData(course, lecturer, id, db);
		currExerciseSet.setIntro(intro);
		hasUnsavedChanges = true;
	}
	
	public void modifyCurrExerciseData(String id, String type, String theme,
			String question) {
		if (currExerciseIdx != -1) {
			getCurrExercise().setData(id,
					(type.equals("sql") ? Type.SQL : Type.OTHER),
					theme, question);
			hasUnsavedChanges = true;
		}
	}
	
	public void insertExercise(Exercise ex) {
		List<Exercise> exercises = currExerciseSet.getExercises();
		exercises.add(++currExerciseIdx, ex);
		hasUnsavedChanges = true;
	}
	
	public Exercise deleteCurrExercise() {
		Exercise result = getCurrExercise();
		currExerciseSet.getExercises().remove(currExerciseIdx);
		if (currExerciseIdx == currExerciseSet.size())
			currExerciseIdx--;
		hasUnsavedChanges = true;
		return result;
	}
	
	public void moveCurrExercise(boolean up) {
		int insertPos = (up ? currExerciseIdx-2 : currExerciseIdx);
		Exercise ex = deleteCurrExercise();
		currExerciseIdx = insertPos;
		insertExercise(ex);
	}
}
