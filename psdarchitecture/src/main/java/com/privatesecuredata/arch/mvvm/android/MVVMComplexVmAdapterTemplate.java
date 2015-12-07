package com.privatesecuredata.arch.mvvm.android;

import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.binder.DisableViewBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewVisibilityBinder;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Objects of this class can be used to pre-define view-VM-binding without having to
 * have an actual view or viewmodel.
 *
 * This is used by the ViewModelListAdapter
 *
 * @param <COMPLEXVM> A complex VM
 * @sa ViewModelListAdapter
 */
public class MVVMComplexVmAdapterTemplate<COMPLEXVM extends IViewModel> implements IComplexVmAdapter {
    private boolean readOnly = false;
    /**
     * View-ID -> ViewtoVM-Adapter
     */
	private HashMap<Integer, List<ViewToVmBinder>> view2ModelAdapters = new HashMap<>();

    public MVVMComplexVmAdapterTemplate(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public MVVMComplexVmAdapterTemplate() {
    }

	@Override
    public <T> ViewToVmBinder setMapping(Class<T> type, int viewId, IGetVMCommand<T> getSimpleVmCmd)
	{
		List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
		if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, adapters);
        }

        ViewToVmBinder adapter = new ViewToVmBinder(type, getSimpleVmCmd, isReadOnly());
        adapters.add(adapter);

        return adapter;
	}

    @Override
    public ViewToVmBinder setViewDisableMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, adapters);
        }

        ViewToVmBinder adapter = new DisableViewBinder(getModelCmd);
        adapters.add(adapter);
        return adapter;
    }

    @Override
    public ViewToVmBinder setViewVisibilityMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, adapters);
        }

        ViewToVmBinder adapter = new ViewVisibilityBinder(getModelCmd);
        adapters.add(adapter);
        return adapter;
    }

    @Override
    public void setMapping(int viewId, ViewToVmBinder adapter)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            view2ModelAdapters.put(viewId, new ArrayList<ViewToVmBinder>());
        }
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }
    HashMap<Integer, List<ViewToVmBinder>> getViewToModelAdapters() { return view2ModelAdapters; }
}
