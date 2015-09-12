package com.privatesecuredata.arch.mvvm.android;


import java.util.ArrayList;

import com.privatesecuredata.arch.mvvm.binder.ViewToVmBinder;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewHolder;

import android.view.View;

public class MVVMViewHolder<COMPLEXVM extends IViewModel<?>> implements IViewHolder<COMPLEXVM> {
	private class Elem {
		// View containing Element of data
		public View view;
		
		// Adapter mapping this single data to a SimpleViewModel<T>
		public ViewToVmBinder<?> adapter;

		// Constructor
		public Elem(View v, ViewToVmBinder<?> a) {view = v; adapter=a; }
	}
	
	private ArrayList<Elem> mapping = new ArrayList<MVVMViewHolder<COMPLEXVM>.Elem>();
	
	public <T> void add(View view, ViewToVmBinder<T> adapter)
	{
		mapping.add(new Elem(view, adapter));
	}
	
	public void updateViews(COMPLEXVM vm)
	{
		for (Elem e : mapping)
		{
			e.adapter.updateView(e.view, vm);
		}
	}
}