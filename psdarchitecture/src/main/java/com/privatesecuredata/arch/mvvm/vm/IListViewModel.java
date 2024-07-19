package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import io.reactivex.Observable;

/**
 * Created by kenan on 12/17/14.
 */
public interface IListViewModel<M>  extends IViewModelChangedListener, IModelChangedListener, IViewModel<List<M>> {
    void init(ComplexViewModel<?> parentVM, Field modelField);
    <VM extends IViewModel> boolean add(VM vm);
    boolean add(M object);
    boolean addAll(IListViewModel<M> list);
    boolean addAll(Collection<? extends  M> arg0);
    boolean addAll(int arg0, Collection<? extends M> arg1);
    M get(int pos);
    DbId getDbId(int pos);
    boolean isEmpty();
    boolean remove(M object);
    M remove(int location);
    boolean removeAll(Collection<?> arg0);
    int size();
    int dirtySize();
    <VM extends IViewModel> VM getViewModel(int pos);
    boolean hasViewModel(int pos);
    <T extends ComplexViewModel> T getParentViewModel();
    void clear();

    IDbBackedListViewModel<M> db();
}
