package com.privatesecuredata.arch.mvvm.vm;

/**
 * Simple interface used by EncapsulatedListViewModel.IModelListCallback to tell
 * the EncapsulatedListViewModel that the underlying cursor has changed
 *
 * Created by kenan on 4/10/15.
 */
public interface IDataChangedListener {
    void notifyDataChanged();
}
