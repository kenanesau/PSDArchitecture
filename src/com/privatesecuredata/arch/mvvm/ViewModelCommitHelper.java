package com.privatesecuredata.arch.mvvm;

import java.util.ArrayList;
import java.util.Collection;

/**
 * You need this helper-class if you want to implement your own Viewmodel which
 * just implement IModel and do not derive from ViewModel -> call ViewModelCommitHelper.notifyCommit() 
 * at the end of your commit()-method.
 * 
 * If you implement a component which wants to be notified if a (Complex)ViewModel was dirty and committed
 * -- register here, you will be called back after the commit has finished.
 * 
 * @author kenan
 *
 */
public class ViewModelCommitHelper {
	private static Collection<IViewModelCommitListener> globalCommitListeners = new ArrayList<IViewModelCommitListener>();
	
	public static void notifyCommit(IViewModel<?> vm) {
		for(IViewModelCommitListener listener : globalCommitListeners)
			listener.notifyCommit(vm);
	}
	
	public static void addGlobalCommitListener(IViewModelCommitListener listener)
	{
		globalCommitListeners.add(listener);
	}
	
	public static void delGlobalCommitListener(IViewModelCommitListener listener)
	{
		globalCommitListeners.remove(listener);
	}

}
