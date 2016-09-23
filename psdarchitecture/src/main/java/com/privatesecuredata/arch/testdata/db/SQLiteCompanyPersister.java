package com.privatesecuredata.arch.testdata.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.privatesecuredata.arch.db.AbstractPersister;
import com.privatesecuredata.arch.db.AutomaticPersister;
import com.privatesecuredata.arch.db.CollectionProxyFactory;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Persister(persists=Company.class)
public class SQLiteCompanyPersister extends AbstractPersister<Company> {
    private SQLiteStatement insert;
    private SQLiteStatement delete;
    private SQLiteStatement update;

    private SQLiteStatement updateBossForeignKey;

	
	protected static final String ADDCOMPANY = "INSERT into company (name, boss_id, employees_size) Values (?, ?, ?)";
	protected static final String DELCOMPANY = "DELETE from company where _id=?";
	protected static final String UPDATECOMPANY = "UPDATE company SET name=?, boss_id=?, employees_size=? where _id=?";
	protected static final String SELECTCOMPANY = "SELECT * from company where _id=?";
	protected static final String SELECTCOMPANIES = "SELECT * from company";
	
	protected static final String UPDATEBOSSFOREIGNKEY = "UPDATE company SET boss_id=? where _id=?";
    private PersisterDescription desc = new PersisterDescription(Company.class);
	
	public SQLiteCompanyPersister() {}

	public void init(Object obj)
	{
		setPM((PersistanceManager) obj);
		insert = getPM().getDb().compileStatement(ADDCOMPANY);
		update = getPM().getDb().compileStatement(UPDATECOMPANY);
		delete = getPM().getDb().compileStatement(DELCOMPANY);
		updateBossForeignKey = getPM().getDb().compileStatement(UPDATEBOSSFOREIGNKEY);
	}
	
	@Override
	public long insert(Company comp) throws DBException {
		insert.clearBindings();
		insert.bindString(1, comp.getName());
		getPM().save(comp.getCeo());
		insert.bindLong(2, comp.getCeo().getDbId().getId());
        insert.bindLong(3, comp.getEmployees().size());
		long companyId = (insert.executeInsert());
		DbId<?> compDbId = getPM().assignDbId(comp, companyId);
		for (Person employee : comp.getEmployees())
		{
			getPM().saveAndUpdateForeignKey(employee, compDbId);
		}
		getPM().updateForeignKey(comp, comp.getCeo().getDbId());
		return companyId;
 	}

	@Override
	public long update(Company comp) throws DBException {
		update.clearBindings();
		update.bindString(1, comp.getName());
		update.bindLong(2, comp.getCeo().getDbId().getId());
        update.bindLong(3, comp.getEmployees().size());
        update.bindLong(4, comp.getDbId().getId());
		for (Person employee : comp.getEmployees())
		{
			getPM().saveAndUpdateForeignKey(employee, comp.getDbId());
		}

		return update.executeUpdateDelete();
	}

	@Override
	public Company load(long id) throws DBException {
		Cursor csr = getPM().getDb().rawQuery(SELECTCOMPANY, new String[] { new Long(id).toString() });
		return rowToObject(0, csr);
	}

	@Override
	public Company rowToObject(int pos, Cursor csr) throws DBException
	{
		Company comp = new Company();
		csr.moveToPosition(pos);

        getPM().assignDbId(comp, csr.getLong(0));
		for (int colIndex=1; colIndex<=csr.getColumnCount(); colIndex++)
		{
			switch (colIndex) {
			case 1:
				comp.setName(csr.getString(colIndex));
				break;
			case 2:
				Person boss = getPM().load(comp.getDbId(), Person.class, csr.getLong(colIndex));
				comp.setCeo(boss);
				break;
            case 3:
                List<Person> employees = CollectionProxyFactory.getCollectionProxy(getPM(), Person.class, comp, (int)csr.getLong(colIndex), getEmployeesCursorLoader());
                comp.setEmployees(employees);
			}
		}
		
		return comp;
	}

    @Override
    public List<SqlDataField> getSqlFields() {
        return null;
    }

    @Override
	public Cursor getLoadAllCursor()
	{
		Cursor csr = getPM().getDb().rawQuery(SELECTCOMPANIES, null);
		return csr;
	}

    @Override
    public Cursor getLoadAllCursor(OrderByTerm[] orderTerms) throws DBException {
        /** Do not support ordering at the moment ... */
        return getLoadAllCursor();
    }

    @Override
	public Cursor getFilteredCursor(String fieldName, CharSequence constraint)
	{
		StringBuilder sb = new StringBuilder(SELECTCOMPANIES);
		sb.append(" WHERE ");
		sb.append(fieldName);
		sb.append(" LIKE ");
		sb.append("'%");
		sb.append(constraint.toString());
		sb.append("%'");

		return getPM().getDb().rawQuery(sb.toString(), null);
	}

    @Override
    public Cursor getFilteredCursor(String fieldName, CharSequence constraint, OrderByTerm[] orderTerms) throws DBException {
        /** Do not support ordering at the moment ... */
        return getFilteredCursor(fieldName, constraint);
    }

    @Override
	public Collection<Company> loadAll() throws DBException {
		Cursor csr = getLoadAllCursor();
		Collection<Company> lst = new ArrayList<Company>(csr.getCount());
		
		for (int i = 0; i < csr.getCount(); i++)
		{
			lst.add(rowToObject(i, csr));
		} while (!csr.isLast());
		
		return lst;
	}

	@Override
	public void delete(Company loc) throws DBException {
		delete.clearBindings();
		DbId<Company> id = loc.getDbId();
		if (null != id)
		{
			delete.bindLong(1, id.getId());
			delete.executeUpdateDelete();
		}
	}

	@Override
	public void updateForeignKey(DbId<Company> compId, DbId<?> foreignId) throws DBException
	{
		IPersistable obj = foreignId.getObj();
		if (obj instanceof Person) {
			updateBossForeignKey.clearBindings();
			updateBossForeignKey.bindLong(1, foreignId.getId());
			updateBossForeignKey.bindLong(2, compId.getId());
			updateBossForeignKey.executeUpdateDelete();
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
    public Company createPersistable() {
        return new Company();
    }

    @Override
    public Query getQuery(String queryId) {
        return null;
    }

    @Override
    public PersisterDescription<Company> getDescription() {
        return desc;
    }

    @Override
    public long updateCollectionProxySize(DbId dbId, Field field, long collSize) throws DBException {
        return 0;
    }

    public ICursorLoader getEmployeesCursorLoader()
    {
        return null;
        //return new IdCursorLoader(getPM(), "employee", "company_id");
    }
}
