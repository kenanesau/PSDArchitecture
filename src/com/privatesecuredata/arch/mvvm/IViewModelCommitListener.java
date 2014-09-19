package com.privatesecuredata.arch.mvvm;

/**
 * Interface for registering Callbacks which are called just after the commit happened;
 * 
 * @author kenan
 *
 */
public interface IViewModelCommitListener {
	void notifyCommit(IModel<?> vm);
}
