package com.privatesecuredata.arch.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract.Data;

import com.privatesecuredata.arch.db.annotations.DbField;
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

			if ((fieldType.equals(integer.class)) || (fieldType.equals(Integer.class)))
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
			else if (fieldType.equals(Data.class))
			{
				_type =  SqlType.DATE;
			}
			else if (fieldType.equals(String.class))
			{
				_type =  SqlType.STRING;
			}
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
		
		for(SqlField fld : _tableFields)
		{
			sql.append(fld.getName());
		}
		
		sql.append(" ) VALUES ( ");
		
		for (int i = 0; i<_tableFields.size(); i++)
			sql.append("? ");
		
		sql.append(" )");
		return sql.toString();
	}
	
	protected String getUpdateStatement()
	{
		StringBuilder sql = new StringBuilder("UPDATE ")
			.append(getTableName())
			.append(" SET ");
		
		for (SqlField field : _tableFields)
		{
			sql.append(field.getName());
			sql.append("=? ");
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
				sql.append("NOT NULL ");
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
		switch(field.getSqlType())
		{
		case STRING:
		case DATE:
			bind(sql, idx, field.getField().get(persistable).toString());
			break;
		case DOUBLE:
			bind(sql, idx, field.getField().getDouble(persistable));
			break;
		case FLOAT:
			bind(sql, idx, field.getField().getFloat(persistable));
			break;
		case INTEGER:
			bind(sql, idx, field.getField().getInt(persistable));
			break;
		case LONG:
			bind(sql, idx, field.getField().getLong(persistable));
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
		
		try {
			obj = _const.newInstance();
			csr.moveToPosition(pos);
			
			obj.setDbId(new DbId<T>(csr.getLong(0)));
			for (int colIndex=1; colIndex<=csr.getColumnCount(); colIndex++)
			{
				SqlField field = _tableFields.get(colIndex);
				switch(field.getSqlType())
				{
				case DATE:
					String dateStr = csr.getString(colIndex);
					java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
						field.getField().set(obj, df.parse(dateStr));
					break;
				case STRING:
					field.getField().set(obj, csr.getString(colIndex));
					break;
				case DOUBLE:
					field.getField().set(obj, csr.getDouble(colIndex));
					break;
				case FLOAT:
					field.getField().set(obj, csr.getFloat(colIndex));
					break;
				case INTEGER:
					field.getField().set(obj, csr.getInt(colIndex));
					break;
				case LONG:
					field.getField().set(obj, csr.getLong(colIndex));
					break;
				default:
					throw new DBException("Unknow data-type");
				}
			}
		} catch (Exception e) {
			throw new DBException(
					String.format("Error converting Curor-row to object of type %s", 
					_persistentType.getName()), e);
		} 
		
		return obj;
	}

}
