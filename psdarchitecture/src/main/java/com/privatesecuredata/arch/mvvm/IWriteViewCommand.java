package com.privatesecuredata.arch.mvvm;

import android.view.View;


public interface IWriteViewCommand<T> {
	void set(View view, T val);
}
