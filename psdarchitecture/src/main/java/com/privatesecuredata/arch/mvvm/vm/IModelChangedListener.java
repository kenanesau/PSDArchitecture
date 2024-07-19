package com.privatesecuredata.arch.mvvm.vm;

/**
 * Created by kenan on 7/3/15.
 */
public interface IModelChangedListener {
    /**
     * This method is called whenever a commit is finished and data was written
     * to the model...
     *
     * @param vm ViewModel notifying the listener
     * @param originator Original originator of the event
     */
    void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator);
}
