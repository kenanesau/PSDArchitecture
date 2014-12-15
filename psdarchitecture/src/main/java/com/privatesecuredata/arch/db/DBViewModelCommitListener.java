package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewModelCommitListener;

public class DBViewModelCommitListener implements IViewModelCommitListener {

	@Override
	public void notifyCommit(IViewModel<?> vm) {
		Object obj = vm.getModel();
		
		if (obj instanceof IPersistable<?>)
		{
			IPersistable<?> persistable = (IPersistable<?>)obj;
			DbId<?> dbId = persistable.getDbId();
			
			if (null == dbId)
				return;
			else
				dbId.setDirty();
		}
	}
}
