package com.privatesecuredata.arch.mvvm.android;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListView;

import com.privatesecuredata.arch.mvvm.IDataBinding;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.IModelChangedListener;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.IModelReaderStrategy;
import com.privatesecuredata.arch.mvvm.IModelReaderStrategy.Pair;
import com.privatesecuredata.arch.mvvm.IViewHolder;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.TransientViewToModelAdapter;
import com.privatesecuredata.arch.mvvm.ViewHolder;
import com.privatesecuredata.arch.mvvm.ViewToModelAdapter;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.FastListViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

public class MVVMFastListViewModelAdapter<M, COMPLEXVM extends IViewModel<M>> extends BaseAdapter
															implements IDataBinding, IViewModelChangedListener
{
	Class<M> modelType;
	Class<COMPLEXVM> viewModelType;
	private final Context ctx;
	private FastListViewModel<M, COMPLEXVM> data;
	private IModelReaderStrategy<M> modelReaderStrategy;
		
	
	/**
	 * Id of the Row-layout
	 */
	private int rowViewId = -1;
	
	/**
	 * Id of the Checkbox in the Row-Layout which is used for selection
	 */
	private int selectionViewId = -1;
	
	private Hashtable<Integer, TransientViewToModelAdapter<?>> view2ModelAdapters = new Hashtable<Integer, TransientViewToModelAdapter<?>>();
	private ArrayList<SimpleValueVM<Boolean>> selectedItemVMs;
	
	public MVVMFastListViewModelAdapter(Class<M> modelClass, Class<COMPLEXVM> vmClass, Context ctx)
	{
		modelType = modelClass;
		viewModelType = vmClass;
		this.ctx = ctx;
	}
	
	public MVVMFastListViewModelAdapter(Class<M> modelClass, Class<COMPLEXVM> vmClass, Context ctx, MVVM mvvm, List<M> lst)
	{
		this(modelClass, vmClass, ctx);		
		setData(mvvm, lst);
	}
	
	public MVVMFastListViewModelAdapter(Class<M> modelClass, Class<COMPLEXVM> vmClass, Context ctx, FastListViewModel<M, COMPLEXVM> lstVMs)
	{
		this(modelClass, vmClass, ctx);		
		setData(lstVMs);
	}
	
	/*
	 * This method discards the old VM and register a new one
	 */
	public void setData(FastListViewModel<M, COMPLEXVM> data)
	{
		if (null != this.data)
		{
			this.data.delViewModelListener(this);
		}
		//data.load();
		this.data = data;
		this.data.addViewModelListener(this);
		this.notifyDataSetChanged();
	}
	
	protected void setData(MVVM mvvm, List<M> lst)
	{
		FastListViewModel<M, COMPLEXVM> lstVM = new FastListViewModel<M, COMPLEXVM>(mvvm, modelType, viewModelType);
		lstVM.init(lst);
		setData(lstVM);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public M getItem(int position) {
		return this.data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setRowViewId(int _rowViewId) {
		this.rowViewId = _rowViewId;
	}
	
	protected int getRowViewId() { return this.rowViewId; }
	
	public <T> void setModelMapping(Class<T> type, int viewId, IGetVMCommand<T> getModelCmd)
	{
		TransientViewToModelAdapter<T> adapter = (TransientViewToModelAdapter<T>)view2ModelAdapters.get(viewId);
		if (null==adapter) {
			adapter=new TransientViewToModelAdapter<T>(type);
			view2ModelAdapters.put(viewId, adapter);
		}
		adapter.setGetVMCommand(getModelCmd);
	}
	
	/**
	 * This is called by (e.g.) a ListView to fill its row-view with data. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		M model = this.data.get(position); 
		COMPLEXVM vm = null;
		
		if (null == rowView)
		{
			LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(getRowViewId(), parent, false);
			
			if (null != modelReaderStrategy)
			{
				ViewHolder<M> holder = new ViewHolder<M>(modelReaderStrategy, model);
				
				for(Pair pair : modelReaderStrategy.getValues(model))
				{
					View elementview = rowView.findViewById(pair.id);
					if (null != elementview)
					{
						holder.addView(elementview);
					}
				}
				
				rowView.setTag(holder);
			}
			else {
				
				MVVMViewHolder<COMPLEXVM> holder = new MVVMViewHolder();

				Enumeration<Integer> keys = view2ModelAdapters.keys();
				vm = this.data.getViewModel(position);

				while(keys.hasMoreElements())
				{
					Integer viewId = keys.nextElement();
					ViewToModelAdapter<?> adapter = view2ModelAdapters.get(viewId).clone();

					View elementview = rowView.findViewById(viewId);
					if (null != elementview)
					{
						adapter.init(elementview, vm);
						holder.add(elementview, adapter);
					}
				}
				
				rowView.setTag(holder);
			}
		}
		
		
		if (null != modelReaderStrategy)
		{
			IViewHolder<M> viewHolder = (IViewHolder<M>) rowView.getTag();
			viewHolder.updateViews(model);
		}
		else 
		{
			IViewHolder<COMPLEXVM> viewHolder = (IViewHolder<COMPLEXVM>) rowView.getTag();
			viewHolder.updateViews(vm);
		}
		
		if (this.selectionViewId>-1)
		{
			View selectionView = rowView.findViewById(selectionViewId);
			
			if (null != selectionView) {
				ViewParent viewParent = (ViewParent)parent;
				ViewParent oldParent = null;
				if (selectionView instanceof Checkable) {
					ListView lv = null;
					do {
						if (parent instanceof ListView)
							lv = (ListView)parent;
						else {
							oldParent = viewParent;
							viewParent = parent.getParent();
						}
					} while ( (viewParent != oldParent) && (lv == null) );
					
					if (null != lv)
						((Checkable)selectionView).setChecked(lv.isItemChecked(position));
				}
			}
		}

		return rowView;
	}
	
	/**
	 * Set the view-Id of the view (e.g. Checkbox) which is used for selection 
	 *  
	 * @param id ID of the view
	 */
	public void setSelectionViewId(int id) 
	{ 
		this.selectionViewId = id;
		
		this.setModelMapping(Boolean.class, id, new IGetVMCommand<Boolean>() {
			@Override
			public SimpleValueVM<Boolean> getVM(IViewModel<?> vm) {
				if (vm instanceof ComplexViewModel<?>)
					return ((ComplexViewModel<?>) vm).getSelectedVM();
				else
					return new SimpleValueVM<Boolean>(false);
			}
		});
	}

	/**
	 * Called by underlying ListViewModel when data has changed
	 */
	@Override
	public void notifyViewModelDirty(IViewModel<?> vm, IViewModelChangedListener originator) {
		//notify (list)view of changed data -> redraw
		this.notifyDataSetChanged();
	}

    public void setModelReaderStrategy(IModelReaderStrategy<M> readerStrategy) {
		this.modelReaderStrategy = (IModelReaderStrategy<M>) readerStrategy;
	}
}
