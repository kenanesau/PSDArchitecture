package com.privatesecuredata.arch.mvvm.android;

import android.view.View;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.binder.DisableViewBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewVisibilityBinder;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MVVMComplexVmAdapter<COMPLEXVM extends IViewModel> implements IComplexVmAdapter {
    private MVVMActivity ctx;
    private View mainView;
    private COMPLEXVM vm;
    private boolean readOnly = false;
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

    public MVVMComplexVmAdapter(MVVMActivity ctx, View mainView, COMPLEXVM vm, boolean readOnly) {
        this(ctx, mainView, vm);
        this.readOnly = readOnly;
    }

    public MVVMComplexVmAdapter(MVVMActivity ctx, View mainView, COMPLEXVM vm) {
        this (mainView, vm);

        this.ctx = ctx;
        this.ctx.registerMVVMAdapter(this);
    }

    public MVVMComplexVmAdapter(MVVMActivity ctx, MVVMComplexVmAdapterTemplate<COMPLEXVM> template,
                                View mainView, COMPLEXVM vm)
    {
        this(ctx, mainView, vm);

        this.vm = vm;
        Set<Integer> keys = template.getViewToModelAdapters().keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> adapters = template.getViewToModelAdapters().get(key);
            if (null != adapters) {
                for (ViewToVmBinder adapter : adapters) {
                    /**
                     * update the view with the current values of the VM
                     */
                    setMapping(key, adapter);
                }
            }
        }

    }

    public MVVMComplexVmAdapter(MVVMActivity ctx, MVVMComplexVmAdapterTemplate<COMPLEXVM> template,
                                View mainView, COMPLEXVM vm, boolean readOnly)
    {
        this(ctx, template, mainView, vm);
        this.readOnly = readOnly;
    }

	public <T> ViewToVmBinder setMapping(Class<T> type, int viewId, IGetVMCommand<T> getSimpleVmCmd)
	{
		List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
		if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, adapters);
        }

        ViewToVmBinder adapter = new ViewToVmBinder(type, getSimpleVmCmd, isReadOnly());
        adapters.add(adapter);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapter.init(view, vm);
        if (this.ctx.isResumedActivity()) {
            adapter.updateVM();
        }
        else {
            adapter.updateView(vm);
        }

        return adapter;
	}

    /**
     * Write all values from the VM to the View
     */
    public void updateView()
    {
        Set<Integer> keys = view2ModelAdapters.keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> adapters = view2ModelAdapters.get(key);
            if (null != adapters) {
                for (ViewToVmBinder adapter : adapters) {
                    /**
                     * update the view with the current values of the VM
                     */
                    adapter.updateView(vm);
                }
            }
        }
    }

    /**
     * Set a new VM an redo all the wiring with the ViewToVmBinders
     *
     * @param vm The ViewModel
     */
    public void updateViewModel(COMPLEXVM vm)
    {
        this.vm = vm;
        Set<Integer> keys = view2ModelAdapters.keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> adapters = view2ModelAdapters.get(key);
            if (null != adapters) {
                for (ViewToVmBinder adapter : adapters) {
                    /**
                     * update the view with the current values of the VM
                     */
                    adapter.reinit(vm);
                }
            }
        }
    }

    public ViewToVmBinder setViewDisableMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
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
        adapter.updateView(vm);
        return adapter;
    }

    public ViewToVmBinder setViewVisibilityMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            adapters = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, new ArrayList<ViewToVmBinder>());
        }

        ViewToVmBinder adapter = new ViewVisibilityBinder(getModelCmd);
        adapters.add(adapter);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapter.init(view, vm);
        adapter.updateView(vm);
        return adapter;
    }

    public void setMapping(int viewId, ViewToVmBinder adapter)
    {
        List<ViewToVmBinder> adapters = view2ModelAdapters.get(viewId);
        if (null == adapters) {
            adapters = new ArrayList<>();
            view2ModelAdapters.put(viewId, adapters);
        }

        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

        adapters.add(adapter);
        adapter.init(view, vm);
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

    public boolean isReadOnly() {
        return readOnly;
    }
}
