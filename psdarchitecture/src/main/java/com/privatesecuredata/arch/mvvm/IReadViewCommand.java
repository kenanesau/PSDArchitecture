package com.privatesecuredata.arch.mvvm;

import android.view.View;

public interface IReadViewCommand<T> {
	T get(View view);
}
