package com.privatesecuredata.arch.mvvm.vm;

import android.support.v4.util.Pair;
import android.widget.Filter;
import android.widget.Filterable;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.mvvm.MVVM;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

/**
 * Simple ListViewModel which is kind of readonly (you can not add but you can delete) and
 * can concatenate N other IListViewModels. The contents of those ListViewModels are not intermixed
 * but shown in a "concatenated" manner
 */
public class ConcatListViewModel<M> extends ComplexViewModel<List<M>>
        implements IListViewModel<M>, IDbBackedListViewModel,
        Filterable
{
    ArrayList<IListViewModel<M>> data;

    /**
     *
     * @param mvvm MVVM-object
     * @param lists Array of IListViewModels
     */
    public ConcatListViewModel(MVVM mvvm,
                               IListViewModel... lists)
    {
        super(mvvm);
        data = new ArrayList<>(lists.length);

        for(IListViewModel vm : lists) {
            data.add(vm);
            vm.addModelListener(this);
            registerChildVM(vm);
        }
    }

    @Override
    protected void addChild(IViewModel<?> vm) {
        /* Add as a child without adding ourselves as parent to the child */
        super.addChild(vm, false);
    }

    /**
     * Returns a Pair where the first-item is the ListViewModel which matches and
     * the second-item is the position within that ListViewModel
     *
     * @param globalPosition
     * @return
     */
    protected Pair<IListViewModel<M>, Integer> getLocalPos(int globalPosition) {
        Pair<IListViewModel<M>, Integer> ret = null;
        int posCurrent = 0;
        int lastIterationListSize = 0;
        for (int i=0; i < data.size(); i++) {
            int localSize = data.get(i).size();
            posCurrent += localSize;

            if (globalPosition > posCurrent - 1) {
                lastIterationListSize += localSize;
                continue;
            }

            ret = new Pair<>(data.get(i), globalPosition - lastIterationListSize);
            break;
        }

        return ret;
    }

    @Override
    public void init(ComplexViewModel<?> parentVM, Field modelField) {

    }

    @Override
    public <VM extends IViewModel> boolean add(VM vm) {
        return false;
    }

    @Override
    public boolean add(M object) {
        return false;
    }

    @Override
    public boolean addAll(IListViewModel<M> list) {
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
        Pair <IListViewModel<M>, Integer> pair = getLocalPos(pos);

        return (pair != null ? pair.first.get(pair.second) : null);
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
        for (IListViewModel vm : data) {
            if (remove(object))
                return true;
        }

        return false;
    }

    @Override
    public M remove(int location) {
        Pair <IListViewModel<M>, Integer> pair = getLocalPos(location);

        return (pair != null ? pair.first.remove(pair.second) : null);
    }

    @Override
    public boolean removeAll(Collection<?> arg0) {
        for (IListViewModel vm : data) {
            if (data.removeAll(arg0))
                return true;
        }

        return false;
    }

    @Override
    public int size() {
        int size = 0;
        for (IListViewModel vm : data)
            size += vm.size();

        return size;
    }

    @Override
    public int dirtySize() {
        return size();
    }

    @Override
    public <VM extends IViewModel> VM getViewModel(int pos) {
        Pair<IListViewModel<M>, Integer> pair = getLocalPos(pos);

        return (pair != null ? pair.first.getViewModel(pair.second) : null);
    }

    @Override
    public boolean hasViewModel(int pos) {
        Pair<IListViewModel<M>, Integer> pair = getLocalPos(pos);

        return (pair != null ? pair.first.hasViewModel(pair.second) : false);
    }

    @Override
    public IDbBackedListViewModel db() {
        return this;
    }

    @Override
    public void setSortOrder(OrderBy... sortOrderTerms) {
        for (IListViewModel<M> vm : data) {
            if (vm instanceof IDbBackedListViewModel)
                ((IDbBackedListViewModel)vm).setSortOrder(sortOrderTerms);
        }
    }

    @Override
    public void setFilterParamId(String filterParamId) {
        for (IListViewModel<M> vm : data)
            if (vm instanceof IDbBackedListViewModel)
                ((IDbBackedListViewModel)vm).setFilterParamId(filterParamId);
    }

    @Override
    public Filter getFilter() {
        IListViewModel vm = data.get(0);
        if (vm instanceof IDbBackedListViewModel)
            return ((IDbBackedListViewModel)vm).getFilter();
        else
            return null;
    }

    @Override
    public void setQueryId(String queryId) {
        for(IListViewModel vm : data)
            if (vm.db() != null)
                vm.db().setQueryId(queryId);
    }

    @Override
    public void where(String id, Object val) {
        for(IListViewModel vm : data)
            if (vm.db() != null)
                vm.db().where(id, val);
    }

    @Override
    public void where(String id, Class val) {
        for(IListViewModel vm : data)
            if (vm.db() != null)
                vm.db().where(id, val);
    }

    @Override
    public void clear() {
        for(IListViewModel vm : data)
            vm.clear();
    }

    @Override
    public void loadData() {
        for (IListViewModel<M> vm : data)
            if (null != vm.db())
                vm.db().loadData();
    }

    @Override
    public Observable<IListViewModel<M>> loadDataAsync() {
        List<Observable<IListViewModel<M>>> obs = new ArrayList<>();

        for (IListViewModel<M> vm : data)
            if (null != vm.db())
                obs.add(vm.db().loadDataAsync());

        return Observable.concat(obs)
                .map(new Function<IListViewModel<M>, IListViewModel<M>>() {
                    @Override
                    public IListViewModel<M> apply(IListViewModel<M> lst) throws Exception {
                        return ConcatListViewModel.this;
                    }
                });
    }

    public void dispose() {
        for(IListViewModel vm : data) {
            vm.delModelListener(vm);
            unregisterChildVM(vm);

            ///DO NOT DISPOSE THE CHILD-LIST-VMs -- they might be still attached to some VM in use...
            ///vm.dispose();
        }

        ///Throw out all references to the child ListViewModels
        data.clear();
    }

}

