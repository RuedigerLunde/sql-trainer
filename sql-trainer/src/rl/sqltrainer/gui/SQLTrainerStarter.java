/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.gui;

import java.io.File;
import java.io.PrintStream;
import java.util.Locale;

import rl.sqltrainer.application.SQLTrainer;
import rl.util.exceptions.ErrorHandler;
import rl.util.persistence.PropertyManager;

/**
* Starts a tool to support SQL select statement training for DABAS-MD2.
* Statements can be formulated and executed. The feedback from the
* database server is shown in a table. Exercises can be loaded into
* the application, answered by the user, and saved again.
* On code level, the implementation demonstrates several technical aspects
* including jdbc, xml, and design patterns. 
* @author Ruediger Lunde
*/
public class SQLTrainerStarter {
	/**
	  * Starts the tool.
	  * @param args Not used!
	  */
	public static void main(String[] args) { // TODO: exercise path!
		Locale.setDefault(Locale.US);
		// set file system paths for properties and data
		try {
			File configDir = new File("sql-trainer-config");
			if (configDir.exists())
				PropertyManager.setApplicationDataDirectory(configDir);
			// Otherwise use the current directory as application data directory!
			String home = System.getProperty("user.home");
			File userDir = new File(home, ".sql-trainer");
			if (!userDir.exists())
				userDir.mkdir();
			PropertyManager.setUserDataDirectory(userDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		SQLTrainer app = new SQLTrainer();
		SQLTrainerModel model = new SQLTrainerModel(app);
		SQLTrainerController controller = new SQLTrainerController(app);
		SQLTrainerFrame view = new SQLTrainerFrame();
		controller.setModel(model);
		controller.setFrame(view);
		view.setModel(model);
		view.setController(controller);
		model.addModelListener(view);
		view.resetSelectionState();
		view.setVisible(true);
		System.setOut(new PrintStream(view.getLogStream()));
		ErrorHandler.setInstance(view.createErrorHandler());
        //ErrorHandler.enableDebugMode(true);
	}
}

