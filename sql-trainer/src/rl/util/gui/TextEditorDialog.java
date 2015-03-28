/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 * Provides a simple dialog for editing string values in text fields
 * (for one-line properties) and scrolled text areas (for texts which do
 * not fit in on line).
 * @author Ruediger Lunde
 */
@SuppressWarnings("serial")
public class TextEditorDialog extends JDialog implements ActionListener {
	JPanel centerPanel;
	JButton okButton;
	JButton cancelButton;
	boolean exitStatus;
	
	/** Creates the dialog. */
	public TextEditorDialog() {
		JPanel buttonPanel = new JPanel();
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		buttonPanel.add(okButton);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		setModal(true);
		setSize(400, 400);
	}
	
	/** Also updates the exit status. */
	public void setVisible(boolean b) {
		if (b) exitStatus = false;
		super.setVisible(b);
	}

	/**
	 * Calls {@link #show(String, String[], String[], int)} - all but the
	 * last value are shown in text fields.
	 */
	public boolean show(String title, String[] labels, String[] values) {
		return show(title, labels, values, labels.length - 1);
	}
	
	/**
	 * Shows a dialog, which displays the specified labels and value texts
	 * and supports value editing. The first <code>propCount</code> values are
	 * displayed in text fields, for the other values, text areas are
	 * created within a tabbed pane.
	 * Note, that the number of labels and values must be equal.
	 * @param values Initially contains defaults, values can then be
	 *        changed by the user.
	 * @return Value true, if values have been changed and OK has been selected.
	 */
	public boolean show(String title, String[] labels, String[] values, int propCount) {
		boolean valuesChanged = false;
		JTextField[] textFields = new JTextField[propCount];
		TextAreaWithUndo[] textAreas = new TextAreaWithUndo[values.length - propCount];
		
		setTitle(title);
		if (centerPanel != null)
			getContentPane().remove(centerPanel);
		centerPanel = new JPanel(new GridBagLayout());
		if (textAreas.length > 0)
			getContentPane().add(centerPanel, BorderLayout.CENTER);
		else
			getContentPane().add(new JScrollPane(centerPanel), BorderLayout.CENTER);
		
		if (textFields.length > 0) {
			GridBagConstraints c1 = new GridBagConstraints();
			GridBagConstraints c2 = new GridBagConstraints();
			c1.fill = GridBagConstraints.HORIZONTAL;
			c1.weightx = 0.1;
			c1.gridx = 0;
			c1.insets = new Insets(5,5,0,5);
			c2.fill = GridBagConstraints.HORIZONTAL;
			c2.weightx = 0.9;
			c2.gridx = 1;
			c2.insets = new Insets(5,5,0,5);
			for (int i = 0; i < textFields.length; i++) {
				JLabel label = new JLabel(labels[i]);
				label.setHorizontalAlignment(JLabel.RIGHT);
				c1.gridy = i;
				centerPanel.add(label, c1);
				textFields[i] = new JTextField(values[i]);
				c2.gridy = i;
				centerPanel.add(textFields[i], c2);
			}
		}
		if (textAreas.length > 0) {
			JTabbedPane tabbedPane = new JTabbedPane();
			GridBagConstraints c3 = new GridBagConstraints();
			c3.gridx = 0;
			c3.gridy = propCount;
			c3.weightx = 1.0;
			c3.weighty = 1.0;
			c3.gridwidth = 2;
			c3.fill = GridBagConstraints.BOTH;
			c3.insets = new Insets(5,0,0,0);
			centerPanel.add(tabbedPane, c3);
			for (int i = 0; i < textAreas.length; i++) {
				textAreas[i] = new TextAreaWithUndo(false);
				textAreas[i].setText(values[propCount + i]);
				JScrollPane scroller = new JScrollPane(textAreas[i]);
				tabbedPane.addTab(labels[propCount + i], scroller);
			}
		}
		
		setVisible(true);
		
		if (exitStatus) {
			for (int i = 0; i < values.length; i++) {
				String text = (i < propCount)
				? textFields[i].getText() : textAreas[i - propCount].getText();
				if (!values[i].equals(text)) {
					values[i] = text;
					valuesChanged = true;
				}
			}
		}
		return exitStatus && valuesChanged;
	}
	
	/** Also updates the exit status. */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == okButton)
			exitStatus = true;
		if (e.getSource() == okButton || e.getSource() == cancelButton)
			setVisible(false);
	}

	// for testing
//	public static void main(String[] args) {
//		TextEditorDialog dialog = new TextEditorDialog();
//		String[] values = new String[]{"value a", "value b", "value c", "value d"};
//		for (int i = 0; i <=4; i++) {
//			boolean result = dialog.show("Editor Test",
//					new String[]{"label a", "label b", "label c", "label d"},
//					values, i);
//			System.out.println(result);
//			System.out.println(Arrays.asList(values));
//		}
//		System.exit(0);
//	}
}
