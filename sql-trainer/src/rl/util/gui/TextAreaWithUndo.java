/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Extends the swing text area with local undo functionality. Note, that the undo
 * manager is reset whenever the method {@link #setText(String)} is called.
 * @author Ruediger Lunde
 */
@SuppressWarnings("serial")
public class TextAreaWithUndo extends JTextArea {

	protected UndoManager undoManager;
	protected UndoableEditListener editListener;
	protected AbstractAction undoAction;
	protected AbstractAction redoAction;
	protected UndoPopupMenu undoPopup;
	
	/** Standard constructor, creates a text area with all features. */
	public TextAreaWithUndo() {
		this(true);
	}
	
	/**
	 * Creates a text area with undo functionality.
	 * @param enablePopup Controls, whether a popup menu is created.
	 */
	public TextAreaWithUndo(boolean enablePopup) {
    	undoManager = new UndoManager();
		editListener = new UndoableEditListener() {
	        public void undoableEditHappened(UndoableEditEvent evt) {
	            undoManager.addEdit(evt.getEdit());
	        }
	    };
		// Listen for undo and redo events
	    getDocument().addUndoableEditListener(editListener);
	    // Create an undo action and add it to the text component
	    undoAction = new AbstractAction("Undo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoManager.canUndo())
                        undoManager.undo();
                } catch (CannotUndoException e) {}}};
	    getActionMap().put("Undo", undoAction);
	    // Bind the undo action to ctl-Z
	    getInputMap().put(KeyStroke.getKeyStroke
	    		(KeyEvent.VK_Z, ActionEvent.CTRL_MASK), "Undo");
	    // Create an undo action and add it to the text component
	    redoAction = new AbstractAction("Redo") {
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undoManager.canRedo())
                        undoManager.redo();
                } catch (CannotRedoException e) {}}};
        getActionMap().put("Redo", redoAction);
	 	// Bind the redo action to ctl-Y
	    getInputMap().put(KeyStroke.getKeyStroke
	    		(KeyEvent.VK_Y, ActionEvent.CTRL_MASK), "Redo");
	    if (enablePopup)
			addMouseListener(new PopupShower(getUndoPopup()));	
	}

	/** Returns an undo action for the current document. */
	public AbstractAction getUndoAction() {
		return undoAction;
	}
	
	/** Returns a redo action for the current document. */
	public AbstractAction getRedoAction() {
		return redoAction;
	}
	
	/**
	 * Returns an undo/redo popup menu. The implementation uses
	 * lazy instantiation.
	 */
	public JPopupMenu getUndoPopup() {
		if (undoPopup == null)
			undoPopup = new UndoPopupMenu();
		return undoPopup;
	}
	
	/** Also removes the undoable edit listener from the old document if any. */
	@Override
	public void setDocument(Document doc) {
		if (getDocument() != null)
			getDocument().removeUndoableEditListener(editListener);
		super.setDocument(doc);
		doc.addUndoableEditListener(editListener);
	}

	/** Also resets the undo manager. */
	@Override
	public void setText(String text) {
		super.setText(text);
		undoManager.discardAllEdits();
	}
	
	
	/////////////////////////////////////////////////////////////////
	// inner classes
	
	/** Simple popup menu providing undo and redo functionality. */
	protected class UndoPopupMenu extends JPopupMenu {
		protected JMenuItem undoMenuItem;
		protected JMenuItem redoMenuItem;
		
		/** Standard constructor. */
		public UndoPopupMenu() {
			undoMenuItem = new JMenuItem(getUndoAction());
			// help the user to remember ctrl z
			undoMenuItem.setAccelerator(KeyStroke.getKeyStroke
		    		(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
			add(undoMenuItem);
			redoMenuItem = new JMenuItem(getRedoAction());
			// help the user to remember ctrl y
			redoMenuItem.setAccelerator(KeyStroke.getKeyStroke
		    		(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		    add(redoMenuItem);
		}
		/** Updates enable state before showing the popup. */
		public void setVisible(boolean b) {
			if (b) {
				undoMenuItem.setEnabled(undoManager.canUndo());
				redoMenuItem.setEnabled(undoManager.canRedo());
			} else {
				undoMenuItem.setEnabled(true);
				redoMenuItem.setEnabled(true);
			}
			super.setVisible(b);
		}
	}
}
