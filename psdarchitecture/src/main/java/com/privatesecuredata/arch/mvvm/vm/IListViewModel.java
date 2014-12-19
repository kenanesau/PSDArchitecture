package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModel;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kenan on 12/17/14.
 */
public interface IListViewModel<M, VM extends IViewModel<M>> {
    void init(ComplexViewModel<?> parentVM, Method childModelGetter, Method childModelSetter);
    boolean add(VM vm);
    boolean add(M object);
    boolean addAll(Collection<? extends  M> arg0);
    boolean addAll(int arg0, Collection<? extends M> arg1);
    M get(int location);
    boolean isEmpty();
    boolean remove(M object);
    M remove(int location);
    boolean removeAll(Collection<?> arg0);
    int size();
}
