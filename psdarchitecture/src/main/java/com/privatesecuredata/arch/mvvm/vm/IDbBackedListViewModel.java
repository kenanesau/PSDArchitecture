package com.privatesecuredata.arch.mvvm.vm;

import android.widget.Filter;

/**
 * Interface for a sortable, filterable and queryable ListViewModel
 * (Usually backed by a DB-Cursor)
 */
public interface IDbBackedListViewModel {
    void setSortOrder(OrderBy... sortOrderTerms);
    void setFilterParamId(String filterParamId);
    Filter getFilter();

    void setQueryId(String queryId);
    void where(String id, Object val);
    void where(String id, Class val);
}
