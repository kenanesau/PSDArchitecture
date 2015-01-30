package com.privatesecuredata.arch.db.vmGlue;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewModelCommitListener;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;

public class DBViewModelCommitListener implements IViewModelCommitListener {

    public DBViewModelCommitListener()
    {}

	@Override
	public void notifyCommit(IViewModel<?> vm) {

        if (vm instanceof IListViewModel) {
            PersistanceManager pm = MVVM.getCfgObj(((ComplexViewModel)vm).getMVVM());

            if (vm instanceof EncapsulatedListViewModel) {
                EncapsulatedListViewModel listVM = (EncapsulatedListViewModel)vm;
                ComplexViewModel parent = listVM.getParentViewModel();
                Object obj = parent.getModel();

                DbId<?> parentDbId = null;
                IPersistable<?> parentPersistable = null;

                if (obj instanceof IPersistable<?>) {
                    parentPersistable = (IPersistable<?>) obj;
                    parentDbId = parentPersistable.getDbId();

                    /**
                     * if we have a dirty dbId or no DbId at all -> disableGlobalNotify()
                     * so the parent is saved when the child-list is saved.
                     */
                    if (null != parentDbId) {
                        if (parent.isGlobalNotifyEnabled())
                        {
                            parentDbId.setDirty();
                            parent.disableGlobalNotify();
                        }
                    }
                    else
                        parent.disableGlobalNotify();
                    /**
                     * enableGlobalNotify() is called in the parent ComplexViewModel when the
                     * commit() of the parent ComplexViewModel is complete
                     */

                }

                listVM.save();
                /**
                 * Check if parent was saved with the list...
                 * -> if not save it
                 */
                parentDbId = parentPersistable.getDbId();

                if ( (null == parentDbId) || (parentDbId.getDirty()) )
                    pm.save(parentPersistable);
            }
        } else if (vm instanceof ComplexViewModel) {
            Object obj = vm.getModel();
            PersistanceManager pm = MVVM.getCfgObj(((ComplexViewModel)vm).getMVVM());

            if (obj instanceof IPersistable<?>) {
                IPersistable<?> persistable = (IPersistable<?>) obj;
                DbId<?> dbId = persistable.getDbId();

                if (null != dbId)
                    dbId.setDirty();

                pm.save(persistable);
            }
        }
	}
}
