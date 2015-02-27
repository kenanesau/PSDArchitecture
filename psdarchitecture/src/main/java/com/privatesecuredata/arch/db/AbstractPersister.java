package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import com.privatesecuredata.arch.exceptions.DBException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public abstract class AbstractPersister<T extends IPersistable<T>> implements IPersister<T> {
	protected static final String DELSQLSTATEMENT = "DELETE from %s where _id=?";
	protected static final String SELECTSINGLESQLSTATEMENT = "SELECT * from %s where _id=?";
	protected static final String SELECTALLSQLSTATEMENT = "SELECT * from %s";
	
	private PersistanceManager pm;
	private SQLiteStatement delete;
    private String tableName;

    /**
     * Update-Statements for the item counts for the foreign key relations
     */
    private Hashtable<Field, SQLiteStatement> _foreignListCountUpdateStatements;


	protected String getSelectAllSQLString() { return String.format(SELECTALLSQLSTATEMENT, getTableName()); }
    protected String getDelSqlString() { return String.format(DELSQLSTATEMENT, getTableName()); }
    protected String getSelectSingleSqlString() { return String.format(SELECTSINGLESQLSTATEMENT, getTableName()); }

	@Override
	public void init(Object obj) {
        setPM((PersistanceManager) obj);
		String delStatement = String.format(getDelSqlString(), getTableName());
		this.delete = getDb().compileStatement(delStatement);
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

	@Override
	public Cursor getLoadAllCursor() {
		return getDb().rawQuery(getSelectAllSQLString(), null);
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

    @Override
    public long updateCollectionProxySize(IPersistable persistable, Field field, Collection coll) throws DBException {
        SQLiteStatement update = getUpdateProxyStatement(field);
        /** Get table field name from parameters (childType + collection or a Field???) */
        update.bindLong(1, coll.size());
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

        sql.append(field.getName());
        sql.append("=? ");
        sql.append("WHERE _id=?");

        SQLiteStatement updateStatement = getDb().compileStatement(sql.toString());

        return updateStatement;
    }
}
