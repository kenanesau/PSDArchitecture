package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.mvvm.vm.IViewModel;

public interface IViewModelChangedListener {
    /**
     * This method is called whenever a VM is changed
     *
     * @param vm ViewModel notifying the listener
     * @param originator Original originator of the event
     */
	void notifyViewModelDirty(IViewModel<?> vm, IViewModel<?> originator);

    /**
     * This method is called whenever a commit is finished and data was written
     * to the model...
     *
     * @param vm ViewModel notifying the listener
     * @param originator Original originator of the event
     */
    void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator);
}
