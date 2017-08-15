package com.privatesecuredata.arch.mvvm.android;

import android.view.View;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.binder.DisableViewBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;
import com.privatesecuredata.arch.mvvm.binder.ViewVisibilityBinder;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
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
     * View-ID -> ViewtoVM-Binder
     */
	private HashMap<Integer, List<ViewToVmBinder>> view2ModelAdapters = new HashMap<>();

    /**
     * BindableViews
     */
    private HashMap<IBindableView, IBindableView> bindableViews = new HashMap<>();

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

    protected List<ViewToVmBinder> getBinderList(int viewId) {
        List<ViewToVmBinder> binders = view2ModelAdapters.get(viewId);
        if (null == binders) {
            binders = new ArrayList<ViewToVmBinder>();
            view2ModelAdapters.put(viewId, binders);
        }

        return binders;
    }

	public <T> ViewToVmBinder setMapping(Class<T> type, int viewId, IGetVMCommand<T> getSimpleVmCmd)
	{
		List<ViewToVmBinder> binders = getBinderList(viewId);

        ViewToVmBinder binder = new ViewToVmBinder(type, getSimpleVmCmd, isReadOnly());
        binders.add(binder);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with id %d", viewId));

        binder.init(view, vm);
        if (this.ctx.isResumedActivity()) {
            binder.updateVM();
        }
        else {
            binder.updateView(vm);
        }

        return binder;
	}

    /**
     * Write all values from the VM to the View
     */
    public void updateView()
    {
        Set<Integer> keys = view2ModelAdapters.keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> binders = view2ModelAdapters.get(key);
            if (null != binders) {
                for (ViewToVmBinder binder : binders) {
                    /**
                     * update the view with the current values of the VM
                     */
                    binder.updateView(vm);
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
            List<ViewToVmBinder> binders = view2ModelAdapters.get(key);
            if (null != binders) {
                for (ViewToVmBinder binder : binders) {
                    /**
                     * update the view with the current values of the VM
                     */
                    binder.reinit(vm);
                }
            }
        }
    }

    public ViewToVmBinder setViewDisableMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> binders = getBinderList(viewId);

        ViewToVmBinder binder = new DisableViewBinder(getModelCmd);
        binders.add(binder);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with id %d", viewId));

        binder.init(view, vm);
        binder.updateView(vm);
        return binder;
    }

    public ViewToVmBinder setViewVisibilityMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        List<ViewToVmBinder> binders = getBinderList(viewId);

        ViewToVmBinder binder = new ViewVisibilityBinder(getModelCmd);
        binders.add(binder);
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with id %d", viewId));

        binder.init(view, vm);
        binder.updateView(vm);
        return binder;
    }

    public void setMapping(int viewId, ViewToVmBinder binder)
    {
        List<ViewToVmBinder> binders = getBinderList(viewId);

        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with id %d", viewId));

        binders.add(binder);
        binder.init(view, vm);
        binder.updateView(view, vm);
    }

    /**
     * Set a mapping between a view implementing IBindableView and a complex viewmodel
     *
     * @param viewId Id of the view implementing IBindableView
     * @param viewModel ComplexViewModel to bind the view to
     * @param <T> Type extending from ComplexViewModel
     */
    public <T extends ComplexViewModel> void setMapping(int viewId, T viewModel)
    {
        View view = mainView.findViewById(viewId);
        if (null == view)
            throw new ArgumentException(String.format("Can not find View with id %d!", viewId));
        if (view instanceof IBindableView) {
            IBindableView<T> bindableView = (IBindableView<T>)view;
            if (bindableViews.containsKey(bindableView)) {
                throw new ArgumentException(String.format("View with id %d is already bound!", viewId));
            }

            bindableViews.put(bindableView, bindableView);
            bindableView.bind(viewModel);
        }
        else {
            throw new ArgumentException(String.format("View with id %d does not implement IBindableView!", viewId));
        }
    }

    public void dispose()
    {
        Set<Integer> keys = view2ModelAdapters.keySet();

        for (Integer key : keys)
        {
            List<ViewToVmBinder> binders = view2ModelAdapters.get(key);
            if (null != binders) {
                for (ViewToVmBinder binder : binders) {
                    /**
                     * unregister VM-listener...
                     */
                    binder.dispose();
                }
            }
        }

        view2ModelAdapters.clear();

        for(IBindableView bindableView : bindableViews.values()) {
            bindableView.unbind();
        }

        bindableViews.clear();
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
