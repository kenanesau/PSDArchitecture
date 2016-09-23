package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.AbstractDbDescription;
import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IDbHistoryDescription;

public class DBDescription extends AbstractDbDescription {
	private String DB_NAME = "companies.db";
    private String _dbName = DB_NAME;
	private int DB_VERSION = 1;

	private static final String COMPANYDB_CREATE = 
		"CREATE TABLE companyDb (version INTEGER)";
	private static final String EMPLOYEE_CREATE =
		"CREATE TABLE employee (" +
		"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
		"fld_firstname TEXT NOT NULL, fld_lastname TEXT NOT NULL," +
		"fld_age INTEGER, " +
		"company_id INTEGER, " +
		"FOREIGN KEY(company_id) REFERENCES company(_id) DEFERRABLE INITIALLY DEFERRED)";
	
	private static final String COMPANY_CREATE =
		"CREATE TABLE company (" +
		"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
		"name TEXT NOT NULL, " +
		"boss_id INTEGER, " +
        "employees_size INTEGER, " +
		"FOREIGN KEY(boss_id) REFERENCES employee(_id) DEFERRABLE INITIALLY DEFERRED )";
	
	private static final Class<?> persisterClasses[] = { SQLiteCompanyPersister.class,
																SQLitePersonPersister.class };
	private int instance = -1;
	
	public DBDescription() { this(0); }
	
	public DBDescription(int instance)
	{
        super(new Provider<IDbDescription>() {
            @Override
            public IDbDescription create(Integer instance) {
                return new DBDescription(instance);
            }
        });
		this.instance  = instance;
	}
	
    public DBDescription(String namePrefix, int instance)
    {
        this(instance);
        _dbName = String.format("%s_%s", namePrefix, _dbName);
    }

    public String getBaseName() {
        return _dbName;
	}
	@Override
	public Integer getVersion() {
		return DB_VERSION;
	}
	@Override
	public String[] getCreateStatements() {
		return new String[] { COMPANYDB_CREATE, EMPLOYEE_CREATE, COMPANY_CREATE };
	}
	@Override
	public Class<?>[] getPersisterTypes() { return persisterClasses; }

	@Override
	public IDbHistoryDescription getDbHistory() {
		return new CompanyDbHistory();
	}

	@Override
	public Integer getInstance() {
		return this.instance;
	}	
}
	
