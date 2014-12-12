package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;

import com.privatesecuredata.arch.exceptions.DBException;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

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
	private PersistanceManager _pm;
	private String _tableName;
	private String _foreignKeyColumn;
	private String _selectAllRawString;
	private String _selectIdRawString;
	
	public IdCursorLoader(PersistanceManager pm, String table, String foreignKeyColumn)
	{
		init(pm, table, foreignKeyColumn);
	}
	
	public IdCursorLoader(PersistanceManager pm, Class<?> persistentType, Class<?> referencedType) 
	{
		String table = DbNameHelper.getTableName(referencedType);
		String foreingKeyColumn = DbNameHelper.getForeignKeyFieldName(persistentType);
		
		init(pm, table, foreingKeyColumn);
	}
	
	protected void init(PersistanceManager pm, String table, String foreignKeyColumn)
	{
		this._pm = pm;
		this._tableName = table;
		this._foreignKeyColumn = foreignKeyColumn;
		
		_selectAllRawString = String.format("SELECT * FROM %s WHERE %s IS NULL", _tableName, _foreignKeyColumn);
		_selectIdRawString = String.format("SELECT * FROM %s WHERE %s=", _tableName, _foreignKeyColumn);
	}

	@Override
	public Cursor getCursor(IPersistable<?> foreignKey) {
		String sqlQuery=null;
		if (null == foreignKey) {
			sqlQuery = _selectAllRawString;
		}
		else if (null != foreignKey.getDbId()) {
			String param = Long.valueOf(foreignKey.getDbId().getId()).toString();
			sqlQuery = _selectIdRawString.concat(param);
		} else
			throw new DBException("Foreign-key not yet saved to DB, so it is not possible to use it in a query!!!");
		
		return _pm.getDb().rawQuery(sqlQuery, new String[] {});
	}
}
