package com.privatesecuredata.arch.mvvm;

public interface IWriteModelCommand<T> {
	void write(IViewModel vm, T val);
}
