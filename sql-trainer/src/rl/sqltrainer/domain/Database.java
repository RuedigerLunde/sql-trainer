/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.domain;

import java.io.File;

/**
 * Maintains all those properties of a database, which are needed by the
 * SQLTrainer to access it properly.
 * @author Ruediger Lunde
 */
public class Database {
	/** Logical name of the database. */
	private String logicalName;
	/**
	 * Server-side physical name of the database. For administrative reasons,
	 * it sometimes might be convenient, to put more than one small training
	 * database into the same physical database.
	 */
	private String physicalName;
	/** File containing a schema image of the database. */
	private File schema;
	/** File containing a textual description of the database. */
	private File description;

	/** Standard constructor. */
	public Database(String logName, String physName, File schema, File desc) {
		this.logicalName = logName;
		this.physicalName = physName;
		this.schema = schema;
		this.description = desc;
	}
	
	/** Returns the logical name of the database. */
	public String getLogicalName() {
		return logicalName;
	}
	/** Returns the physical (server-side) name of the database. */
	public String getPhysicalName() {
		return physicalName;
	}
	/** Returns a File for accessing a corresponding schema image or null. */
	public File getSchema() {
		return schema;
	}
	/** Returns a File for accessing a corresponding description text or null. */
	public File getDescription() {
		return description;
	}
}
