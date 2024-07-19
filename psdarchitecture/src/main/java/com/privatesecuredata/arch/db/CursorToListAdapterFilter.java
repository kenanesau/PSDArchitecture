package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.widget.Filter;

/**
 * Created by kenan on 4/3/15.
 */
public class CursorToListAdapterFilter extends Filter {
    CursorToListAdapter adapter;

    CursorToListAdapterFilter(CursorToListAdapter adapter)
    {
        this.adapter = adapter;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        Cursor csr = adapter.runQueryOnBackgroundThread(constraint);

        FilterResults res = new FilterResults();

        if (null != csr) {
            res.count = csr.getCount();
            res.values = csr;
        }

        return res;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        Cursor oldCsr = adapter.getCursor();

        if ( (results.values != null) && (results.values != oldCsr) )
            adapter.changeCursor((Cursor)results.values);
    }
}
