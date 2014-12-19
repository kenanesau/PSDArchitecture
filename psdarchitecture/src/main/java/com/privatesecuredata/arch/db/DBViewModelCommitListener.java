package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewModelCommitListener;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.ListViewModel;

public class DBViewModelCommitListener implements IViewModelCommitListener {

    private PersistanceManager pm;

    public DBViewModelCommitListener(PersistanceManager pm)
    {
        this.pm = pm;
    }

    protected PersistanceManager getPM() { return this.pm; }

	@Override
	public void notifyCommit(IViewModel<?> vm) {

        if (vm instanceof IListViewModel) {
            return;
        } else if (vm instanceof ComplexViewModel) {
            Object obj = vm.getModel();

            if (obj instanceof IPersistable<?>) {
                boolean newObject = false;
                IPersistable<?> persistable = (IPersistable<?>) obj;
                DbId<?> dbId = persistable.getDbId();

                if (null != dbId)
                    dbId.setDirty();
                else
                    newObject=true;

                getPM().save(persistable);
            }
        }
	}
}
