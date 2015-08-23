package com.privatesecuredata.arch.db;

import java.util.List;

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
	private PersistanceManager _pm;
	private String _tableName;
	private String _foreignKeyColumn;
    private List<SqlDataField> _fields;

	private String _selectAllRawString;
	private String _selectIdRawString;

    /**
     * Used by PartialClassReader
     * @param pm
     * @param referencingType
     * @param referencedType
     * @param fields
     */
    public IdCursorLoader(PersistanceManager pm, Class referencingType, Class referencedType, List<SqlDataField> fields)
    {
        init (pm, referencedType, referencingType, fields);
    }

    /**
     * Used by Hand-made persisters
     * @param pm
     * @param table
     * @param foreignKeyColumn
     */
	/*public IdCursorLoader(PersistanceManager pm, String table, String foreignKeyColumn)
	{
		init(pm, table, foreignKeyColumn, null);
	}*/

    /**
     * Used by AutomaticPersister
     *
     * @param pm
     * @param referencingType
     * @param referencedType
     */
	public IdCursorLoader(PersistanceManager pm, Class<?> referencingType, Class<?> referencedType)
	{
		String table = DbNameHelper.getTableName(referencedType);
		String foreignKeyColumn = DbNameHelper.getForeignKeyFieldName(referencingType);
        IPersister persister = pm.getPersister((Class)referencedType);

		init(pm, table, foreignKeyColumn, persister.getSqlFields());
	}

    protected void init(PersistanceManager pm, String tableName, String foreignKeyColumn, List<SqlDataField> fields)
    {
        this._pm = pm;
        this._tableName = tableName;
        this._foreignKeyColumn = foreignKeyColumn;
        this._fields = fields;

        StringBuilder sbSelectAll = AbstractPersister.createSelectAllStatement(_tableName, fields, null);
        StringBuilder sbSelectId = new StringBuilder(sbSelectAll.toString());

        sbSelectAll.append(" WHERE ")
                .append(_foreignKeyColumn)
                .append(" IS NULL");

        sbSelectId.append(" WHERE ")
                .append(_foreignKeyColumn)
                .append("=");

        _selectAllRawString = sbSelectAll.toString();
        _selectIdRawString = sbSelectId.toString();
        /*
        if (null == fields) {
            sbSelectAll.append(" WHERE ").append(_foreignKeyColumn).append(" IS NULL");
            sbSelectId.append(" WHERE ").append(_foreignKeyColumn).append("=");
        }

        else {
            StringBuilder sqlSelectAll = new StringBuilder("SELECT _id ");
            StringBuilder sqlSelectId = new StringBuilder("SELECT _id ");

            for(SqlDataField field : fields) {
                sqlSelectAll.append(", ")
                        .append(field.getSqlName());
                sqlSelectId.append(", ")
                        .append(field.getSqlName());
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
        */
    }

	protected void init(PersistanceManager pm, Class referencedType, Class referencingType, List<SqlDataField> fields)
	{
        init(pm, DbNameHelper.getTableName(referencedType), DbNameHelper.getForeignKeyFieldName(referencingType), fields);
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
        return _pm.getDb().rawQuery(getBaseQuery(foreignKey), new String[]{});
    }

    @Override
    public Cursor getCursor(DbId<?> foreignKey, OrderByTerm... orderByTerms) {
        if (null == orderByTerms)
            return getCursor(foreignKey);

        StringBuilder sb = AbstractPersister.appendOrderByString(
                AbstractPersister.createSelectAllStatement(_tableName,
                        _fields,
                        orderByTerms),
                orderByTerms);
        return _pm.getDb().rawQuery(sb.toString(), new String[]{});
    }
}
