package com.privatesecuredata.arch.mvvm;

import android.view.View;

public interface IModelReaderStrategy<T> {
	public class Pair {
		public Integer id;
		public Object val;
		public View view;

		public Pair(int id, String val) {
			this.id = id;
			this.val = val;
		}
	}
	
	Pair[] getValues(T item);
}
