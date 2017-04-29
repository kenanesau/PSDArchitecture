package com.privatesecuredata.arch.db.query;

import android.widget.Filter;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.vm.IDbBackedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.OrderBy;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

/**
 * Caches all parameters which can be needed by a query.
 */
public class QueryParameterCache implements IDbBackedListViewModel {
    private class Pair {
        private String id;
        private Object objVal;
        private Class classVal;

        public Pair(String id, Object val) {
            this.id = id;
            this.objVal = val;
        }

        public Pair(String id, Class val) {
            this.id = id;
            this.classVal = val;
        }

        public void configQuery(Query q) {
            if (null == objVal)
                q.setParameter(id, objVal);
            else
                q.setParameter(id, classVal);
        }

        public void configList(IDbBackedListViewModel list) {
            if (null != objVal)
                list.where(id, objVal);
            else
                list.where(id, classVal);
        }
    }

    private String filteredParamId = null;
    private OrderBy[] sortOrder;
    private String queryId = null;
    private String whereId = null;
    private List<Pair> whereParams = new ArrayList<>();
    private Object whereClassVal = null;

    @Override
    public void setSortOrder(OrderBy... sortOrderTerms) {
        this.sortOrder = sortOrderTerms;
    }

    public OrderBy[] getSortOrder() {
        return this.sortOrder;
    }

    @Override
    public void setFilterParamId(String filterParamId) {
        this.filteredParamId = filterParamId;
    }

    public String getFilteredParamId() {
        return this.filteredParamId;
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryId() {
        return this.queryId;
    }

    @Override
    public void where(String id, Object val) {
        whereParams.add(new Pair(id, val));
    }

    @Override
    public void where(String id, Class val) {
        whereParams.add(new Pair(id, val));
    }

    @Override
    public void loadData() {
    }

    @Override
    public Observable<IListViewModel> loadDataAsync() {
        return null;
    }

    /**
     * Configure a ListViewModels query according to the cached settings
     * @param listViewModel
     */
    public void configureListViewModel(IDbBackedListViewModel listViewModel) {
        if (null != sortOrder)
            listViewModel.setSortOrder(sortOrder);

        if (null != filteredParamId)
            listViewModel.setFilterParamId(filteredParamId);

        if (null != queryId) {
            listViewModel.setQueryId(queryId);

            for (Pair where : whereParams)
                where.configList(listViewModel);
        }
        if ( (null == queryId) && (whereParams.size() > 0) )
            throw new ArgumentException(String.format("You have configured '%d' where-clauses but not Query-ID!!"));
    }
}
