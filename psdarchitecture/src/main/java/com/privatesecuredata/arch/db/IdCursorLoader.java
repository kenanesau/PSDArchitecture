package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.List;

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

    public IdCursorLoader(PersistanceManager pm, Class<?> persistentType, Class<?> referencedType, List<SqlDataField> fields)
    {
        String table = DbNameHelper.getTableName(referencedType);
        String foreignKeyColumn = DbNameHelper.getForeignKeyFieldName(persistentType);

        init (pm, table, foreignKeyColumn, fields);
    }
	
	public IdCursorLoader(PersistanceManager pm, String table, String foreignKeyColumn)
	{
		init(pm, table, foreignKeyColumn, null);
	}
	
	public IdCursorLoader(PersistanceManager pm, Class<?> referencingType, Class<?> referencedType)
	{
		String table = DbNameHelper.getTableName(referencedType);
		String foreignKeyColumn = DbNameHelper.getForeignKeyFieldName(referencingType);
		
		init(pm, table, foreignKeyColumn, null);
	}
	
	protected void init(PersistanceManager pm, String table, String foreignKeyColumn, List<SqlDataField> fields)
	{
		this._pm = pm;
		this._tableName = table;
		this._foreignKeyColumn = foreignKeyColumn;

        if (null == fields) {
            _selectAllRawString = String.format("SELECT * FROM %s WHERE %s IS NULL", _tableName, _foreignKeyColumn);
            _selectIdRawString = String.format("SELECT * FROM %s WHERE %s=", _tableName, _foreignKeyColumn);
        }
        else {
            StringBuilder sqlSelectAll = new StringBuilder("SELECT _id ");
            StringBuilder sqlSelectId = new StringBuilder("SELECT _id ");

            for(SqlDataField field : fields) {
                sqlSelectAll.append(", ")
                        .append(field.getName());
                sqlSelectId.append(", ")
                        .append(field.getName());
            }

            sqlSelectAll.append(" FROM ")
                    .append(_tableName)
                    .append(" WHERE ")
                    .append(_foreignKeyColumn)
                    .append(" IS NULL");

            sqlSelectId.append(" FROM ")
                    .append(_tableName)
                    .append(" WHERE ")
                    .append(_foreignKeyColumn)
                    .append("=");

            _selectAllRawString = sqlSelectAll.toString();
            _selectIdRawString = sqlSelectId.toString();
        }

	}

    protected String getBaseQuery(DbId<?> foreignKey) {
        String sqlQuery=null;
        if (null == foreignKey) {
            sqlQuery = _selectAllRawString;
        }
        else if (null != foreignKey) {
            String param = Long.valueOf(foreignKey.getId()).toString();
            sqlQuery = _selectIdRawString.concat(param);
        } else
            throw new DBException("Foreign-key not yet saved to DB, so it is not possible to use it in a query!!!");

        return sqlQuery;
    }

	@Override
    public Cursor getCursor(DbId<?> foreignKey)
    {
        return _pm.getDb().rawQuery(getBaseQuery(foreignKey), new String[] {});
    }

    @Override
    public Cursor getCursor(DbId<?> foreignKey, OrderByTerm... orderByTerms) {
        if (null == orderByTerms)
            return getCursor(foreignKey);

        StringBuilder sb = AbstractPersister.appendOrderByString(new StringBuilder(getBaseQuery(foreignKey)), orderByTerms);
        return _pm.getDb().rawQuery(sb.toString(), new String[] {});
    }
}
