/*
 * Copyright (C) 2008-2013 Ruediger Lunde
 * Licensed under the GNU General Public License, Version 3
 */
package rl.sqltrainer.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Class, responsible for communication with a database using JDBC. It maintains
 * connection data and provides a method to execute a query.
 * 
 * @author Ruediger Lunde
 */
public class DBConnectionHandler {
	private String jdbcDriver =
		"com.microsoft.sqlserver.jdbc.SQLServerDriver"; // example
	private String jdbcUrl = 
		"jdbc:sqlserver://SERVER;databaseName=DBNAME"; // example
	private String server;
	private String db;
	private String user;
	private String password;

	/**
	 * Changes JDBC driver settings.
	 * 
	 * @param driver
	 *            Class name of the JDBC driver to be used.
	 * @param protocol
	 *            Name of the vendor specific JDBC sub protocol to be used.
	 */
	public void setJdbcDriverData(String driver, String url) {
		jdbcDriver = driver;
		jdbcUrl = url;
	}

	/** Modifies the connection data settings. */
	public void setConnectionData(String server, String db, String user,
			String passwd) {
		this.server = server;
		this.db = db;
		this.user = user;
		this.password = passwd;
	}

	/** Returns server name, database name, user name, and password. */
	public String[] getConnectionData() {
		return new String[] { server, db, user, password };
	}

	/**
	 * Connects to the database, executes the specified SQL query, and returns
	 * the result in nested list of strings. Note that exception handling is not
	 * included. It is delegated to the caller.
	 */
	public List<List<String>> executeStatement(String statement)
			throws ClassNotFoundException, SQLException {
		List<List<String>> result = new ArrayList<List<String>>();
		Connection con = null;
		Statement stmt = null;
		ResultSet res = null;
		try {
			// for debugging only
			// DriverManager.setLogWriter(new java.io.PrintWriter(System.out));

			// needed because our current VPN client does not support IPv6... 
			System.setProperty("java.net.preferIPv4Stack", "true");
			
			// make sure that JDBC driver is loaded
			Class.forName(jdbcDriver);

			// set up the connection to the database
			String url = jdbcUrl;
			url = url.replace("SERVER", server);
			url = url.replace("DBNAME", db);
			
			con = DriverManager.getConnection(url, user, password);

			// via a JDBC/ODBC bridge:
			// Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			// con = DriverManager.getConnection("jdbc:odbc:DS_DABAS_MD2_A");

			// generate a statement
			stmt = con.createStatement();
			// execute statement and create result
			res = stmt.executeQuery(statement);

			ResultSetMetaData metadata = res.getMetaData();
			int colCount = metadata.getColumnCount();
			List<String> row = new ArrayList<String>(colCount);
			for (int i = 0; i < colCount; i++)
				row.add(metadata.getColumnName(i + 1));
			result.add(row);
			// position the cursor
			while (res.next()) {
				row = new ArrayList<String>(colCount);
				// retrieve the result
				for (int i = 0; i < colCount; i++)
					row.add(res.getString(i + 1));
				result.add(row);
			}
		} finally {
			// close everything down
			if (res != null)
				res.close();
			if (stmt != null)
				stmt.close();
			if (con != null)
				con.close();
		}
		return result;
	}
}
