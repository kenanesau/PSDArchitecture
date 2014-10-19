package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
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
	
	public enum SqlType 
	{
		INTEGER,
		LONG, 
		STRING,
		FLOAT,
		DOUBLE,
		DATE,
	}
	
	private String _tableName;
	private String _name;
	private SqlType _type;
	private Field _field;
	private boolean _mandatory = false;
	
	protected SqlDataField() {}
	
	public SqlDataField(String table, String name, SqlType type)
	{
		_tableName = table.toLowerCase(Locale.US);
		_name = name;
		_type = type;
	}
	
	public SqlDataField(String name, SqlType type)
	{
		this("", name, type);
	}
	
	public SqlDataField(Field field)
	{
		_field = field;
		_name = String.format("fld_%s", field.getName().toLowerCase(Locale.US));

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
	
	protected void setName(String name) { _name = name; }
	public String getName() { return _name; }
	
	protected void setSqlType(SqlType type) { _type = type; }
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
				.add("Table", ((_tableName == null) ? "null" : _tableName))
				.add("Field", ((_name == null) ? "null" : _name))
				.add("Type", getSqlTypeString())
				.toString();
	}
	

}
