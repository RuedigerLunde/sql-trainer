/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import rl.util.exceptions.ErrorHandler;

/**
 * Provides indentation functionality for Swing text components.
 * @author Ruediger Lunde
 */
public class IndentationUtil {
	
	/** Adds useful indentation functionality to the specified text component. */
	public static void addIndentActions(JTextComponent textComponent) {
	    AbstractAction action = new TabAction(textComponent, false);
	    textComponent.getActionMap().put("Tab", action);
	 	// Bind the tab action to <tab>
	    textComponent.getInputMap().put(KeyStroke.getKeyStroke
	    		(KeyEvent.VK_TAB, 0), "Tab");
	    
	    action = new TabAction(textComponent, true);
	    textComponent.getActionMap().put("ShiftTab", action);
	 	// Bind the shift-tab action to <shift tab>
	    textComponent.getInputMap().put(KeyStroke.getKeyStroke
	    		(KeyEvent.VK_TAB, ActionEvent.SHIFT_MASK), "ShiftTab");
	    
    	action = new NewLineAction(textComponent);
    	textComponent.getActionMap().put("NewLine", action);
	 	// Bind the new-line action to <enter>
    	textComponent.getInputMap().put(KeyStroke.getKeyStroke
	    		(KeyEvent.VK_ENTER, 0), "NewLine");
	}
	
	
	/////////////////////////////////////////////////////////////////
	// inner classes

	/**
	 * Inserts and removes groups of four spaces depending on text selection
	 * state and the value of attribute <code>shift</code>.
	 * @author R. Lunde
	 */
	@SuppressWarnings("serial")
	private static class TabAction extends AbstractAction {
		JTextComponent textComponent;
		boolean shift;
		
		public TabAction(JTextComponent textComponent, boolean shift) {
			this.textComponent = textComponent;
			this.shift = shift;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				Document doc = textComponent.getDocument();
				int start = textComponent.getSelectionStart();
				int end = textComponent.getSelectionEnd();
				if (end > start) {
					int pos = textComponent.getText().lastIndexOf('\n', start);
					do {
						if (!shift) {
							doc.insertString(pos+1, "    ", null);
						} else if (pos+4 < end
								&& textComponent.getText(pos+1, 4).equals("    ")) {
							doc.remove(pos+1, 4);
						}
						pos = textComponent.getText().indexOf('\n', pos+1);
						end = textComponent.getSelectionEnd();
					} while (pos != -1 && pos < end);
				} else if (!shift) {
					doc.insertString(textComponent.getCaretPosition(), "    ", null);
				} else {
					int pos = textComponent.getCaretPosition();
					if (pos >= 4 && textComponent.getText(pos - 4, 4).equals("    "))
						doc.remove(pos-4, 4);	
				}
			} catch (BadLocationException e) {
				RuntimeException re = new RuntimeException
				("While executing tab action...", e);
				ErrorHandler.getInstance().handleError(re);
			}
		}
	}
	
	/**
	 * Inserts a new line character at the current caret position
	 * and additionally the same number of spaces as in the line before.
	 * @author R. Lunde
	 */
	@SuppressWarnings("serial")
	private static class NewLineAction extends AbstractAction {
		JTextComponent textComponent;
		
		public NewLineAction(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			try {
				String txt = textComponent.getText();
				int iPos = textComponent.getCaretPosition();
				StringBuffer iText = new StringBuffer("\n");
				int pos = txt.lastIndexOf('\n', iPos-1) + 1;
				while (pos < txt.length() && txt.charAt(pos) == ' ') {
					iText.append(' ');
					++pos;
				}
				textComponent.getDocument().insertString
				(iPos, iText.toString(), null);
			} catch (BadLocationException e) {
				RuntimeException re = new RuntimeException
				("While executing new line action...", e);
				ErrorHandler.getInstance().handleError(re);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
