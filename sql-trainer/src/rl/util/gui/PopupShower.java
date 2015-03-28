/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.util.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Useful helper class for showing popup menus. The
 * idea is taken from the java tutorial "How to Use Menus".
 * @author Ruediger Lunde
 */
public class PopupShower extends MouseAdapter {
	JPopupMenu popup = new JPopupMenu();

	public PopupShower(JPopupMenu popup) {
		this.popup = popup;
	}
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }
    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
