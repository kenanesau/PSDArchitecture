package com.privatesecuredata.arch.mvvm.android;

import java.util.Hashtable;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DisableViewAdapter;
import com.privatesecuredata.arch.mvvm.IDataBinding;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.ViewToModelAdapter;

import android.view.View;

public class MVVMAdapter<COMPLEXVM extends IViewModel> implements IDataBinding {
	View mainView;
	COMPLEXVM vm;
	private Hashtable<Integer, ViewToModelAdapter<?>> view2ModelAdapters = new Hashtable<Integer, ViewToModelAdapter<?>>();
    boolean getDataFromView = false;

    public MVVMAdapter(View mainView, COMPLEXVM vm) {
        this(mainView, vm, false);
    }
	
	public MVVMAdapter(View mainView, COMPLEXVM vm, boolean getDataFromView) {
        this.getDataFromView = getDataFromView;
		if (null == mainView)
			throw new ArgumentException("Parameter \"mainView\" must not be null");
		this.mainView = mainView;

		if (null == vm)
			throw new ArgumentException("Parameter \"vm\" must not be null");
		this.vm = vm;
	}
	
	
	public <T> void setModelMapping(Class<T> type, int viewId, IGetVMCommand<T> getModelCmd)
	{
		ViewToModelAdapter<T> adapter = (ViewToModelAdapter<T>)view2ModelAdapters.get(viewId);
		if (null == adapter) {
			adapter = new ViewToModelAdapter<T>(type, getModelCmd);
			View view = mainView.findViewById(viewId);
			if (null == view)
				throw new ArgumentException(String.format("Can not find View with Id %d", viewId));
			
			adapter.init(view, vm);
            if (getDataFromView) {
                adapter.updateVM();
            }
            else {
                adapter.updateView(view, vm);
            }
			view2ModelAdapters.put(viewId, adapter);
		}
		else {
			adapter.setGetVMCommand(getModelCmd);
		}
	}

    public void setDisableViewMapping(int viewId, IGetVMCommand<Boolean> getModelCmd)
    {
        ViewToModelAdapter adapter = (ViewToModelAdapter)view2ModelAdapters.get(viewId);
        if (null == adapter) {
            adapter = new DisableViewAdapter(getModelCmd);
            View view = mainView.findViewById(viewId);
            if (null == view)
                throw new ArgumentException(String.format("Can not find View with Id %d", viewId));

            adapter.init(view, vm);
            adapter.updateView(view, vm);
            view2ModelAdapters.put(viewId, adapter);
        }
        else {
            adapter.setGetVMCommand(getModelCmd);
        }
    }

}
