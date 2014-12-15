package com.privatesecuredata.arch.mvvm.android;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersister;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IModelReaderStrategy;
import com.privatesecuredata.arch.mvvm.IModelReaderStrategy.Pair;
import com.privatesecuredata.arch.mvvm.IViewHolder;
import com.privatesecuredata.arch.mvvm.TransientViewToModelAdapter;
import com.privatesecuredata.arch.mvvm.ViewHolder;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

public class MVVMCursorAdapter<M extends IPersistable<M>> extends BaseAdapter
{
	Class<M> modelType;
	private final Context ctx;
	Cursor data;
	private IModelReaderStrategy<M> modelReaderStrategy;
	private SectionIndexer indexer;
	
	/**
	 * Id of the Row-layout
	 */
	private int rowViewId = -1;
	
	private Hashtable<Integer, TransientViewToModelAdapter<?>> view2ModelAdapters = new Hashtable<Integer, TransientViewToModelAdapter<?>>();
	private ArrayList<SimpleValueVM<Boolean>> selectedItemVMs;
	private IPersister<M> persister;
	
	public MVVMCursorAdapter(PersistanceManager pm, Class<M> modelClass, Context ctx)
	{
		modelType = modelClass;
		this.ctx = ctx;
		
		persister = (IPersister<M>) pm.getPersister(modelClass);
	}
	
	public MVVMCursorAdapter(PersistanceManager pm, Class<M> modelClass, Context ctx, Cursor cursor)
	{
		this(pm, modelClass, ctx);	
		setData(cursor);
	}
	
	public void setSectionIndexer(SectionIndexer indexer)
	{
		this.indexer = indexer;
		
		if (this.indexer instanceof AlphabetIndexer)
		{
			AlphabetIndexer idx = (AlphabetIndexer)indexer;
			idx.setCursor(this.data);
		}
	}
	
    /**
     * Performs a binary search or cache lookup to find the first row that matches a given section's starting letter.
     */
    public int getPositionForSection(int sectionIndex)
    {
        return indexer.getPositionForSection(sectionIndex);
    }

    /**
     * Returns the section index for a given position in the list by querying the item and comparing it with all items
     * in the section array.
     */
    public int getSectionForPosition(int position)
    {
        return indexer.getSectionForPosition(position);
    }

    /**
     * Returns the section array constructed from the alphabet provided in the constructor.
     */
    public Object[] getSections()
    {
        return indexer.getSections();
    }
	
	/*
	 * This method discards the old VM and register a new one
	 */
	public void setData(Cursor cursor)
	{
		this.data = cursor;
		if (null != this.indexer)
			setSectionIndexer(this.indexer);
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (null != data)
			return data.getCount();
		else
			return 0;
	}

	@Override
	public M getItem(int position) {
		M item = null;;
		try {
			item = persister.rowToObject(position, data);
		}
		finally {
			return item;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setRowViewId(int _rowViewId) {
		this.rowViewId = _rowViewId;
	}
	
	protected int getRowViewId() { return this.rowViewId; }
	
	/**
	 * This is called by (e.g.) a ListView to fill its row-view with data. 
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		M model = getItem(position); 
		
		if (null == rowView)
		{
			LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(getRowViewId(), parent, false);
			
			if (null==modelReaderStrategy)
				throw new MVVMException("No Modelreader-Strategy defined!!");
			

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
		
		
		if (null != modelReaderStrategy)
		{
			IViewHolder<M> viewHolder = (IViewHolder<M>) rowView.getTag();
			viewHolder.updateViews(model);
		}
		else 
		{
			throw new MVVMException("No ModelReader-Strategy defined!");
		}
		
		return rowView;
	}
	
	public void setModelReaderStrategy(IModelReaderStrategy<M> readerStrategy) {
		this.modelReaderStrategy = (IModelReaderStrategy<M>) readerStrategy;
	}
}
