package com.privatesecuredata.arch.testdata.v1.db;

import com.privatesecuredata.arch.db.AbstractDbDescription;
import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IDbHistoryDescription;
import com.privatesecuredata.arch.testdata.v1.Company;
import com.privatesecuredata.arch.testdata.v1.Person;

public class AutomaticPersisterDBDescription extends AbstractDbDescription {
	private static final String DB_NAME = "automatic_companies.db";
	private static final int DB_VERSION = 1;

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
	
	@Override
	public String getBaseName() {
		return DB_NAME;
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
		return null;
	}

	@Override
	public Integer getInstance() {
		return this.instance;
	}	
}
	
