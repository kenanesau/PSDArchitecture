package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.AbstractDbDescription;
import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IDbHistoryDescription;
import com.privatesecuredata.arch.testdata.Company;
import com.privatesecuredata.arch.testdata.Person;

public class AutomaticPersisterDBDescription extends AbstractDbDescription {
	private static final String DB_NAME = "automatic_companies";
	private String _dbName = DB_NAME;
	private static final int DB_VERSION = 2;

	private static final Class<?> persistentTypes[] = { Company.class, Person.class };
	private static final Class<?> persisterClasses[] = { };
	private int instance = -1;
	
	public AutomaticPersisterDBDescription() { this(0); }
	
	public AutomaticPersisterDBDescription(int instance)
	{
        super(new Provider<IDbDescription>() {
            @Override
            public IDbDescription create(Integer instance) {
                return new AutomaticPersisterDBDescription(instance);
            }
        });
        this.instance  = instance;
	}

	public AutomaticPersisterDBDescription(String namePrefix, int instance)
	{
		this(instance);
		_dbName = String.format("%s_%s", namePrefix, _dbName);
	}
	
	@Override
	public String getBaseName() {
		return _dbName;
	}
	@Override
	public Integer getVersion() {
		return DB_VERSION;
	}
	@Override
	public String[] getCreateStatements() {
		return new String[] { };
	}
	@Override
	public Class<?>[] getPersisterTypes() { return persisterClasses; }

	@Override
	public Class<?>[] getPersistentTypes() {
		return persistentTypes;
	}

	@Override
	public IDbHistoryDescription getDbHistory() {
		return new CompanyDbHistory();
	}

	@Override
	public Integer getInstance() {
		return this.instance;
	}	
}
	
