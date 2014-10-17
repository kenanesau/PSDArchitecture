package com.privatesecuredata.arch.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.R.integer;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

public class AutomaticPersister<T extends IPersistable<T>> extends AbstractPersister<T> {
	
	public enum SqlType 
	{
		INTEGER,
		LONG, 
		STRING,
		FLOAT,
		DOUBLE,
		DATE,
	}
	
	protected class SqlField {
		private String _tableName;
		private String _name;
		private SqlType _type;
		private Field _field;
		private boolean _mandatory = false;
		
		public SqlField(String table, String name, SqlType type)
		{
			_tableName = table;
			_name = name;
			_type = type;
		}
		
		public SqlField(String name, SqlType type)
		{
			this("", name, type);
		}
		
		public SqlField(Field field)
		{
			_field = field;
			_name = String.format("fld_%s", field.getName());

			Class<?> fieldType = field.getType();

			if ((fieldType.equals(int.class)) || (fieldType.equals(Integer.class)))
			{
				_type =  SqlType.INTEGER;
			} 
			else if ((fieldType.equals(long.class)) || (fieldType.equals(Long.class)))
			{
				_type =  SqlType.LONG;
			}
			else if ((fieldType.equals(float.class)) || (fieldType.equals(Float.class)))
			{
				_type =  SqlType.FLOAT;
			}
			else if ((fieldType.equals(double.class)) || (fieldType.equals(Double.class)))
			{
				_type =  SqlType.DOUBLE;
			}
			else if (fieldType.equals(Date.class))
			{
				_type =  SqlType.DATE;
			}
			else if (fieldType.equals(String.class))
			{
				_type =  SqlType.STRING;
			}
			else
				throw new ArgumentException("FATAL: Could not determine type of SqlField!");
		}
		
		public String getName() { return _name; }
		
		public SqlType getSqlType() { return _type; }
		
		public String getSqlTypeString() 
		{
			switch (_type)
			{
			case INTEGER:
				return "INTEGER";
			case LONG:
				return "INTEGER";
			case FLOAT:
				return "REAL";
			case DOUBLE:
				return "REAL";
			case STRING:
				return "TEXT";
			case DATE:
				return "TEXT";
			default:
				return "";
			}
		}

		public String getTableName() {
			return _tableName;
		}

		public void setTableName(String _tableName) {
			this._tableName = _tableName;
		}
		
		public Field getField() { return _field; }

