package com.privatesecuredata.arch.db;

import android.content.Context;
import android.util.Log;

import com.privatesecuredata.arch.exceptions.DBException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import rx.subjects.ReplaySubject;

public class PersistanceManagerLocator {
	private static PersistanceManagerLocator instance = null;
	private static HashMap<String, PersistanceManager> pmMap = new HashMap<String, PersistanceManager>();

	private PersistanceManagerLocator() {}

	public static PersistanceManagerLocator getInstance()
	{
		if (null == instance) 
			instance = new PersistanceManagerLocator();
		
		return instance;
	}

    public static void initializePM(Context ctx, IDbDescription dbDesc)
    {
        initializePM(ctx, dbDesc, null);
    }

	public static void initializePM(Context ctx, IDbDescription dbDesc, ReplaySubject<StatusMessage> statusRelay) throws DBException {
        if (!pmMap.containsKey(dbDesc))
		{
            PersistanceManager pm = new PersistanceManager(dbDesc, statusRelay);
            File dbFile = ctx.getDatabasePath(dbDesc.getName());
			pmMap.put(dbFile.getAbsolutePath(), pm);
            pm.publishStatus(new StatusMessage(PersistanceManager.Status.INITIALIZEDPM));
		}
	}

    protected boolean checkAndDoUpgrade(PersistanceManager pm, Context ctx, IDbDescription dbDesc) throws DBException
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

            if (dbName.startsWith("upgrading_"))
            {
                ctx.deleteDatabase(dbName);
                Log.w(this.getClass().getName(), String.format("deleting files for db '%s'", dbName));
            }
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

        boolean ret = false;
        if ( (currentDbs.size() > 0) && (highestDbVersion < dbDesc.getVersion()) ) {
            Log.i(getClass().getName(), String.format("Upgrading DB '%s' V%d I%d to V%d",
                    dbDesc.getBaseName(), dbDesc.getVersion(), dbDesc.getInstance(), highestDbVersion));
            pm.onUpgrade(ctx, highestDbVersion, dbDesc.getVersion(), dbNames);
            ret = true;
        }
        else if (highestDbVersion > dbDesc.getVersion())
        {
            Log.w(getClass().getName(), String.format("Opening DB '%s' 'V%d' but newer version 'V%d' is available",
                    dbDesc.getBaseName(), dbDesc.getVersion(), highestDbVersion));
            /*throw new DBException(
                    String.format("Current DB version 'V%d' is newer than that of the App 'V%d'",
                            highestDbVersion, dbDesc.getVersion()));*/
        }

        return ret;
    }

    public PersistanceManager getPersistanceManager(Context ctx,
                                                    IDbDescription dbDesc,
                                                    ReplaySubject<StatusMessage> statusObserver) throws DBException {

        return init(ctx, dbDesc, statusObserver);
    }


    public PersistanceManager getPersistanceManager(Context ctx, IDbDescription dbDesc) throws DBException {
        return init(ctx, dbDesc);
    }

    private PersistanceManager init(Context ctx, IDbDescription dbDesc) {
        return init(ctx, dbDesc, null);
    }

    private PersistanceManager init(Context ctx, IDbDescription dbDesc,
                                    ReplaySubject<StatusMessage> statusObserver) throws DBException {
        /* We do not save the reference to the context -> so we do NOT NEED the application-context
        -> use the ctx directly since otherwise eg RenamingDelegatingContext would not work ... */

        PersistanceManager pm = null;
        try {
            File dbFile = ctx.getDatabasePath(dbDesc.getName());
            if (!pmMap.containsKey(dbFile.getAbsolutePath()))
                initializePM(ctx, dbDesc, statusObserver);

            pm = pmMap.get(dbFile.getAbsolutePath());
            if (!pm.hasInitializedDb()) {
                if (checkAndDoUpgrade(pm, ctx, dbDesc)) {
                    pm.publishStatus(new StatusMessage(PersistanceManager.Status.OPERATIONAL));
                }
                else {
                    /* This also creates the tables if they do not exist yet */
                    pm.initializeDb(ctx);
                    pm.publishStatus(new StatusMessage(PersistanceManager.Status.OPERATIONAL));
                }
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
        Log.d(getClass().getName(), "Clearing PersistanceManagerLocator!");
        for(PersistanceManager pm : pmMap.values())
            pm.close();

        pmMap.clear();
    }
}
