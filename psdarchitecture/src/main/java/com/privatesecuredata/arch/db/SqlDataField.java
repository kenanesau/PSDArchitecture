package com.privatesecuredata.arch.db;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * Class used by @see AutomaticPersister to save the SQL-data-fields
 * 
 * @author kenan
 *
 */
public class SqlDataField {

    public enum SqlFieldType
	{
        BOOLEAN,
		INTEGER,
		LONG, 
		STRING,
		FLOAT,
		DOUBLE,
		DATE,
		OBJECT_REFERENCE,
        OBJECT_NAME,
		COLLECTION_REFERENCE,
	}
	
	private String _tableName = null;
	private String _name;
	private SqlFieldType _type;
	private Field _field;
	private boolean _mandatory = false;
	private Class<?> _referencedType = null;

    /**
     * Only set if this is a SqlDataField describing  a composed field.
     * ComposeField is the field in the parent object which points to the object whose fields
     * are composed to the data of the parent.
     */
    private Field _composeField = null;
    private String _id;
	
	protected SqlDataField() {}

	public SqlDataField(Field composeParentField, SqlDataField composedField) {
        _tableName = null;
        this._field = composedField._field;
        this._type = composedField.getSqlType();
        this._name = DbNameHelper.getComposedFieldName(composeParentField.getName(), composedField._field.getName(), this._type);
        this._composeField = composeParentField;
        this._composeField.setAccessible(true);
	}

	public SqlDataField(String table, String name, SqlFieldType type)
	{
		_tableName = ((table == null) ? null : table.toLowerCase(Locale.US));
        if (type == type.OBJECT_NAME)
        {
            _name = DbNameHelper.getFieldName(name, type);
        }
        else
		    _name = name;
		_type = type;
	}
	
	public SqlDataField(String name, SqlFieldType type)
	{
		this(null, name, type);
	}
	
	public SqlDataField(Field field, Class<?> referencedType)
	{
		this(field);
		if (Collection.class.isAssignableFrom(field.getType())) {
            //It's a Collection-Proxy-Size-field
            _referencedType = referencedType;
        }
        else {
            //It's a Object-Name of a OneToMany-relationship
            _type = SqlFieldType.OBJECT_NAME;
            _name = DbNameHelper.getFieldName(field, _type);
        }
	}
	
	public SqlDataField(Field field)
	{
		_field = field;

		Class<?> fieldType = field.getType();

        if ( (fieldType.equals(boolean.class)) || (fieldType.equals(Boolean.class)) )
        {
            _type = SqlFieldType.BOOLEAN;
        }
		else if ( (fieldType.equals(int.class)) || (fieldType.equals(Integer.class)) )
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
				_type = SqlFieldType.OBJECT_REFERENCE;
			else if (Collection.class.isAssignableFrom(fieldType))
				_type = SqlFieldType.COLLECTION_REFERENCE;
			else
				throw new ArgumentException("FATAL: Could not determine type of SqlField!");
		}
		
		_name = DbNameHelper.getFieldName(field, _type);
			
	}

    public boolean isComposition() {
        return _composeField!=null;
    }

    public Field getComposeField() {
        return _composeField;
    }

    protected void setName(String name) { _name = name; }
	public String getSqlName() { return _name; }
	
	protected void setSqlType(SqlFieldType type) { _type = type; }
	public SqlFieldType getSqlType() { return _type; }
	
	public Class<?> getReferencedType() {
		if (_type != SqlFieldType.COLLECTION_REFERENCE)
			throw new DBException("Can not return referenced type since this is no Collection-reference");
		
		return _referencedType;
	}
	
	public String getSqlTypeString() 
	{
		switch (_type)
		{
            case BOOLEAN:
                return "INTEGER";
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
            case OBJECT_REFERENCE:
                return "INTEGER";
            case OBJECT_NAME:
                return "TEXT";
            case COLLECTION_REFERENCE:
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
	
	public Field getObjectField() { return _field; }
    public void setObjectField(Field fld) { _field = fld; }

    public String getId() { return _id; }
    public void setId(String id) { _id = id; }

	public boolean isMandatory() {
		return _mandatory;
	}
	public void setMandatory() { _mandatory = true; }
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("table", ((_tableName == null) ? "null" : _tableName))
				.add("fieldName", ((_name == null) ? "null" : _name))
				.add("fieldType", ((_type == null) ? "null" : _type))
				.add("sqlType", getSqlTypeString())
				.toString();
	}
	

}