		public boolean isMandatory() {
			return _mandatory;
		}
		public void setMandatory() { _mandatory = true; }
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("Table", _tableName)
					.add("Field", _name)
					.add("Type", getSqlTypeString())
					.toString();
		}
	}
	
	SQLiteStatement insert;
	SQLiteStatement update;
	SQLiteStatement updateForeignKey;
	
	private String _tableName;
	private List<SqlField> _tableFields;
	Constructor<T> _const;
	Class<T> _persistentType;
	
	
	public AutomaticPersister(Class<T> persistentType) throws Exception
	{
		_tableName = persistentType.getSimpleName();
		_persistentType = persistentType;
				
		Field[] fields = persistentType.getDeclaredFields();
		_const = persistentType.getConstructor((Class<?>[])null);
			
		_tableFields = new ArrayList<SqlField>();
		for (Field field : fields)
		{
			DbField anno = field.getAnnotation(DbField.class);
			if (null == anno)
				continue;
			
		
			SqlField sqlField = new SqlField(field); 
			if (anno.isMandatory())
				sqlField.setMandatory();
			
			_tableFields.add(sqlField);
		}

		if (_tableFields.isEmpty())
			throw new Exception(String.format("No DBField-annotation found in type \"%s\"", persistentType.getName()));
	}
	
	@Override
	public String getTableName() {
		return _tableName;
	}
	
	protected String getAddStatement()
	{
		StringBuilder sql = new StringBuilder("INSERT INTO ")
			.append(getTableName())
			.append(" ( ");
		
		int i=1;
		for(SqlField fld : _tableFields)
		{
			sql.append(fld.getName());
			
			if (i < _tableFields.size())
			  sql.append(", ");
			
			i++;
		}
		
		sql.append(" ) VALUES ( ");
		
		for (i = 0; i<_tableFields.size(); i++)
		{
			sql.append("?");
			if (i + 1 < _tableFields.size())
				sql.append(", ");
			else
				sql.append(" ");
		}
		
		sql.append(")");
		return sql.toString();
	}
	
	protected String getUpdateStatement()
	{
		StringBuilder sql = new StringBuilder("UPDATE ")
			.append(getTableName())
			.append(" SET ");
		
		int i = 1;
		for (SqlField field : _tableFields)
		{
			sql.append(field.getName());
			sql.append("=? ");
			if (i < _tableFields.size())
				sql.append(", ");
			i++;
		}
		
		sql.append("WHERE _id=?");
		
		return sql.toString();
	}
	
	protected String getCreateStatement()
	{
		StringBuilder sql = new StringBuilder("CREATE TABLE ")
			.append(getTableName())
			.append(" ( ")
			.append("_id INTEGER PRIMARY KEY AUTOINCREMENT");
		
		for (SqlField field : _tableFields)
		{
			sql.append(", ")
				.append(field.getName()).append(" ")
				.append(field.getSqlTypeString()).append(" ");
			
			if (field.isMandatory())
				sql.append("NOT NULL");
		}
		
		sql.append(" ) ");

		return sql.toString();
	}

	@Override
	public void init(Object obj) throws DBException {
		setPM((PersistanceManager)obj);
		
		insert = getDb().compileStatement(getAddStatement());
		update = getDb().compileStatement(getUpdateStatement());
	}
	
	public void bind(SQLiteStatement sql, int idx, SqlField field, T persistable) throws IllegalAccessException, IllegalArgumentException
	{
		Field fld = field.getField();
		fld.setAccessible(true);
		
		switch(field.getSqlType())
		{
		case STRING:
		case DATE:
			Object val = fld.get(persistable);
			String valStr = null;
			if (null != val)
				valStr = val.toString();
			
			bind(sql, idx, valStr);
			break;
		case DOUBLE:
			bind(sql, idx, fld.getDouble(persistable));
			break;
		case FLOAT:
			bind(sql, idx, fld.getFloat(persistable));
			break;
		case INTEGER:
			bind(sql, idx, fld.getInt(persistable));
			break;
		case LONG:
			bind(sql, idx, fld.getLong(persistable));
			break;
		default:
			break;
		}
	}
	
	public void bind(SQLiteStatement sql, T persistable)
	{
		sql.clearBindings();
		
		int i=1;
		try {
			for (SqlField field : _tableFields) {
				bind(sql, i++, field, persistable);
			}
		} 
		catch (Exception e) 
		{
			throw new DBException(
					String.format("Error binding to SQL statement \"%s\"", sql.toString()), e);
		}
	}

	@Override
	public long insert(T persistable) throws DBException {
		bind(insert, persistable);
			
		return insert.executeInsert();
	}

	@Override
	public void update(T persistable) throws DBException {
		bind(update, persistable);
		
		update.executeUpdateDelete();
	}

	@Override
	public void updateForeignKey(T persistable, DbId<?> foreignId)
			throws DBException {
		// TODO Auto-generated method stub
	}

	@Override
	public T rowToObject(int pos, Cursor csr) throws DBException {
		T obj = null;
		SqlField field = null;
		
		try {
			obj = _const.newInstance();
			csr.moveToPosition(pos);
			
			obj.setDbId(new DbId<T>(csr.getLong(0)));
			for (int colIndex=1; colIndex<csr.getColumnCount(); colIndex++)
			{
				field = _tableFields.get(colIndex - 1);
				//int colIndex = csr.getColumnIndex(field.getName());
				
				Field fld = field.getField();
				fld.setAccessible(true);
				switch(field.getSqlType())
				{
				case DATE:
					String dateStr = csr.getString(colIndex);
					java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
					Date val = null;
					if (null != dateStr)
						val = df.parse(dateStr);
					fld.set(obj, val);
					break;
				case STRING:
					fld.set(obj, csr.getString(colIndex));
					break;
				case DOUBLE:
					fld.set(obj, csr.getDouble(colIndex));
					break;
				case FLOAT:
					fld.set(obj, csr.getFloat(colIndex));
					break;
				case INTEGER:
					fld.set(obj, csr.getInt(colIndex));
					break;
				case LONG:
					fld.set(obj, csr.getLong(colIndex));
					break;
				default:
					throw new DBException("Unknow data-type");
				}
			}
		} catch (Exception e) {
			if (field != null) {
				throw new DBException(
					String.format("Error converting value for Field \"%s\" in object of type \"%s\"", 
							field.getName(),
							_persistentType.getName()), e);
			}
			else {
				throw new DBException(
						String.format("Error converting Cursor-row to object of type \"%s\"", 
								_persistentType.getName()), e);
			}
		} 
		
		return obj;
	}

}
