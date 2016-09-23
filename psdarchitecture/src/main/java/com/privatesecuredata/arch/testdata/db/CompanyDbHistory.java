package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.IConversionDescription;
import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IDbHistoryDescription;
import com.privatesecuredata.arch.exceptions.DBException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kenan on 3/22/16.
 */
public class CompanyDbHistory implements IDbHistoryDescription {

    private static final Map<Integer, IConversionDescription> versionToConversionDesc;
    static
    {
        versionToConversionDesc = new LinkedHashMap<>();
        versionToConversionDesc.put(2, new V2CompanyConversionDescription());
    }
    private static final Map<Integer, IDbDescription> versionToDbDesc;
    static
    {
        versionToDbDesc = new LinkedHashMap<>();
        versionToDbDesc.put(2, new AutomaticPersisterDBDescription());
        versionToDbDesc.put(1, new com.privatesecuredata.arch.testdata.v1.db.AutomaticPersisterDBDescription());
    }

    @Override
    public Map<Integer, IDbDescription> getDbDescriptionHistory() {
        return versionToDbDesc;
    }

    @Override
    public Map<Integer, IConversionDescription> getDbConversions() {
        return versionToConversionDesc;
    }

    @Override
    public IDbDescription getDbDescription(int version, int instance) {
        IDbDescription dbDesc = null;

        if (versionToDbDesc.containsKey(version)) {
            dbDesc = versionToDbDesc.get(version).createInstance(instance);
        } else {
            throw new DBException(String.format("Unable to find a DbDescription V%d I%d",
                    version, instance));
        }

        return dbDesc;
    }
}
