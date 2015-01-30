package com.privatesecuredata.arch.db;

import java.util.HashMap;
import android.content.Context;
import com.privatesecuredata.arch.exceptions.DBException;

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
