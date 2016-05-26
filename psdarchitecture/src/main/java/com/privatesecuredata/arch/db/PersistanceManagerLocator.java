package com.privatesecuredata.arch.db;

import android.content.Context;
import android.util.Log;

import com.privatesecuredata.arch.db.annotations.DbExtends;
import com.privatesecuredata.arch.db.query.QueryBuilder;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import rx.Observer;

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

    public static void initializePM(IDbDescription dbDesc)
    {
        initializePM(dbDesc, null);
    }

	public static void initializePM(IDbDescription dbDesc, Observer<StatusMessage> statusObserver) throws DBException {
		if (!pmMap.containsKey(dbDesc))
		{
            PersistanceManager pm = new PersistanceManager(dbDesc, statusObserver);
			pmMap.put(dbDesc, pm);

			for(Class<?> classObj : dbDesc.getPersisterTypes())
				pm.addPersister(classObj);
			
			for(Class<?> classObj : dbDesc.getPersistentTypes())
				pm.addPersistentType(classObj);

            /** Count the number of references to types (needed for updates) **/
            for(Class<?> classObj : dbDesc.getPersistentTypes()) {
                IPersister persister = pm.getIPersister(classObj);

                Collection<ObjectRelation> rels = persister.getDescription().getOneToOneRelations();
                for (ObjectRelation rel : rels) {
                    IPersister referencedPersister = pm.getIPersister(rel.getField().getType());
                    referencedPersister.getDescription().increaseRefCount();
                }

                rels = persister.getDescription().getOneToManyRelations();
                for (ObjectRelation rel : rels) {
                    IPersister referencedPersister = pm.getIPersister(rel.getReferencedListType());
                    referencedPersister.getDescription().increaseRefCount();
                }
            }

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
                    String msg = String.format("unable to create or register Querybuilder of type \"%s\"",
                            queryBuilderType.getName());
                    if (statusObserver == null)
                        throw new ArgumentException(msg, e);
                    else {
                        Exception ex = new ArgumentException(msg, e);

                        if (statusObserver != null)
                            pm.publishStatus(new StatusMessage(PersistanceManager.Status.ERROR, msg, ex));
                    }
                }
            }

            pm.publishStatus(new StatusMessage(PersistanceManager.Status.INITIALIZEDPM));
		}
	}

    protected void checkAndDoUpgrade(PersistanceManager pm, Context ctx, IDbDescription dbDesc) throws DBException
    {
        pm.publishStatus(new StatusMessage(PersistanceManager.Status.UPGRADINGDB));
        /** Version -> HashMap (Instance -> dbName) **/
        HashMap<Integer, HashMap<Integer, String>> dbNames = new LinkedHashMap<>();
        int highestDbVersion = 0;

        String[] allDbs = ctx.databaseList();
        ArrayList<String> currentDbs = new ArrayList<>();

        /**
         * Get rid of journals and other DBs
         */
        for(String dbName : allDbs) {
            if (dbName.startsWith(dbDesc.getBaseName()))
                if(dbName.endsWith(".db"))
                    currentDbs.add(dbName);
        }

        for(String dbName : currentDbs) {
            String[] tokens = dbName.split("_");
            int foundVersion = 0;
            int instance = 0;
            for(int i = tokens.length-1; i>=0; i--) {
                String tok = tokens[i];
                if (i == tokens.length - 1) {
                    if (tok.startsWith("V")) {
                        tok = tok.replace("V", "");
                        tok = tok.replace(".db", "");
                        foundVersion = Integer.decode(tok);
                        if (foundVersion > highestDbVersion)
                            highestDbVersion = foundVersion;
                    }
                    else {
                        Log.w(this.getClass().getName(),
                                String.format("Error parsing DB-filename '%s'. No version found!", dbName));
                        continue;
                    }
                }

                if (i == tokens.length - 2) {
                    if (tok.startsWith("I")) {
                        tok = tok.replace("I", "");
                        try {
                            instance = Integer.decode(tok);
                        }
                        catch (Exception ex)
                        {
                            Log.w(this.getClass().getName(),
                                    String.format("Error parsing instance-number of DB-filename '%s'", dbName));
                            continue;
                        }

                        if (!dbNames.containsKey(foundVersion))
                            dbNames.put(foundVersion, new LinkedHashMap<Integer, String>());

                        dbNames.get(foundVersion).put(instance, dbName);
                    }
                    else {
                        Log.w(this.getClass().getName(),
                                String.format("Error parsing DB-filename '%s'. No instance found!", dbName));
                        continue;
                    }
                }
            }
        }

        if (highestDbVersion < dbDesc.getVersion()) {
            Log.i(getClass().getName(), String.format("Upgrading DB '%s' V%d I%d to V%d",
                    dbDesc.getBaseName(), dbDesc.getVersion(), dbDesc.getInstance(), highestDbVersion));
            pm.onUpgrade(ctx, highestDbVersion, dbDesc.getVersion(), dbNames);
        }
        else if (highestDbVersion > dbDesc.getVersion())
        {
            Log.w(getClass().getName(), String.format("Opening DB '%s' 'V%d' but newer version 'V%d' is available",
                    dbDesc.getBaseName(), dbDesc.getVersion(), highestDbVersion));
            /*throw new DBException(
                    String.format("Current DB version 'V%d' is newer than that of the App 'V%d'",
                            highestDbVersion, dbDesc.getVersion()));*/
        }
    }

    public PersistanceManager getPersistanceManager(Context ctx,
                                                    IDbDescription dbDesc,
                                                    Observer<StatusMessage> statusObserver) throws DBException {

        return init(ctx, dbDesc, statusObserver);
    }


    public PersistanceManager getPersistanceManager(Context ctx, IDbDescription dbDesc) throws DBException {
        return init(ctx, dbDesc);
    }

    private PersistanceManager init(Context ctx, IDbDescription dbDesc) {
        return init(ctx, dbDesc, null);
    }

    private PersistanceManager init(Context ctx, IDbDescription dbDesc,
                                    Observer<StatusMessage> statusObserver) throws DBException {
        PersistanceManager pm = null;
        try {
            if (!pmMap.containsKey(dbDesc))
                initializePM(dbDesc, statusObserver);

            pm = pmMap.get(dbDesc);
            if (!pm.isInitialized()) {
                checkAndDoUpgrade(pm, ctx, dbDesc);
                pm.publishStatus(new StatusMessage(PersistanceManager.Status.OPERATIONAL));
            }
            if (!pm.isInitialized()) {
                pm.initializeDB(ctx);
                pm.publishStatus(new StatusMessage(PersistanceManager.Status.OPERATIONAL));
            }
        }
        catch (Exception e) {
            if (null != pm)
                pm.publishStatus("Error initializing PM", e);
            else
                throw e;
        }

        return pm;
    }

    public void clear() {
        for(PersistanceManager pm : pmMap.values())
            pm.close();

        pmMap.clear();
    }
}
