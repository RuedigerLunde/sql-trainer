/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import rl.sqltrainer.domain.Exercise;
import rl.sqltrainer.domain.ExerciseSet;
import rl.util.exceptions.ErrorHandler;
import rl.util.exceptions.PersistenceException;
import rl.util.gui.ApplicationFrame;
import rl.util.gui.IndentationUtil;
import rl.util.gui.TextAreaWithUndo;
import rl.util.persistence.PropertyManager;

/**
 * Frame to control the tool. It is responsible for all presentation aspects.
 * 
 * @author Ruediger Lunde
 */
@SuppressWarnings("serial")
class SQLTrainerFrame extends ApplicationFrame implements
		SQLTrainerModel.ModelListener {
	private SQLTrainerModel model;
	private SQLTrainerController controller;
	private ActionListener actionListener;

	private JMenuItem newMenuItem;
	// private JMenuItem loadMenuItem;
	private JMenuItem saveMenuItem;
	private JMenu exerciseMenu;
	private JMenuItem prevExerciseMenuItem;
	private JMenuItem nextExerciseMenuItem;
	private JMenuItem executeMenuItem;
	private JMenuItem feedbackMenuItem;
	private JCheckBoxMenuItem lockSolutionsMenuItem;

	private JMenu authoringMenu;
	private JMenuItem editSetDataMenuItem;
	private JMenuItem modifyExMenuItem;
	private JMenuItem insertExMenuItem;
	private JMenuItem deleteExMenuItem;
	private JMenuItem moveUpExMenuItem;
	private JMenuItem moveDownExMenuItem;

	private JCheckBoxMenuItem enableAuthoringMenuItem;
	private JCheckBoxMenuItem lookAndFeelMenuItem;
	// private JMenuItem incFontMenuItem;
	// private JMenuItem decFontMenuItem;

	JComboBox<String> exerciseCombo; // visible to controller
	private JButton nextExerciseButton;
	private JButton prevExerciseButton;
	JComboBox<String> dbCombo; // visible to controller
	private JButton executeButton;
	private JButton feedbackButton;

	JTabbedPane mainTabbedPane;
	private JSplitPane northPane;
	private JTabbedPane nwTabbedPane;
	private JScrollPane questionTab;
	JTabbedPane neTabbedPane;
	private JScrollPane answerTab;
	JScrollPane feedbackTab; // visible to controller
	JScrollPane solutionTab;
	JEditorPane questionArea;
	TextAreaWithUndo answerArea;
	JTextArea feedbackArea;
	TextAreaWithUndo solutionArea;
	JScrollPane schemaTab;
	JScrollPane resultTableTab; // visible to controller
	private JTable resultTable;
	private JLabel schemaArea;
	private JEditorPane dbDescriptionArea;
	JScrollPane htmlViewerTab;
	JEditorPane htmlViewerArea;

	/**
	 * Standard constructor.
	 */
	SQLTrainerFrame() {
		actionListener = new ActionListener() { // delegates calls to controller
			@Override
			public void actionPerformed(ActionEvent e) {
				if (controller != null)
					controller.actionPerformed(e);
			}
		};
		init();
		restoreSession();
	}

	/**
	 * Restores view settings according to the settings of the last session.
	 */
	private void restoreSession() {
		PropertyManager pm = PropertyManager.getInstance();
		lookAndFeelMenuItem.setSelected(pm.getBooleanValue(
				"gui.look&feel.usePlatform", false));
		usePlatformLookAndFeel(lookAndFeelMenuItem.isSelected());
		setFontScale((float) pm.getDoubleValue("gui.fontscale", 1.0));
		setSize(pm.getIntValue("gui.window.width", 700),
				pm.getIntValue("gui.window.height", 700));
		// static properties - not saved
		answerArea.setText(pm.getStringValue("gui.statement", ""));
	}

	/** Saves session settings before exiting from the application. */
	public void storeSessionAndExit() {
		if (controller == null || controller.updateCurrExerciseData(true)) {
			setVisible(false);
			PropertyManager pm = PropertyManager.getInstance();
			pm.setValue("gui.look&feel.usePlatform", UIManager.getLookAndFeel()
					.getClass().getName() == UIManager
					.getSystemLookAndFeelClassName());
			pm.setValue("gui.fontscale", getFontScale());
			pm.setValue("gui.window.width", getSize().width);
			pm.setValue("gui.window.height", getSize().height);
			if (controller.exerciseFileChooser != null) {
				File file = controller.exerciseFileChooser.getSelectedFile();
				if (file != null && file.getParent() != null)
					pm.setValue("directory.exercises", file.getParent());
			}
			try {
				pm.saveSessionProperties();
			} catch (PersistenceException e) {
				ErrorHandler.getInstance().handleError(e);
			}
			System.exit(0);
		}
	}

	public void setModel(SQLTrainerModel model) {
		this.model = model;
		resultTable.setModel(model.getResultTableModel());
		dbCombo.removeAllItems();
		List<String> dbs = model.getLogicalDBNames();
		for (String dbName : dbs)
			dbCombo.addItem(dbName);
	}

	public void setController(SQLTrainerController controller) {
		this.controller = controller;
	}

	/**
	 * Brings model, view and controller into a consistent state. Call this
	 * method, after plugging the three components together.
	 */
	public void resetSelectionState() {
		if (exerciseCombo.getItemCount() > 0
				&& exerciseCombo.getSelectedIndex() != 0)
			exerciseCombo.setSelectedIndex(0);
		if (dbCombo.getItemCount() == 0)
			controller.changeDB("??"); // no database name available...
		else if (dbCombo.getSelectedIndex() != 0)
			dbCombo.setSelectedIndex(0);
		else
			controller.changeDB((String) dbCombo.getSelectedItem());
		updateEnableState();
	}

	/**
	 * Calls super class implementation and adjusts the result table row height.
	 */
	public void setFontScale(float scale) {
		super.setFontScale(scale);
		resultTable.setRowHeight((int) (16 * scale)); // hack!
	}

	/**
	 * Initializes the frame. All layout details are included here.
	 */
	private void init() {
		setTitle("SQL Trainer");
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new MyWindowListener());
		Container cp = getContentPane();

		JMenuItem item;
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		newMenuItem = createMenuItem("New", Commands.NEW_EXERCISE_SET_CMD,
				fileMenu);
		item = createMenuItem("Load Exercises", Commands.LOAD_EXERCISE_SET_CMD,
				fileMenu);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
				ActionEvent.CTRL_MASK));
		saveMenuItem = createMenuItem("Save Exercises", Commands.SAVE_EXERCISE_SET_CMD,
				fileMenu);
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));
		createMenuItem("Exit", Commands.EXIT_CMD, fileMenu);

		exerciseMenu = new JMenu("Exercise");
		menuBar.add(exerciseMenu);
		prevExerciseMenuItem = createMenuItem("Previous Exercise",
				Commands.PREV_EXERCISE_CMD, exerciseMenu);
		prevExerciseMenuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		nextExerciseMenuItem = createMenuItem("Next Exercise",
				Commands.NEXT_EXERCISE_CMD, exerciseMenu);
		nextExerciseMenuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		executeMenuItem = createMenuItem("Execute Statement",
				Commands.EXECUTE_CMD, exerciseMenu);
		executeMenuItem.setAccelerator(KeyStroke
				.getKeyStroke(KeyEvent.VK_F5, 0));
		feedbackMenuItem = createMenuItem("Feedback for Statement",
				Commands.FEEDBACK_CMD, exerciseMenu);
		feedbackMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
				ActionEvent.CTRL_MASK));
		exerciseMenu.addSeparator();
		lockSolutionsMenuItem = new JCheckBoxMenuItem("Lock Solutions");
		lockSolutionsMenuItem.addActionListener(actionListener);
		lockSolutionsMenuItem.setModel(new DefaultButtonModel() {
			public boolean isSelected() {
				return model != null && model.hasExercises()
						&& !model.getCurrExerciseSet().getPasswd().isEmpty();
			}
		});
		lockSolutionsMenuItem.setActionCommand(Commands.LOCK_SOLUTIONS_CMD);
		exerciseMenu.add(lockSolutionsMenuItem);

		authoringMenu = new JMenu("Authoring");
		menuBar.add(authoringMenu);
		editSetDataMenuItem = createMenuItem("Exercise Set Data",
				Commands.EDIT_EXERCISE_SET_CMD, authoringMenu);
		editSetDataMenuItem.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		JMenu editExMenu = new JMenu("Exercise");
		authoringMenu.add(editExMenu);
		modifyExMenuItem = createMenuItem("Modify",
				Commands.MODIFY_EXERCISE_CMD, editExMenu);
		modifyExMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
				ActionEvent.CTRL_MASK));
		insertExMenuItem = createMenuItem("Insert",
				Commands.INSERT_EXERCISE_CMD, editExMenu);
		insertExMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
				ActionEvent.CTRL_MASK));
		deleteExMenuItem = createMenuItem("Delete",
				Commands.DELETE_EXERCISE_CMD, editExMenu);
		moveUpExMenuItem = createMenuItem("Move Up",
				Commands.MOVE_UP_EXERCISE_CMD, authoringMenu);
		moveDownExMenuItem = createMenuItem("Move Down",
				Commands.MOVE_DOWN_EXERCISE_CMD, authoringMenu);

		JMenu viewMenu = new JMenu("View");
		menuBar.add(viewMenu);
		enableAuthoringMenuItem = new JCheckBoxMenuItem("Enable Authoring");
		enableAuthoringMenuItem.setActionCommand(Commands.ENABLE_AUTHORING_CMD);
		enableAuthoringMenuItem.addActionListener(actionListener);
		viewMenu.add(enableAuthoringMenuItem);
		lookAndFeelMenuItem = new JCheckBoxMenuItem("Use Platform L&F");
		lookAndFeelMenuItem.setActionCommand(Commands.USE_PLATFORM_LAF_CMD);
		lookAndFeelMenuItem.addActionListener(actionListener);
		viewMenu.add(lookAndFeelMenuItem);
		JMenu fontMenu = new JMenu("Font Size");
		viewMenu.add(fontMenu);
		item = createMenuItem("Increase", Commands.INCREASE_FONT_CMD, fontMenu);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
				ActionEvent.CTRL_MASK));
		item = createMenuItem("Decrease", Commands.DECREASE_FONT_CMD, fontMenu);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
				ActionEvent.CTRL_MASK));

		JMenu helpMenu = new JMenu("?");
		menuBar.add(helpMenu);
		createMenuItem("About", Commands.SHOW_ABOUT_CMD, helpMenu);

		JToolBar toolbar = new JToolBar();
		cp.add(toolbar, BorderLayout.NORTH);
		toolbar.setFloatable(false);
		exerciseCombo = new JComboBox<String>(new String[] {});
		exerciseCombo.setToolTipText("Select Exercise");
		exerciseCombo.setFocusable(false);
		exerciseCombo.setVisible(false);
		exerciseCombo.setActionCommand(Commands.SELECT_EXERCISE_CMD);
		exerciseCombo.addActionListener(actionListener);
		toolbar.add(exerciseCombo);
		prevExerciseButton = createButton("<", "Previous Exercise",
				Commands.PREV_EXERCISE_CMD);
		toolbar.add(prevExerciseButton);
		nextExerciseButton = createButton(">", "Next Exercise",
				Commands.NEXT_EXERCISE_CMD);
		toolbar.add(nextExerciseButton);

		toolbar.addSeparator();
		dbCombo = new JComboBox<String>();
		dbCombo.setToolTipText("Select Database");
		dbCombo.setActionCommand(Commands.SELECT_DB_CMD);
		dbCombo.addActionListener(actionListener);
		dbCombo.setFocusable(false);
		toolbar.add(dbCombo);
		toolbar.add(Box.createHorizontalGlue());
		executeButton = createButton("Execute", "Execute Statement",
				Commands.EXECUTE_CMD);
		toolbar.add(executeButton);
		feedbackButton = createButton("Feedback", "Feedback for Statement",
				Commands.FEEDBACK_CMD);
		toolbar.add(feedbackButton);

		// the center pane is a split pane with nested split panes
		// centerPane =
		// [top: mainPane =
		// [top: northPane = [left:nwTabbedPane, right:neTabbedPane],
		// bottom: mainTabbedPane]
		// bottom: logArea]
		JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPane.setDividerSize(5);
		mainPane.setDividerLocation(150);
		centerPane.add(mainPane, JSplitPane.TOP);
		northPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		northPane.setDividerSize(0);
		northPane.setDividerLocation(0);
		mainPane.add(northPane, JSplitPane.TOP);
		nwTabbedPane = new JTabbedPane();
		northPane.add(nwTabbedPane, JSplitPane.LEFT);
		neTabbedPane = new JTabbedPane();
		northPane.add(neTabbedPane, JSplitPane.RIGHT);
		mainTabbedPane = new JTabbedPane();
		mainPane.add(mainTabbedPane, JSplitPane.BOTTOM);

		questionArea = new JEditorPane();
		questionArea.setContentType("text/html; charset=EUC-JP");
		questionArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
				Boolean.TRUE);
		questionArea.setEditable(false);
		questionTab = new JScrollPane(questionArea);
		nwTabbedPane.addTab("Question", questionTab);
		nwTabbedPane.setMinimumSize(new Dimension(0, 0));

		answerArea = new TextAreaWithUndo();
		registerComponent(answerArea.getUndoPopup());
		IndentationUtil.addIndentActions(answerArea);
		// answerArea.setLineWrap(true);
		answerTab = new JScrollPane(answerArea);
		neTabbedPane.addTab("Answer", answerTab);
		feedbackArea = new JTextArea();
		feedbackArea.setLineWrap(true);
		feedbackArea.setEditable(false);
		feedbackTab = new JScrollPane(feedbackArea);
		neTabbedPane.addTab("Feedback", feedbackTab);
		solutionArea = new TextAreaWithUndo();
		registerComponent(solutionArea.getUndoPopup());
		IndentationUtil.addIndentActions(solutionArea);
		// solutionArea.setLineWrap(true);
		solutionArea.setEditable(false);
		solutionTab = new JScrollPane(solutionArea);
		neTabbedPane.addTab("Solution", solutionTab);

		resultTable = new JTable();
		resultTableTab = new JScrollPane(resultTable);
		mainTabbedPane.addTab("Results", resultTableTab);

		schemaArea = new JLabel();
		JPanel schemaPanel = new JPanel(new BorderLayout());
		schemaPanel.setBackground(java.awt.Color.white);
		schemaPanel.add(schemaArea, BorderLayout.CENTER);
		schemaTab = new JScrollPane(schemaPanel);
		javax.swing.JScrollBar vbar = schemaTab.getVerticalScrollBar();
		vbar.setUnitIncrement(30);
		mainTabbedPane.addTab("DB Schema", schemaTab);

		dbDescriptionArea = new JEditorPane();
		dbDescriptionArea.setContentType("text/html; charset=EUC-JP");
		dbDescriptionArea.putClientProperty(
				JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		dbDescriptionArea.setEditable(false);
		JScrollPane dbDescriptionTab = new JScrollPane(dbDescriptionArea);
		vbar = dbDescriptionTab.getVerticalScrollBar();
		vbar.setUnitIncrement(30);
		mainTabbedPane.addTab("DB Description", dbDescriptionTab);

		htmlViewerArea = new JEditorPane();
		htmlViewerArea.setContentType("text/html; charset=EUC-JP");
		htmlViewerArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
				Boolean.TRUE);
		htmlViewerArea.setEditable(false);
		htmlViewerTab = new JScrollPane(htmlViewerArea);
		vbar = htmlViewerTab.getVerticalScrollBar();
		vbar.setUnitIncrement(30);
		mainTabbedPane.addTab("HTML Viewer", htmlViewerTab);

		getRootPane().setDefaultButton(executeButton);
	}

	/** Little helper method to reduce code size. */
	private JMenuItem createMenuItem(String title, String actionCmd, JMenu menu) {
		JMenuItem result = new JMenuItem(title);
		result.setActionCommand(actionCmd);
		result.addActionListener(actionListener);
		menu.add(result);
		return result;
	}

	/** Little helper method to reduce code size. */
	private JButton createButton(String text, String tooltip, String actionCmd) {
		JButton result = new JButton(text);
		result.setActionCommand(actionCmd);
		result.addActionListener(actionListener);
		result.setFocusable(false);
		return result;
	}

	/**
	 * Sets the enable state of buttons and menus, modifies tab titles and
	 * adjusts divider locations according to the current state of the
	 * application.
	 */
	public void updateEnableState() {
		if (model.hasExercises()) {
			if (northPane.getDividerSize() == 0) {
				northPane.setDividerSize(5);
				northPane.setDividerLocation(northPane.getWidth() / 2);
				northPane.setResizeWeight(0.5);
			}
			if (model.getExercise() != null) {
				setTabTitle(questionTab, "Question");
				setTabTitle(answerTab, "Answer");
				answerArea.setEditable(true);
			} else {
				setTabTitle(questionTab, "Introduction");
				setTabTitle(answerTab, "Editing Info");
				answerArea.setEditable(false);
			}
			prevExerciseMenuItem
					.setEnabled(exerciseCombo.getSelectedIndex() > 0);
			prevExerciseButton.setEnabled(exerciseCombo.getSelectedIndex() > 0);
			nextExerciseMenuItem
					.setEnabled(exerciseCombo.getSelectedIndex() < exerciseCombo
							.getItemCount() - 1);
			nextExerciseButton
					.setEnabled(exerciseCombo.getSelectedIndex() < exerciseCombo
							.getItemCount() - 1);
		} else {
			northPane.setDividerSize(0);
			northPane.setDividerLocation(0);
			northPane.setResizeWeight(0);
			setTabTitle(answerTab, "SQL Statement");
			answerArea.setEditable(true);
		}
		saveMenuItem.setEnabled(model.hasExercises());
		exerciseMenu.setEnabled(model.hasExercises());
		exerciseCombo.setVisible(model.hasExercises());
		prevExerciseButton.setVisible(model.hasExercises());
		nextExerciseButton.setVisible(model.hasExercises());
		newMenuItem.setEnabled(enableAuthoringMenuItem.isSelected());
		authoringMenu.setEnabled(enableAuthoringMenuItem.isSelected()
				&& model.hasExercises() && !lockSolutionsMenuItem.isSelected());
		modifyExMenuItem.setEnabled(model.getExercise() != null);
		insertExMenuItem.setEnabled(model.hasExercises());
		deleteExMenuItem.setEnabled(model.getExercise() != null);
		moveUpExMenuItem.setEnabled(exerciseCombo.getSelectedIndex() > 1);
		moveDownExMenuItem.setEnabled(model.getExercise() != null
				&& exerciseCombo.getSelectedIndex() < exerciseCombo
						.getItemCount() - 1);
		solutionArea.setEditable(authoringMenu.isEnabled());
		neTabbedPane.setEnabledAt(1, !feedbackArea.getText().isEmpty()); // hack!
		neTabbedPane.setEnabledAt(2,
				authoringMenu.isEnabled() && model.getExercise() != null
						|| !lockSolutionsMenuItem.isSelected()
						&& !solutionArea.getText().isEmpty()); // hack!
		if (!neTabbedPane.isEnabledAt(neTabbedPane.getSelectedIndex()))
			neTabbedPane.setSelectedIndex(0);
		Exercise currEx = model.getExercise();
		boolean sqlEx = currEx != null && currEx.getType() == Exercise.Type.SQL;
		boolean intro = model.hasExercises() && currEx == null;
		executeMenuItem.setEnabled(sqlEx || !model.hasExercises());
		executeButton.setEnabled(sqlEx || !model.hasExercises());
		feedbackMenuItem.setEnabled(intro || sqlEx
				&& !currEx.getSolution().isEmpty());
		feedbackButton.setEnabled(intro || sqlEx
				&& !currEx.getSolution().isEmpty());
	}

	/**
	 * Helper method.
	 */
	private void setTabTitle(Component tab, String title) {
		JTabbedPane[] panes = new JTabbedPane[] { mainTabbedPane, nwTabbedPane,
				neTabbedPane };
		for (JTabbedPane tp : panes) {
			int idx = tp.indexOfComponent(tab);
			if (idx != -1) {
				tp.setTitleAt(idx, title);
				return;
			}
		}
	}

	/** Reacts on model changes. */
	public void modelChanged(SQLTrainerModel.ModelEvent e) {
		if (e.dbInfoChanged()) {
			if (model.getDBSchemaImage() != null) {
				Icon icon = new ImageIcon(model.getDBSchemaImage());
				schemaArea.setIcon(icon);
				mainTabbedPane.setSelectedComponent(schemaTab);
			} else {
				schemaArea.setIcon(null);
			}
			if (model.getDBDescription() != null) {
				dbDescriptionArea.setText(model.getDBDescription());
				dbDescriptionArea.setSelectionStart(0);
				dbDescriptionArea.setSelectionEnd(1);
			} else {
				dbDescriptionArea.setText("");
			}
		} else if (e.exerciseSetChanged()) {
			exerciseCombo.removeActionListener(actionListener);
			exerciseCombo.removeAllItems();
			if (model.hasExercises()) {
				for (String item : model.getCurrExerciseSet()
						.getExerciseLabels())
					exerciseCombo.addItem(item);
				String db = model.getCurrExerciseSet().getDB();
				if (!db.isEmpty()) {
					dbCombo.setSelectedItem(db);
					if (!dbCombo.getSelectedItem().equals(db)) {
						RuntimeException re = new RuntimeException("Database "
								+ db + " not found.");
						ErrorHandler.getInstance().handleWarning(re);
					}
				}
			}
			exerciseCombo.addActionListener(actionListener);
		} else if (e.currExerciseChanged()) {
			updateCurrExerciseViews();
		}
		updateEnableState();
	}

	/** Displays the data contained in the current exercise. */
	public void updateCurrExerciseViews() {
		String questionTxt = "";
		String answerTxt = "";
		String solutionTxt = "";
		if (model.hasExercises()) {
			ExerciseSet exSet = model.getCurrExerciseSet();
			Exercise ex = model.getExercise();
			if (ex == null) {
				questionTxt = exSet.getIntro();
				answerTxt = model.getEditInfo();
			} else {
				questionTxt = ex.getQuestion();
				answerTxt = ex.getAnswer();
				solutionTxt = ex.getSolution();
			}
		}
		questionArea.setText(questionTxt);
		questionArea.setSelectionStart(0);
		questionArea.setSelectionEnd(1);
		answerArea.setText(answerTxt);
		feedbackArea.setText("");
		solutionArea.setText(solutionTxt);
		if (exerciseCombo.getItemCount() > 0
				&& exerciseCombo.getSelectedIndex() != model.getExerciseIdx() + 1) {
			exerciseCombo.removeActionListener(actionListener);
			exerciseCombo.setSelectedIndex(model.getExerciseIdx() + 1);
			exerciseCombo.addActionListener(actionListener);
		}
	}

	// ///////////////////////////////////////////////////////////////
	// inner classes

	/** Reacts on window close button events. */
	private class MyWindowListener implements WindowListener {
		@Override
		public void windowActivated(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowClosed(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowClosing(WindowEvent arg0) {
			storeSessionAndExit();
		}

		@Override
		public void windowDeactivated(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowDeiconified(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowIconified(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void windowOpened(WindowEvent arg0) {
			// TODO Auto-generated method stub
		}
	}

	// public static void main(String[] args) {
	// SQLTrainerFrame frame = new SQLTrainerFrame();
	// frame.setSize(600, 600);
	// frame.setVisible(true);
	// try {
	// UIManager.LookAndFeelInfo[] infos =
	// UIManager. getInstalledLookAndFeels();
	// for (int i = 0; i < infos.length; i++)
	// System.out.println(infos[i].getName() + ": " + infos[i].getClassName());
	// } catch (Exception e){}
	// }
}