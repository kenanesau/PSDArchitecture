package com.privatesecuredata.arch.db;

import android.content.Context;

import com.privatesecuredata.arch.db.annotations.DbExtends;
import com.privatesecuredata.arch.db.query.QueryBuilder;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class PersistanceManagerLocator {
	private static PersistanceManagerLocator instance = null;
	private static HashMap<IDbDescription, PersistanceManager> pmMap = new HashMap<IDbDescription, PersistanceManager>();
	
	private PersistanceManagerLocator() {}
	
	public static PersistanceManagerLocator getInstance()
	{
		if (null == instance) 
			instance = new PersistanceManagerLocator();
		
		return instance;
	}
	
	public static void initializeDB(IDbDescription dbDesc) throws DBException {
		if (!pmMap.containsKey(dbDesc))
		{
			PersistanceManager pm = new PersistanceManager(dbDesc); 
			pmMap.put(dbDesc, pm);

			for(Class<?> classObj : dbDesc.getPersisterTypes())
				pm.addPersister(classObj);
			
			for(Class<?> classObj : dbDesc.getPersistentTypes())
				pm.addPersistentType(classObj);

            /**
             * Work on the extends-relationships
             */
            for(Class<?> persistentType : dbDesc.getPersistentTypes()) {
                DbExtends anno = persistentType.getAnnotation(DbExtends.class);

                if (null != anno) {
                    AutomaticPersister persister = (AutomaticPersister)pm.getIPersister(persistentType);
                    if (null == persister)
                        throw new DBException(String.format("Could not find persister for type \"%s\"", persistentType.getName()));
                    AutomaticPersister parentPersister = (AutomaticPersister)pm.getIPersister(anno.extendedType());
                    if (null == parentPersister)
                        throw new DBException(String.format("Could not find persister for parent of extends-relationship of type \"%s\"!", anno.extendedType().getName()));
                    persister.extendsPersister(parentPersister);
                }
            }

            /**
             * Register the query-Builders
             */
            for(Class<?> queryBuilderType : dbDesc.getQueryBuilderTypes()) {
                try {
                    Constructor constructor = queryBuilderType.getConstructor();
                    QueryBuilder queryBuilder = (QueryBuilder)constructor.newInstance();

                    pm.registerQuery(queryBuilder);

                } catch (Exception e) {
                    throw new ArgumentException(
                            String.format("unable to create or register Querybuilder of type \"%s\"",
                                    queryBuilderType.getName()));
                }
            }
		}
	}

	public PersistanceManager getPersistanceManager(Context ctx, IDbDescription dbDesc) throws DBException {
		PersistanceManager pm = pmMap.get(dbDesc);
		if (!pm.isInitialized())
			pm.initialize(ctx, dbDesc);

		return pm;
	}

	public PersistanceManager getPM(IDbDescription dbDesc)
	{
		return pmMap.get(dbDesc);
	}
}
