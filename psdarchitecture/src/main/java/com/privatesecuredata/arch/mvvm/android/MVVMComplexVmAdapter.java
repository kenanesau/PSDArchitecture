package com.privatesecuredata.arch.mvvm.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.binder.DisableViewBinder;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import android.view.View;

public class MVVMComplexVmAdapter<COMPLEXVM extends IViewModel> {
    MVVMActivity ctx;
	View mainView;
	COMPLEXVM vm;
    /**
     * View-ID -> ViewtoVM-Adapter
     */
	private HashMap<Integer, List<ViewToVmBinder>> view2ModelAdapters = new HashMap<>();

	protected MVVMComplexVmAdapter(View mainView, COMPLEXVM vm) {
		if (null == mainView)
			throw new ArgumentException("Parameter \"mainView\" must not be null");
		this.mainView = mainView;

		if (null == vm)
			throw new ArgumentException("Parameter \"vm\" must not be null");
		this.vm = vm;
	}

    public MVVMComplexVmAdapter(MVVMActivity ctx, View mainView, COMPLEXVM vm) {
        this (mainView, vm);

        this.ctx = ctx;
        this.ctx.registerMVVMAdapter(this);
    }
	
	
	public <T> ViewToVmBinder setModelMapping(Class<T> type, int viewId, IGetVMCommand<T> getSimpleVmCmd)
	{
		List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
		if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, adapters);
        }

        ViewToVmBinder adapter = new ViewToVmBinder(type, getSimpleVmCmd);
        adapters.add(adapter);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapter.init(view, vm);
        if (this.ctx.isResumedActivity()) {
            adapter.updateVM();
        }
        else {
            adapter.updateView(view, vm);
        }

        return adapter;
	}

    public ViewToVmBinder setDisableViewMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, new ArrayList<ViewToVmBinder>());
        }

        ViewToVmBinder adapter = new DisableViewBinder(getModelCmd);
        adapters.add(adapter);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapter.init(view, vm);
        adapter.updateView(view, vm);
        return adapter;
    }

    public void setViewMapping(int viewId, ViewToVmBinder adapter)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            view2ModelAdapters.put(viewId, new ArrayList<ViewToVmBinder>());
        }

        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapters.add(adapter);
        adapter.updateView(view, vm);
    }

    public void dispose()
    {
        Set<Integer> keys = view2ModelAdapters.keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> adapters = view2ModelAdapters.get(key);
            if (null != adapters) {
                for (ViewToVmBinder adapter : adapters) {
                    /**
                     * unregister VM-listener...
                     */
                    adapter.dispose();
                }
            }
        }

        view2ModelAdapters.clear();
    }
}
