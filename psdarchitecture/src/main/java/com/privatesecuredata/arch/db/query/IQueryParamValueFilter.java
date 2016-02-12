package com.privatesecuredata.arch.db.query;

/**
 * Interface for the callback which is used when the value
 * on a QueryParameter is set by the Query.
 *
 * Using this interface the value which is set to the parameter can be "decorated" to
 * accomodate the SQL.
 *
 * @sa QueryParameter
 * @sa Query
 */
public interface IQueryParamValueFilter {
    /**
     * This method is called each time the value for a QueryParameter is set
     * @param oldValue Value which is "put in" by the user
     * @return The actual value like it is present in the QueryParameter
     */
    Object filterValue(Object oldValue);
}
