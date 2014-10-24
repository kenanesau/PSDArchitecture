package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.exceptions.ArgumentException;

/**
 * Class used by @see AutomaticPersister to save the SQL-data-fields
 * 
 * @author kenan
 *
 */
public class SqlDataField {
	
	public enum SqlFieldType 
	{
		INTEGER,
		LONG, 
		STRING,
		FLOAT,
		DOUBLE,
		DATE,
		REFERENCE,
		COLLECTION_PROXY_SIZE,
	}
	
	private String _tableName;
	private String _name;
	private SqlFieldType _type;
	private Field _field;
	private boolean _mandatory = false;
	
	protected SqlDataField() {}
	
	public SqlDataField(String table, String name, SqlFieldType type)
	{
		_tableName = table.toLowerCase(Locale.US);
		_name = name;
		_type = type;
	}
	
	public SqlDataField(String name, SqlFieldType type)
	{
		this("", name, type);
	}
	
	public SqlDataField(Field field)
	{
		_field = field;
		_name = DbNameHelper.getFieldName(field);

		Class<?> fieldType = field.getType();

		if ((fieldType.equals(int.class)) || (fieldType.equals(Integer.class)))
		{
			_type = SqlFieldType.INTEGER;
		} 
		else if ((fieldType.equals(long.class)) || (fieldType.equals(Long.class)))
		{
			_type = SqlFieldType.LONG;
		}
		else if ((fieldType.equals(float.class)) || (fieldType.equals(Float.class)))
		{
			_type = SqlFieldType.FLOAT;
		}
		else if ((fieldType.equals(double.class)) || (fieldType.equals(Double.class)))
		{
			_type = SqlFieldType.DOUBLE;
		}
		else if (fieldType.equals(Date.class))
		{
			_type = SqlFieldType.DATE;
		}
		else if (fieldType.equals(String.class))
		{
			_type = SqlFieldType.STRING;
		}
		else {
			if (IPersistable.class.isAssignableFrom(fieldType))
				_type = SqlFieldType.REFERENCE;
			else if (Collection.class.isAssignableFrom(fieldType))
				_type = SqlFieldType.COLLECTION_PROXY_SIZE;
			else
				throw new ArgumentException("FATAL: Could not determine type of SqlField!");
		}
			
	}
	
	protected void setName(String name) { _name = name; }
	public String getName() { return _name; }
	
	protected void setSqlType(SqlFieldType type) { _type = type; }
	public SqlFieldType getSqlType() { return _type; }
	
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
		case REFERENCE:
			return "INTEGER";
		case COLLECTION_PROXY_SIZE:
			return "INTEGER";
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
				.add("Table", ((_tableName == null) ? "null" : _tableName))
				.add("FieldName", ((_name == null) ? "null" : _name))
				.add("FieldType", _type)
				.add("SqlType", getSqlTypeString())
				.toString();
	}
	

}
