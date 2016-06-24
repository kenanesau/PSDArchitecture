package com.privatesecuredata.arch.db.query;

/**
 * Use this class to create pre-defined queries
 * @param <T>
 */
public abstract class QueryTemplateBuilder<T> extends QueryBuilder<T> {

    public QueryTemplateBuilder() {
        Class type = configure();
        setDescriptionGetter(new DefaultDescriptionGetter(type));
    }

    @Override
    public abstract String id();

    /**
     * Implement this to configure the query and return the type of object the query returns
     */
    public abstract Class configure();
}
