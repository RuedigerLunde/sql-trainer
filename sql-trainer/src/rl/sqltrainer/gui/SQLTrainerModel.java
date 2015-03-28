/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.gui;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import rl.sqltrainer.application.SQLTrainer;
import rl.sqltrainer.domain.Exercise;
import rl.sqltrainer.domain.ExerciseSet;

/**
 * Adapter which hides application design details from the frame.
 * It provides access to the state of the <code>SQLTrainer</code> instance
 * and maintains some additional informations about the currently used databases. 
 * @author Ruediger Lunde
 */
public class SQLTrainerModel {
	SQLTrainer application;
	private DBTableModel resultTable;
	/** Classifies the events signaled to the view to inform about model changes. */
	enum EventType { DB_INFO_CHANGED, EXERCISE_SET_CHANGED, CURR_EXERCISE_CHANGED,
		RESULT_TABLE_CHANGED }
	
	/** Maintains all views. Typically, just one view is maintained here. */
	protected List<ModelListener> listeners = new ArrayList<ModelListener>();

	/** Creates a model for a given application. */
	public SQLTrainerModel(SQLTrainer app) {
		this.application = app;
		resultTable =  new DBTableModel(app.getResultTableData());
	}
	
	/** Adds a new view to the model. */
	public void addModelListener(ModelListener listener) {
		listeners.add(listener);
	}
	
	/** Signals to all registered listeners that the model has changed. */
	public void fireModelEvent(EventType type) {
		ModelEvent e = new ModelEvent(type);
		if (e.resultTableChanged())
			resultTable.fireTableStructureChanged();
		for (ModelListener listener : listeners)
			listener.modelChanged(e);
	}
	
	public boolean hasExercises() {
		return application.getExerciseSet() != null;
	}
	
	public ExerciseSet getCurrExerciseSet() {
		return application.getExerciseSet();
	}
	
	public Exercise getExercise() {
		return application.getCurrExercise();
	}
	
	public int getExerciseIdx() {
		return application.getCurrExerciseIdx();
	}
	
	public String getEditInfo() {
		ExerciseSet es = application.getExerciseSet();
		return "Editor: " + es.getEditor() + "\nLast Edit: " + es.getLastEdit();
	}
	
	public AbstractTableModel getResultTableModel() {
		return resultTable;
	}
	
	public List<String> getLogicalDBNames() {
		return application.getLogicalDBNames();
	}
	
	public Image getDBSchemaImage() {
		return application.getDBSchemaImage();
	}
	
	public String getDBDescription() {
		return application.getDBDescription();
	}
	
	///////////////////////////////////////////////////////////
	// inner classes
	
	/**
	 * Object indicating to interested listeners what has changed
	 * in the model.
	 */
	public static class ModelEvent {
		
		EventType type;
		private ModelEvent(EventType type) {
			this.type = type;
		}
		public boolean dbInfoChanged() {
			return type == EventType.DB_INFO_CHANGED;
		}
		public boolean exerciseSetChanged() {
			return type == EventType.EXERCISE_SET_CHANGED;
		}
		public boolean currExerciseChanged() {
			return type == EventType.CURR_EXERCISE_CHANGED;
		}
		public boolean resultTableChanged() {
			return type == EventType.RESULT_TABLE_CHANGED;
		}
	}
	
	/**
	 * Observer interface for views which need to be informed
	 * about model state changes and log messages to be printed out. 
	 */
	public static interface ModelListener {
		public void modelChanged(ModelEvent e);
	}
	
	/**
	 * Model, which provides <code>JTable</code> instances with the
	 * data they need.
	 */
	@SuppressWarnings("serial")
	static class DBTableModel extends AbstractTableModel {
	    /**
	     * List of data sets. The first row contains the column names.
	     * All values are represented as strings.
	     */
	    private List<List<String>> data;
	    
	    DBTableModel(List<List<String>> data) {
	    	this.data = data;
	    }
	    
	    /** Number of rows currently available. */
	    public int getRowCount() {
	        return Math.max(data.size()-1, 0);
	    }
	    /** Number of columns currently available. */
	    public int getColumnCount() {
	    	if (!data.isEmpty())
	    		return data.get(0).size();
	    	else
	    		return 0;
	    }
	    /** Returns the name of the <col>-th column. */
	    public String getColumnName(int col) {
	        return data.get(0).get(col);
	    }
	    /** Looks up the values for each table cell. */
	    public Object getValueAt(int row, int col) {
	        return data.get(row+1).get(col);
	    }
	}
}