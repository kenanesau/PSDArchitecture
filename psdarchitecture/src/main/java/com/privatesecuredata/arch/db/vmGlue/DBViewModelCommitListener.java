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
            if (vm instanceof EncapsulatedListViewModel) {
                EncapsulatedListViewModel listVM = (EncapsulatedListViewModel)vm;

                /**
                 *
                 * The Parent-VM of the listVM does not need to be dirty because
                 * it is sufficient if the listVM is dirty -> this will save all changed
                 * of the list and then the state of the parent will be updated too.
                 **/

                listVM.save();
            }
        } else if (vm instanceof ComplexViewModel) {
            Object obj = vm.getModel();
            PersistanceManager pm = MVVM.getCfgObj(((ComplexViewModel)vm).getMVVM());

            if (obj instanceof IPersistable) {
                IPersistable persistable = (IPersistable) obj;
                DbId<?> dbId = persistable.getDbId();

                if (null != dbId)
                    dbId.setDirty();

                pm.save(persistable);
            }
        }
	}
}
