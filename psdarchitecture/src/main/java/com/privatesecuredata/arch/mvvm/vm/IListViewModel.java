package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.MVVM;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Created by kenan on 12/17/14.
 */
public interface IListViewModel<M, VM extends IViewModel<M>> {
    void init(ComplexViewModel<?> parentVM, Field modelField);
    boolean add(VM vm);
    boolean add(M object);
    boolean addAll(Collection<? extends  M> arg0);
    boolean addAll(int arg0, Collection<? extends M> arg1);
    M get(int pos);
    DbId getDbId(int pos);
    boolean isEmpty();
    boolean remove(M object);
    M remove(int location);
    boolean removeAll(Collection<?> arg0);
    int size();
    public VM getViewModel(int pos);
    ComplexViewModel<?> getParentViewModel();
    void setSortOrder(OrderBy... sortOrderTerms);

    public abstract void addViewModelListener(IViewModelChangedListener listener);
    public abstract void delViewModelListener(IViewModelChangedListener listener);
    public abstract void notifyViewModelDirty(IViewModel<?> changedModel, IViewModelChangedListener originator);
    public abstract void notifyViewModelDirty();
    public abstract void notifyModelChanged(IViewModel<?> changedModel, IViewModel<?> originator);
    public abstract void notifyModelChanged();

    public abstract boolean isDirty();
    public abstract void setDirty();
    public abstract void setClean();

    /**
     * commit all changes from the ViewModel to the Model;
     */
    public abstract void commit();

    /**
     * reload data from Model to ViewModel
     */
    public abstract void reload();

    public abstract List<M> getModel() throws MVVMException;

    public MVVM getMVVM();
}
