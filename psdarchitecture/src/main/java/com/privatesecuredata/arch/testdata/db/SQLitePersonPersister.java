package com.privatesecuredata.arch.testdata.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.privatesecuredata.arch.db.AbstractPersister;
import com.privatesecuredata.arch.db.AutomaticPersister;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.ICursorLoader;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.OrderByTerm;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.db.annotations.Persister;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;
import com.privatesecuredata.arch.testdata.Company;
import com.privatesecuredata.arch.testdata.Person;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Persister(persists=Person.class)
public class SQLitePersonPersister extends AbstractPersister<Person> {
	SQLiteStatement insert;
	SQLiteStatement delete;
	SQLiteStatement update;
	
	SQLiteStatement updateCompanyForeignKey;
	
	protected static final String ADDPERSON = "INSERT into employee (fld_firstname, fld_lastname, fld_age) Values (?, ?, ?)";
	protected static final String DELPERSON = "DELETE from employee where _id=?";
	protected static final String UPDATEPERSON = "UPDATE employee SET fld_firstname=?, fld_lastname=?, fld_age=? where _id=?";
	protected static final String SELECTPERSON = "SELECT * from employee where _id=?";
	protected static final String SELECTPERSONS = "SELECT * from employee";
	
	protected static final String UPDATECOMPANYFOREIGNKEY = "UPDATE employee SET company_id=? where _id=?";

    private PersisterDescription desc = new PersisterDescription(Person.class);
	
	public SQLitePersonPersister() {}

	@Override
	public void init(Object obj)
	{
		super.init(obj);
		insert = getPM().getDb().compileStatement(ADDPERSON);
		update = getPM().getDb().compileStatement(UPDATEPERSON);
		delete = getPM().getDb().compileStatement(DELPERSON);
		updateCompanyForeignKey = getPM().getDb().compileStatement(UPDATECOMPANYFOREIGNKEY);
		
		//pm.registerCursorLoader(Company.class, Person.class, getEmployeeCursorLoader());
	}
	
	@Override
	public long insert(Person pers) throws DBException {
		insert.clearBindings();
		insert.bindString(1, pers.getFirstName());
		insert.bindString(2, pers.getLastName());
        java.text.DateFormat df = new SimpleDateFormat(AutomaticPersister.DATE_FORMAT);
        String valStr = null;
        if (null != pers.getBirthDate())
            valStr = df.format((Date) pers.getBirthDate());
        if (null != valStr)
		    insert.bindString(3, valStr);
        else
            insert.bindNull(3);
		return(insert.executeInsert());
 	}

	@Override
	public long update(Person pers) throws DBException {
		update.clearBindings();
		update.bindString(1, pers.getFirstName());
		update.bindString(2, pers.getLastName());
        String valStr = null;
        java.text.DateFormat df = new SimpleDateFormat(AutomaticPersister.DATE_FORMAT);
        if (null != pers.getBirthDate())
            valStr = df.format((Date) pers.getBirthDate());
        if (null != valStr)
            insert.bindString(3, valStr);
        else
            insert.bindNull(3);
		update.bindLong(4, pers.getDbId().getId());
		return update.executeUpdateDelete();
	}

	@Override
	public Person load(long id) throws DBException {
		Cursor csr = getPM().getDb().rawQuery(SELECTPERSON, new String[] { new Long(id).toString() });
		return rowToObject(0, csr);
	}

	@Override
	public Person rowToObject(int pos, Cursor csr)
	{
		Person pers = new Person();
		csr.moveToPosition(pos);
		
		for (int colIndex=1; colIndex<=csr.getColumnCount(); colIndex++)
		{
			switch (colIndex) {
			case 1:
				pers.setFirstName(csr.getString(colIndex));
				break;
			case 2:
				pers.setLastName(csr.getString(colIndex));
				break;
			case 3:
                java.text.DateFormat df = new SimpleDateFormat(AutomaticPersister.DATE_FORMAT);
                String dateStr = csr.getString(colIndex);
                Date val = null;
                if (null != dateStr) {
                    try {
                        val = df.parse(dateStr);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
				pers.setBirthDate(val);
				break;
			}
		}
		
		return pers;
	}

    @Override
    public List<SqlDataField> getSqlFields() {
        return null;
    }

    public Cursor getLoadAllCursor()
    {
        Cursor csr = getPM().getDb().rawQuery(SELECTPERSONS, null);
        return csr;
    }

    @Override
    public Cursor getLoadAllCursor(OrderByTerm[] orderTerms) throws DBException {
        StringBuilder sb = AbstractPersister.appendOrderByString(new StringBuilder(SELECTPERSONS), orderTerms);
        return getPM().getDb().rawQuery(sb.toString(), null);
    }

    @Override
	public Cursor getFilteredCursor(String fieldName, CharSequence constraint)
	{
		StringBuilder sb = AbstractPersister.appendFilterString(new StringBuilder(SELECTPERSONS), fieldName, constraint);
		return getPM().getDb().rawQuery(sb.toString(), null);
	}

    @Override
    public Cursor getFilteredCursor(String fieldName, CharSequence constraint, OrderByTerm[] orderTerms) throws DBException {
        StringBuilder sb = AbstractPersister.appendOrderByString(
                AbstractPersister.appendFilterString(new StringBuilder(SELECTPERSONS), fieldName, constraint),
                orderTerms);
        return getPM().getDb().rawQuery(sb.toString(), null);
    }

    @Override
	public Collection<Person> loadAll() throws DBException {
		Cursor csr = getLoadAllCursor();
		Collection<Person> lst = new ArrayList<Person>(csr.getCount());
		
		for (int i = 0; i < csr.getCount(); i++)
		{
			lst.add(rowToObject(i, csr));
		} while (!csr.isLast());
		
		return lst;
	}

	@Override
	public void delete(Person loc) throws DBException {
		delete.clearBindings();
		DbId<Person> id = loc.getDbId();
		if (null != id)
		{
			delete.bindLong(1, id.getId());
			delete.executeUpdateDelete();
		}
	}

	@Override
	public void updateForeignKey(DbId<Person> persId, DbId<?> foreignId) throws DBException
	{
		IPersistable obj = foreignId.getObj();
		if (obj instanceof Company) {
			updateCompanyForeignKey.clearBindings();
			updateCompanyForeignKey.bindLong(1, foreignId.getId());
			updateCompanyForeignKey.bindLong(2, persId.getId());
			updateCompanyForeignKey.executeUpdateDelete();
		} 
		else 
		{
			throw new ArgumentException("Unknown type of foreign key");
		}
	}

    @Override
    public boolean tableExists() {
        return true;
    }

    @Override
    public void addExtendingPersister(AutomaticPersister childPersister) {

    }

    @Override
    public List<AutomaticPersister> getExtendingPersisters() {
        return null;
    }

    @Override
    public Person createPersistable() {
        return new Person();
    }

    @Override
    public Query getQuery(String queryId) {
        return null;
    }

    @Override
    public PersisterDescription<Person> getDescription() {
        return desc;
    }

	@Override
	public void registerAction(PersistanceManager.Action<Person> action) {

	}

	@Override
	public void unregisterAction(PersistanceManager.Action<Person> action) {

	}

	@Override
	public boolean hasDeleteActions() {
		return false;
	}

	@Override
    public long updateCollectionProxySize(DbId dbId, Field field, long collSize) throws DBException {
        return 0;
    }

    public ICursorLoader getEmployeeCursorLoader()
	{
        return null;
		//return new IdCursorLoader(pm, "employee", "company_id");
	}
}
