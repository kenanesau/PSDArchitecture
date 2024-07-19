package com.privatesecuredata.arch.db.vmGlue;

import android.util.Log;

import com.privatesecuredata.arch.db.CollectionProxyFactory;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersister;
import com.privatesecuredata.arch.db.ObjectRelation;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewModelCommitListener;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;

import java.lang.reflect.Field;
import java.util.Collection;

public class DBViewModelCommitListener implements IViewModelCommitListener {

    public DBViewModelCommitListener()
    {}

    @Override
    public void notifyStartCommit(IViewModel<?> vm) {
        if (vm instanceof IListViewModel) {
            /** DO nothing -> is done in list-CB **/
        } else if (vm instanceof ComplexViewModel) {
            Object obj = vm.getModel();
            PersistanceManager pm = MVVM.getCfgObj(((ComplexViewModel)vm).getMVVM());

            if (obj == null)
                return;

            if (obj instanceof IPersistable) {
                IPersistable persistable = (IPersistable) obj;
                IPersister persister = pm.getPersister(persistable);
                PersisterDescription desc = persister.getDescription();


                /** Prohibit saving of composed objects **/
                Collection<ObjectRelation> oneToOneRels = desc.getOneToOneRelations();
                for(ObjectRelation rel : oneToOneRels) {
                    if (!rel.isComposition())
                        continue;

                    try {
                        Field fld = rel.getField();
                        Object referencedObj = fld.get(obj);

                        if (referencedObj instanceof  IPersistable) {
                            IPersistable childPersistable = (IPersistable)referencedObj;
                            if (childPersistable.getDbId() != null)
                                continue;

                            DbId id = pm.assignDbId(childPersistable, 0, true);
                            id.setComposition();
                        }
                    }
                    catch (Exception e) {
                        Log.e(getClass().getName(), "Error!!");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
	public void notifyCommit(IViewModel<?> vm) {

        if (vm instanceof IListViewModel) {
            /** DO nothing -> is done in list-CB **/
        } else if (vm instanceof ComplexViewModel) {
            Object obj = vm.getModel();
            PersistanceManager pm = MVVM.getCfgObj(((ComplexViewModel)vm).getMVVM());

            if (obj instanceof IPersistable) {
                IPersistable persistable = (IPersistable) obj;
                DbId<?> dbId = persistable.getDbId();

                pm.forceSave(persistable);
            }
        }
	}
}
