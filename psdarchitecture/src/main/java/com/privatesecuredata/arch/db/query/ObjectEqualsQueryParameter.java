package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersisterDescription;

/**
 * Created by kenan on 12/1/17.
 */

public class ObjectEqualsQueryParameter extends QueryParameter {
    PersisterDescription desc;


    public ObjectEqualsQueryParameter(String paraId, String paraField) {
        super(paraId, paraField);
    }

    @Override
    public void setValue(Object obj) {
        super.setValue(obj);
    }

    public void setDescription(PersisterDescription desc) {
        this.desc = desc;
    }
}
