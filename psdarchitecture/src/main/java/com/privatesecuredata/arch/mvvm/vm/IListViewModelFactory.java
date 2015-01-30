package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;

import java.lang.reflect.Field;

/**
 * Created by kenan on 12/21/14.
 */
public interface IListViewModelFactory {
    IListViewModel<?, ?> createListVM(ComplexViewModel<?> parentVM, final Field modelField, ListVmMapping listAnno) throws IllegalAccessException;
}