package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.db.query.Query;

/**
 * Wraps a Query in a CursorLoader. This is used in AutomaticPersister for oneToMany-Realtionships
 * which have a more complex query attached.
 */
public class QueryCursorLoader implements ICursorLoader {

    Query _q;

    public QueryCursorLoader(Query q) {
        _q = q;
    }

    @Override
    public Cursor getCursor(DbId<?> foreignKey) {
        _q.setForeignKeyParameter(foreignKey);

        return _q.run();
    }

    @Override
    public Cursor getCursor(DbId<?> foreignKey, OrderByTerm... orderByTerms) {
        _q.setForeignKeyParameter(foreignKey);

        return _q.run(orderByTerms);
    }
}
