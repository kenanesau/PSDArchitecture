package com.privatesecuredata.arch.mvvm.android;

import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;

/**
 * Created by kenan on 11/27/15.
 */
public interface IComplexVmAdapter {
    /**
     * Function to set a mapping between a view-id SimpleValueVM which usually belongs to a ComplexVM.
     *
     * @param viewId ID of the View which later displays the data of the SimpleValueVM
     * @param adapter The adapter to establish the binding
     */
    void setMapping(int viewId, ViewToVmBinder adapter);

    /**
     * Set an "ordinary" mapping which puts the data of the SimpleValueVM into the View and vice
     * versa
     *
     * @param type
     * @param viewId
     * @param getSimpleVmCmd
     * @param <T>
     * @return
     */
    <T> ViewToVmBinder setMapping(Class<T> type, int viewId, IGetVMCommand<T> getSimpleVmCmd);

    /**
     * Convenience function to create a DisableViewBinder
     *
     * @param viewId
     * @param getModelCmd
     * @return
     * @sa DisableViewBinder
     */
    ViewToVmBinder setViewDisableMapping(int viewId, IGetVMCommand<Boolean> getModelCmd);

    /**
     * Convenience function to create a ViewVisibilityBinder
     * @param viewId
     * @param getModelCmd
     * @return
     * @sa ViewVisibilityBinder
     */
    ViewToVmBinder setViewVisibilityMapping(int viewId, IGetVMCommand<Boolean> getModelCmd);

    boolean isReadOnly();
}
