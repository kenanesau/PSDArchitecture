package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.IViewModel;

/**
 * Interface for registering Callbacks which are called just after the commit happened;
 * 
 * @author kenan
 *
 */
public interface IViewModelCommitListener {
	void notifyCommit(IViewModel<?> vm);
}
