package com.privatesecuredata.arch.db;

import java.util.ArrayList;
import java.util.Collection;

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

	public abstract String getTableName();
	protected String getSelectAllSQLString() { return String.format(SELECTALLSQLSTATEMENT, getTableName()); }
	
	@Override
	public void init(Object obj) {
		setPM((PersistanceManager)obj);
		String delStatement = String.format(DELSQLSTATEMENT, getTableName());
		this.delete = getDb().compileStatement(delStatement);
	}
	@Override
	public abstract long insert(T persistable) throws DBException;
	@Override
	public abstract long update(T persistable) throws DBException;
	@Override
	public abstract void updateForeignKey(T persistable, DbId<?> foreignId) throws DBException;
	@Override
	public abstract T rowToObject(int pos, Cursor csr) throws DBException;
	
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
	public T load(DbId<T> id) throws DBException {
		Cursor csr = getDb().rawQuery(String.format(SELECTSINGLESQLSTATEMENT, getTableName()), 
								new String[] { Long.toString(id.getId()) } );
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
}
