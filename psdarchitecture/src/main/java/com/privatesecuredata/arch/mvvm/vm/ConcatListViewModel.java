package com.privatesecuredata.arch.mvvm.vm;

import android.support.v4.util.Pair;
import android.widget.Filter;
import android.widget.Filterable;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.mvvm.android.MVVMActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple ListViewModel which is readonly and can concatenate N other IListViewModels.
 * The contents of those ListViewModels are not intermixed but shown in a "concatenated"
 * manner
 */
public class ConcatListViewModel<M, VM extends IViewModel<M>> extends ComplexViewModel<List<M>>
        implements IListViewModel<M, VM>,
        Filterable
{
    ArrayList<IListViewModel> data;

    /**
     *
     * @param ctx Context
     * @param referencedType Type of the Model
     * @param vmType Type of the ViewModel
     * @param lists Array of IListViewModels
     */
    public ConcatListViewModel(MVVMActivity ctx,
                                     Class<M> referencedType,
                                     Class<VM> vmType,
                                     IListViewModel... lists)
    {
        data = new ArrayList<>(lists.length);

        for(IListViewModel vm : lists)
            data.add(vm);
    }

    protected Pair<IListViewModel<M, VM>, Integer> getLocalPos(int globalPosition) {
        Pair<IListViewModel<M, VM>, Integer> ret = null;
        int posCurrent = 0;
        int lastIterationListSize = 0;
        for (int i=0; i<data.size(); i++) {
            int localSize = data.get(i).size();
            posCurrent += localSize;

            if (globalPosition > posCurrent - 1)
                continue;

            ret = new Pair<>(data.get(i), globalPosition - lastIterationListSize);
            lastIterationListSize += localSize;
        }

        return ret;
    }

    @Override
    public void init(ComplexViewModel<?> parentVM, Field modelField) {

    }

    @Override
    public boolean add(VM vm) {
        return false;
    }

    @Override
    public boolean add(M object) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends M> arg0) {
        return false;
    }

    @Override
    public boolean addAll(int arg0, Collection<? extends M> arg1) {
        return false;
    }

    @Override
    public M get(int pos) {
        Pair <IListViewModel<M, VM>, Integer> pair = getLocalPos(pos);

        return (pair != null ? pair.first.get(pos) : null);
    }

    @Override
    public DbId getDbId(int pos) {
        return ((IPersistable)get(pos)).getDbId();
    }

    @Override
    public boolean isEmpty() {
        boolean ret = false;
        for (IListViewModel vm : data)
        {
            if (!vm.isEmpty()) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    @Override
    public boolean remove(M object) {
        return false;
    }

    @Override
    public M remove(int location) {
        return null;
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        return false;
    }

    @Override
    public int size() {
        int size = 0;
        for (IListViewModel vm : data)
            size += data.size();

        return size;
    }

    @Override
    public VM getViewModel(int pos) {
        Pair<IListViewModel<M, VM>, Integer> pair = getLocalPos(pos);

        return (pair != null ? pair.first.getViewModel(pair.second) : null);
    }

    @Override
    public void setSortOrder(OrderBy... sortOrderTerms) {
        for (IListViewModel<M, VM> vm : data)
            vm.setSortOrder(sortOrderTerms);
    }

    @Override
    public void setFilterParamId(String filterParamId) {
        for (IListViewModel<M, VM> vm : data)
            vm.setFilterParamId(filterParamId);
    }

    @Override
    public Filter getFilter() {
        return data.get(0).getFilter();
    }

}

