package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.exceptions.DBException;

import android.database.Cursor;

/**
 * This Cursorloader tries to load a cursor which is retrieved from a SQL-statement
 * of the form of "Select * from table where foreignKeyId=...".
 * 
 * The DbId<?>-property of the persistable object is used as a parameter to that query.
 * 
 * @author kenan
 *
 */
public class IdCursorLoader implements ICursorLoader {
	private PersistanceManager pm;
	private String dbName;
	private String foreignKeyColumn;
	
	public IdCursorLoader(PersistanceManager pm, String dbName, String foreignKeyColumn)
	{
		this.pm = pm;
		this.dbName = dbName;
		this.foreignKeyColumn = foreignKeyColumn;
	}
	
	@Override
	public Cursor getCursor(IPersistable<?> foreignKey) {
		String sqlQuery=null;
		if (null==foreignKey)
			sqlQuery = String.format("select * from %s where %s is null", dbName, foreignKeyColumn);
		else if (null != foreignKey.getDbId()) {
			String param = Long.valueOf(foreignKey.getDbId().getId()).toString();
			sqlQuery = String.format("select * from %s where %s=%s", dbName, foreignKeyColumn, param);
		} else
			throw new DBException("foreign-key not yet saved to DB, so it is not possible to use it in a query!!!");
		
		return pm.getDb().rawQuery(sqlQuery, new String[] {});
	}
}
