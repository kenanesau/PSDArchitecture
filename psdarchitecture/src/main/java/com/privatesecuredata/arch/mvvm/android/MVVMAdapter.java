package com.privatesecuredata.arch.mvvm.android;

import java.util.Hashtable;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.IDataBinding;
import com.privatesecuredata.arch.mvvm.IGetModelCommand;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.ViewToModelAdapter;

import android.view.View;

public class MVVMAdapter<COMPLEXVM extends IViewModel<?>> implements IDataBinding {
	View mainView;
	COMPLEXVM vm;
	private Hashtable<Integer, ViewToModelAdapter<?>> view2ModelAdapters = new Hashtable<Integer, ViewToModelAdapter<?>>();
	
	public MVVMAdapter(View mainView, COMPLEXVM vm) {
		if (null == mainView)
			throw new ArgumentException("Parameter \"Activity\" must not be null");
		this.mainView = mainView;
		
		if (null == mainView)
			throw new ArgumentException("Parameter \"COMPLEXVM\" must not be null");
		this.vm = vm;
	}
	
	
	public <T> void addModelMapping(Class<T> type, int viewId, IGetModelCommand<T> getModelCmd)
	{
		ViewToModelAdapter<T> adapter = (ViewToModelAdapter<T>)view2ModelAdapters.get(viewId);
		if (null==adapter) {
			adapter=new ViewToModelAdapter<T>(type, getModelCmd);
			View view = mainView.findViewById(viewId);
			if (null == view)
				throw new ArgumentException(String.format("Can not find View with Id %d", viewId));
			
			adapter.init(view, vm);
			adapter.updateView(view, vm);
			view2ModelAdapters.put(viewId, adapter);
		}
		else {
			adapter.setGetModelCommand(getModelCmd);
		}
	}

}
