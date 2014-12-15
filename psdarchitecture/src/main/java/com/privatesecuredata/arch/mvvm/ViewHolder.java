package com.privatesecuredata.arch.mvvm;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

public class ViewHolder<T> implements IViewHolder<T> {
	IModelReaderStrategy<T> strategy;
	T model;
	IModelReaderStrategy.Pair[] pairs;
	
	public ViewHolder(IModelReaderStrategy<T> strategy, T model)
	{
		this.strategy = strategy;
		this.model = model;
		pairs = strategy.getValues(model);
	}
	
	public void addView(View view)
	{
		for (IModelReaderStrategy.Pair pair : pairs)
		{
			if (pair.id == view.getId())
			{
				pair.view = view;
				break;
			}
		}
	}
	
	public void updateViews(T newModel)
	{
		IModelReaderStrategy.Pair[] newPairs = strategy.getValues(newModel);
		for (int i=0; i<newPairs.length; i++)
		{
			newPairs[i].view = pairs[i].view; 
			View view = newPairs[i].view;
			Object val = newPairs[i].val;
			
			if (null == view)
				continue;
			
			if (view instanceof TextView)
				((TextView) view).setText((val != null ? val.toString() : ""));
			else if (view instanceof CompoundButton)
				((CompoundButton) view).setChecked((val != null ? (Boolean)val : false));
		}
		
		this.pairs = newPairs;
	}
}