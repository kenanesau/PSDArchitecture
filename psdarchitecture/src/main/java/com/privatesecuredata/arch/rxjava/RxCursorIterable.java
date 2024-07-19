package com.privatesecuredata.arch.rxjava;
import android.database.Cursor;

import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersister;
import com.privatesecuredata.arch.db.PersistanceManager;

import java.util.Iterator;


/**
 * Created by kenan on 4/21/17.
 */

public class RxCursorIterable<T extends IPersistable> implements Iterable<T> {

    private final PersistanceManager _pm;
    private final Cursor _csr;
    private final Class<T> _type;

    public RxCursorIterable(PersistanceManager pm, Cursor csr, Class<T> type) {
        _pm = pm;
        _csr = csr;
        _type = type;
    }

    public static <U extends IPersistable> RxCursorIterable<U> from(PersistanceManager pm, Cursor csr, Class<U> type) {
        return new RxCursorIterable<U>(pm, csr, type);
    }

    @Override
    public Iterator<T> iterator() {
        return new RxCursorIterator<T>(_pm, _csr, _type);
    }

    static class RxCursorIterator<T extends  IPersistable> implements Iterator<T> {

        private final PersistanceManager _pm;
        private final Cursor _csr;
        private final IPersister<T> _persister;
        private int position = 0;

        public RxCursorIterator(PersistanceManager pm, Cursor csr, Class<T> type) {
            _pm = pm;
            _csr = csr;
            _persister = pm.getPersister(type);
        }

        @Override
        public boolean hasNext() {
            return !_csr.isClosed() && !_csr.isLast() && _csr.getCount() > 0;
        }

        @Override
        public T next() {
            return _pm.load(_persister, _csr, position++);
        }
    }
}
