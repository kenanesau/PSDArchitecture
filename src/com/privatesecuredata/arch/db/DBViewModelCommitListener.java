package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.mvvm.IModel;
import com.privatesecuredata.arch.mvvm.IViewModelCommitListener;

public class DBViewModelCommitListener implements IViewModelCommitListener {

	@Override
	public void notifyCommit(IModel<?> vm) {
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
