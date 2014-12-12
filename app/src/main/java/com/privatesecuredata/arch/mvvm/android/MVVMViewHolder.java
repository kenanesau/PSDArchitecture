package com.privatesecuredata.arch.mvvm.android;


import java.util.ArrayList;

import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.IViewHolder;
import com.privatesecuredata.arch.mvvm.ViewToModelAdapter;

import android.view.View;

public class MVVMViewHolder<COMPLEXVM extends IViewModel<?>> implements IViewHolder<COMPLEXVM> {
	private class Elem {
		// View containing Element of data
		public View view;
		
		// Adapter mapping this single data to a SimpleViewModel<T>
		public ViewToModelAdapter<?> adapter;

		// Constructor
		public Elem(View v, ViewToModelAdapter<?> a) {view = v; adapter=a; }
	}
	
	private ArrayList<Elem> mapping = new ArrayList<MVVMViewHolder<COMPLEXVM>.Elem>();
	
	public <T> void add(View view, ViewToModelAdapter<T> adapter)
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