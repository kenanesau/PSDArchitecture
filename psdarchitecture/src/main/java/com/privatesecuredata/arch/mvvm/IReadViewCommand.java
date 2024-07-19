package com.privatesecuredata.arch.mvvm;

import android.view.View;

import com.privatesecuredata.arch.exceptions.MVVMException;

public interface IReadViewCommand<T> {
	T get(View view) throws MVVMException;
}
