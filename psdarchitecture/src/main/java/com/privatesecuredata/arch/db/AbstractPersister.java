package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import com.privatesecuredata.arch.exceptions.DBException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public abstract class AbstractPersister<T extends IPersistable> implements IPersister<T> {
	protected static final String DELSQLSTATEMENT = "DELETE FROM %s WHERE _id=?";
	protected static final String SELECTSINGLESQLSTATEMENT = "SELECT * FROM %s WHERE _id=?";
	protected static final String SELECTALLSQLSTATEMENT = "SELECT * FROM %s";
	
	private PersistanceManager pm;
	private SQLiteStatement delete;
    private String tableName;

    /**
     * Update-Statements for the item counts for the foreign key relations
     */
    private Hashtable<Field, SQLiteStatement> _foreignListCountUpdateStatements;


	protected String getSelectAllStatement() { return getSelectAllStatement(null); }
    protected String getSelectAllStatement(OrderByTerm... terms) { return String.format(SELECTALLSQLSTATEMENT, getTableName()); }
    protected String getDelSqlString() { return String.format(DELSQLSTATEMENT, getTableName()); }
    protected String getSelectSingleSqlString() { return String.format(SELECTSINGLESQLSTATEMENT, getTableName()); }

	@Override
	public void init(Object obj) {
        setPM((PersistanceManager) obj);

        /** Check if table exists ... **/
		Cursor cursor = getDb().rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '"+getTableName()+"'", null);
		if ( (null != cursor) && (cursor.getCount() > 0) ) {
			String delStatement = String.format(getDelSqlString(), getTableName());
			this.delete = getDb().compileStatement(delStatement);
		}
		if (null != cursor)
			cursor.close();

        this._foreignListCountUpdateStatements = new Hashtable<Field, SQLiteStatement>();
	}
	@Override
	public abstract long insert(T persistable) throws DBException;
	@Override
	public abstract long update(T persistable) throws DBException;
	@Override
	public abstract void updateForeignKey(T persistable, DbId<?> foreignId) throws DBException;
	@Override
	public abstract T rowToObject(int pos, Cursor csr) throws DBException;

    @Override
    public void delete(T persistable) throws DBException {
        delete.clearBindings();
        long _id = persistable.getDbId().getId();
        delete.bindLong(1, _id);
        int ret = delete.executeUpdateDelete();
        if (ret != 1)
            throw new DBException(String.format("Error deleting persistable type=\"%s\", _id=\"%d\"!",
                    ((persistable==null) ? "null" : persistable.getClass().getName()),
                    _id));
    }

    protected void addUpdateProxyStatement(Field field, SQLiteStatement update)
    {
        _foreignListCountUpdateStatements.put(field, update);
    }
    protected SQLiteStatement getUpdateProxyStatement(Field field)
    {
        return _foreignListCountUpdateStatements.get(field);
    }

	
	protected SQLiteDatabase getDb() {
		return pm.getDb();
	}
	
	protected void setPM(PersistanceManager pm) {
		this.pm = pm;
	}
	
	protected PersistanceManager getPM() { return this.pm; }
	
	public void bindNull(SQLiteStatement sql, int idx)
	{
		sql.bindNull(idx);
	}
	
	public void bind(SQLiteStatement sql, int idx, String value)
	{
		if (null == value)
			sql.bindNull(idx);
		else
			sql.bindString(idx, value);
	}
	
	public void bind(SQLiteStatement sql, int idx, long value)
	{
		sql.bindLong(idx, value);
	}
	
	public void bind(SQLiteStatement sql, int idx, double value)
	{
		sql.bindDouble(idx, value);
	}
		
	@Override
	public T load(long id) throws DBException {
		Cursor csr = getDb().rawQuery(getSelectSingleSqlString(),
                                      new String[] { Long.toString(id) } );
		return rowToObject(0, csr);
	}

    public static StringBuilder appendOrderByString(StringBuilder sb, OrderByTerm[] orderByTerms)
    {
        sb.append(" ORDER BY ");

        for(int i=0; i < orderByTerms.length; i++) {
            OrderByTerm term = orderByTerms[i];
            sb = appendOrderByTermsString(sb, term);
            if ( (orderByTerms.length > 0) && (i < (orderByTerms.length - 1)) )
                sb.append(", ");
        }

        return sb;
    }

    public static StringBuilder appendWhereClause(StringBuilder sb, String foreignKeyColumn, DbId foreignKey)
    {
        StringBuilder res = null;

        if (null == foreignKey)
        {
            res = sb.append(" WHERE ")
                    .append(foreignKeyColumn)
                    .append(" IS NULL");
        }
        else {
            res = sb.append(" WHERE ")
                    .append(foreignKeyColumn)
                    .append("=").append(foreignKey.getId());
        }

        return res;
    }

    protected static StringBuilder appendOrderByTermsString(StringBuilder sb, OrderByTerm term)
    {
        return sb.append(term.toString());
    }

    public static StringBuilder appendFilterString(StringBuilder sb, String fieldName, CharSequence constraint)
    {
        sb.append(" WHERE ");
        sb.append(fieldName);
        sb.append(" LIKE ");
        sb.append("'%");
        sb.append(constraint.toString());
        sb.append("%'");
        return sb;
    }

    public static StringBuilder createSelectAllStatement(String tableName, List<SqlDataField> fields, OrderByTerm... terms)
    {
        StringBuilder sql = new StringBuilder("SELECT ").append(tableName).append("._id, ");

        int fieldCount = 0;
        Hashtable<Class, SqlDataField> references = new Hashtable<>();
        for(SqlDataField fld : fields)
        {
            if (fieldCount > 0)
                sql.append(", ");

            sql.append(fld.getSqlName());
            if (fld.getSqlType()== SqlDataField.SqlFieldType.OBJECT_REFERENCE)
                references.put(fld.getField().getType(), fld);

            fieldCount++;
        }

        Hashtable<String, OrderByTerm> tables = null;
        if (null != terms) {
            tables = new Hashtable<String, OrderByTerm>();
            for (OrderByTerm term : terms) {
                if (fieldCount > 0)
                    sql.append(", ");

                sql.append(term.getSqlFieldName());

                fieldCount++;
                String table = term.getSqlTableName();
                /**
                 * If there is no tablename set, assume the field is in the current table
                 * -> no tablename needed an no INNER JOIN needed...
                 */
                if (null != table) {
                    if (!tables.containsKey(term.getSqlTableName()))
                        tables.put(term.getSqlTableName(), term);
                }
            }
        }

        sql.append(" FROM ").append(tableName);

        if (null != terms) {
            if (tables.size() > 0) {
                Enumeration keys = tables.keys();
                while (keys.hasMoreElements()) {
                    String otherTable = (String)keys.nextElement();
                    OrderByTerm term = tables.get(otherTable);
                    SqlDataField referenceFld = references.get(term.getType());
                    if (null != referenceFld) {

                        sql.append(" INNER JOIN ");

                        sql.append(otherTable)
                                .append(" ON ")
                                .append(referenceFld.getSqlName())
                                .append("=").append(otherTable).append("._id ");
                    }
                }
            }
        }

        return sql;
    }

	@Override
	public Cursor getLoadAllCursor() {
		return getDb().rawQuery(getSelectAllStatement(), null);
	}

    @Override
    public Cursor getLoadAllCursor(OrderByTerm[] orderTerms) {
        StringBuilder selectAll = appendOrderByString(
                                    new StringBuilder(getSelectAllStatement()),
                                    orderTerms);

        return getDb().rawQuery(selectAll.toString(), null);
    }

    @Override
    public Cursor getFilteredCursor(String fieldName, CharSequence constraint)
    {
        StringBuilder sb = new StringBuilder(getSelectAllStatement());

        return getDb().rawQuery(appendFilterString(sb, fieldName, constraint).toString(), null);
    }

    @Override
    public Cursor getFilteredCursor(String fieldName, CharSequence constraint, OrderByTerm[] orderTerms)
    {
        StringBuilder sb = appendFilterString(new StringBuilder(getSelectAllStatement(orderTerms))
                , fieldName, constraint);

        return getDb().rawQuery(appendOrderByString(sb, orderTerms).toString(), null);
    }

    @Override
	public Collection<T> loadAll() throws DBException {
		Cursor csr = getLoadAllCursor();
		int cnt = csr.getCount();
		Collection<T> lst = new ArrayList<T>(cnt);
		
		if (0 == cnt)
			return lst;
		
		for (int i = 0; i < csr.getCount(); i++)
		{
			lst.add(rowToObject(i, csr));
		}
		
		return lst;
	}
	
    @Override
    public long updateCollectionProxySize(IPersistable persistable, Field field, long newCollSize) throws DBException {
        SQLiteStatement update = getUpdateProxyStatement(field);
        /** Get table field name from parameters (childType + collection or a Field???) */
        update.bindLong(1, newCollSize);
        update.bindLong(2, persistable.getDbId().getId());
        int rowsAffected = update.executeUpdateDelete();

        return rowsAffected;
    }

    protected void setTableName(String tableName) {
        this.tableName = tableName;
    }
    protected String getTableName() {
        return this.tableName;
    }

    /**
     * Creates the SQL-Statement for updating the list-count for a list of childs in the database
     *
     * @return SQL-Statement (update)
     */
    protected SQLiteStatement createUpdateListCountStatement(String tableName, SqlDataField field)
    {
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(tableName)
                .append(" SET ");

        sql.append(field.getSqlName());
        sql.append("=? ");
        sql.append("WHERE _id=?");

        SQLiteStatement updateStatement = getDb().compileStatement(sql.toString());

        return updateStatement;
    }

}
