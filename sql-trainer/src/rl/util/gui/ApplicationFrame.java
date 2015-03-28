/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import rl.util.exceptions.ErrorHandler;
import rl.util.exceptions.SimpleErrorHandler;

/**
 * Useful base class for building GUIs. The frame comes with a split pane
 * and a text area for displaying log messages, which is added to the bottom
 * of the split pane. The provided functionality includes look&feel changes,
 * font size adjustments, useful divider adjustments,
 * an error handler implementation, an output stream for
 * redirecting standard output, and a simple popup menu for clearing the log.
 * @author Ruediger Lunde
 *
 */
@SuppressWarnings("serial")
public class ApplicationFrame extends JFrame {
	protected JSplitPane centerPane;
	private JTextArea logArea;
	private List<Component> registeredComponents;
	private float fontScale;

	/** Creates the frame. */
	public ApplicationFrame() {
		registeredComponents = new ArrayList<Component>();
	    fontScale = 1f;
	    
		Container cp = getContentPane();
		centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		centerPane.setDividerSize(5);
        centerPane.setResizeWeight(1.0);
		cp.add(centerPane, BorderLayout.CENTER);
		logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroller = new JScrollPane(logArea);
        logScroller.setMinimumSize(new Dimension(0, 0));
        logScroller.setPreferredSize(new Dimension(0, 0));
        centerPane.add(logScroller, JSplitPane.BOTTOM);
       
        JPopupMenu popup = createPopup();
        registerComponent(popup);
        // Add listener to components that can bring up popup menus.
	    MouseListener listener = new PopupShower(popup);
	    logArea.addMouseListener(listener);
	}
	
	/**
	 * Changes the used look and feel at runtime.
	 */
	public void usePlatformLookAndFeel(boolean b) {
		try {
			if (b)
				UIManager.setLookAndFeel
				(UIManager.getSystemLookAndFeelClassName());
			else
				UIManager.setLookAndFeel
				(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			RuntimeException re = new RuntimeException
			("Could not change the look & feel.", e);
			ErrorHandler.getInstance().handleError(re);
		}
		SwingUtilities.updateComponentTreeUI(this);
		for (Component c : registeredComponents)
			SwingUtilities.updateComponentTreeUI(c);
		if (logArea.getText().isEmpty())
			centerPane.setDividerLocation(1.0);
	}
	
	/**
	 * Returns the currently used font scaling factor. The default
	 * is 1.
	 */
	public float getFontScale() {
		return fontScale;
	}
	
	/**
	 * Scales all fonts used within this frame and also within all
	 * registered components. Does unfortunately not work
	 * with all look and feels but at least with metal and windows L&F.
	 * @param scale Value 1 by default.
	 */
	public void setFontScale(float scale) {
		UIDefaults defaults = UIManager.getDefaults();
		Enumeration<?> keys = defaults.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = defaults.get(key);
			if (value != null && value instanceof Font) {
				UIManager.put(key, null);
				Font font = UIManager.getFont(key);
				if (font != null) {
					float size = font.getSize2D();
					UIManager.put(key, new FontUIResource(
							font.deriveFont(size* scale))
							);
				}
			}
		}
		fontScale = scale;
		SwingUtilities.updateComponentTreeUI(this);
		for (Component c : registeredComponents)
			SwingUtilities.updateComponentTreeUI(c);
	}
	
	/**
	 * Registers components like file choosers, which shall be updated
	 * on L&F and font scale changes and are not part of the frame
	 * component tree.
	 */
	public void registerComponent(Component c) {
		registeredComponents.add(c);
	}
	
	/**
	 * Returns an output stream which writes to the log area.
	 */
	public OutputStream getLogStream() {
		return new TextAreaOutputStream();
	}
	
	/**
	 * Returns an error handler, which uses the log area to display
	 * the messages.
	 */
	public ErrorHandler createErrorHandler() {
		return new MessageLogErrorHandler();
	}
	
	/** Factory method, responsible for creating a popup for the log area. */
	protected JPopupMenu createPopup() {
		JPopupMenu result = new JPopupMenu();
	    JMenuItem menuItem = new JMenuItem("Clear");
	    menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				clearLog();
			}
	    });
	    result.add(menuItem);
	    return result;
	}
	
	/**
	 * Prints a log message on the log area and adjusts the divider
	 * position if necessary.
	 */
	private void logMessage(String message) {
		int height = centerPane.getHeight();
		if (height == 0 || centerPane.getDividerLocation() * 1.2 > height) {
			centerPane.setDividerLocation(0.8);
		}
			
		int start = logArea.getDocument().getLength();
		logArea.append(message);
		int end = logArea.getDocument().getLength();
		logArea.setSelectionStart(start);
		logArea.setSelectionEnd(end);
	}
	
	/**
	 * Clears the text in the log area and sets the divider
	 * position to the bottom.
	 */
	public void clearLog() {
		try {
			Document doc = logArea.getDocument();
			doc.remove(0, doc.getLength());
			centerPane.setDividerLocation(1.0);	
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}
	
	/////////////////////////////////////////////////////////////////
	// inner classes
	
	private class MessageLogErrorHandler extends SimpleErrorHandler {
		protected void print(String text) {
			Document doc = logArea.getDocument();
			int len = doc.getLength();
			try {
				if (len > 0 && !doc.getText(len-1, 1).equals("\n"))
					logMessage("\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
			logMessage(text);
		}
		
	}
	
	/** Writes everything into the log area. */
	private class TextAreaOutputStream extends OutputStream { 
        @Override 
        public void write(int b) throws java.io.IOException { 
        	String s = new String(new char[]{(char)b}); 
        	logArea.append(s); 
        } 
	}
	
//	public static void main(String[] args) {
//		ApplicationFrame frame = new ApplicationFrame();
//		frame.setSize(600, 600);
//		frame.setVisible(true);
//	}
}
