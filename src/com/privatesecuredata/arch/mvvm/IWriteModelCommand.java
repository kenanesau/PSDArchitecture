package com.privatesecuredata.arch.mvvm;

public interface IWriteModelCommand<T> {
	void write(IModel vm, T val);
}
