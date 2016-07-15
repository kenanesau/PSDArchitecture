package com.privatesecuredata.arch.db.vmGlue;

import android.database.Cursor;

import com.privatesecuredata.arch.db.CollectionProxyFactory;
import com.privatesecuredata.arch.db.CursorToListAdapter;
import com.privatesecuredata.arch.db.ICursorChangedListener;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersister;
import com.privatesecuredata.arch.db.LazyCollectionInvocationHandler;
import com.privatesecuredata.arch.db.ObjectRelation;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.FastListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModelFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collection;

/**
 * This Factory is used by the ComplexViewModel for all kinds of Lists. This is needed
 * to enable the update of the Models child-List-objects after an update.
 *
 * e.g.: the EncapsulatedListViewModel does not directly operate on the child-List of a model
 * but captures all changes directly on the database. These changes have to be reflected in the
 * models child-list (mainly size())...
 *
 * @see com.privatesecuredata.arch.mvvm.vm.ComplexViewModel
 *
 * Created by kenan on 12/17/14.
 */
public class DbListViewModelFactory implements IListViewModelFactory {
    private PersistanceManager pm;
    private MVVM mvvm;

    public DbListViewModelFactory(final PersistanceManager pm)
    {
        this.pm = pm;
        this.mvvm = MVVM.getMVVM(pm);
        setModelListCbGetter(new IModelListCbGetter() {
            @Override
            public CursorToListAdapter getCb() {
                return new CursorToListAdapter(pm);
            }
        });
    }

    public interface IModelListCbGetter
    {
        <V extends IPersistable> CursorToListAdapter<V> getCb();
    }

    IModelListCbGetter getter;

    public IListViewModel<?, ?> createListVM(ComplexViewModel<?> parentVM, final Field modelField, ListVmMapping listAnno) throws IllegalAccessException {
        Class<?> viewModelType = listAnno.vmType();
        final Class<?> modelType = listAnno.modelType();
        final Class<?> parentModelType = listAnno.parentType();
        IListViewModel<?, ?> listVM;
        final IPersistable model = (IPersistable)parentVM.getModel();
        if (null == model) {
            listVM = new FastListViewModel(this.mvvm, modelType, viewModelType);
        }
        else
        {
            CursorToListAdapter<?> cb = (CursorToListAdapter<?>)getter.getCb();
            IPersister parentPersister = pm.getUnspecificPersister(parentVM.getModel().getClass());
            ObjectRelation rel = parentPersister.getDescription().getOneToManyRelation(modelField.getName());
            Query query = rel.getAndCacheQuery(pm);
            if (null != query) {
                query.setForeignKeyParameter(model);
                cb.setQuery(query);
            }
            listVM = new EncapsulatedListViewModel(parentVM.getMVVM(), parentModelType, modelType, viewModelType, cb);
            final Object lst = parentVM.getModel();
            if (Proxy.isProxyClass(lst.getClass()))
            {
                LazyCollectionInvocationHandler handler = (LazyCollectionInvocationHandler)Proxy.getInvocationHandler(lst);
                cb.addCursorChangedListener(handler);
            }
            else
            {
                cb.addCursorChangedListener(new ICursorChangedListener() {
                    @Override
                    public void notifyCursorChanged(Cursor csr) throws IllegalAccessException {
                        if (null != csr) {
                            Collection lstItems = CollectionProxyFactory.getCollectionProxy(DbListViewModelFactory.this.pm, (Class) parentModelType, model, csr.getCount(), csr);
                            Collection oldItems = (Collection) modelField.get(model);
                            if ((null != oldItems) && (oldItems.size() != lstItems.size())) {
                                DbListViewModelFactory.this.pm.updateCollectionProxySize(model.getDbId(), modelField, lstItems);
                            }

                            modelField.setAccessible(true);
                            modelField.set(model, lstItems);
                        }
                    }
                });
            }
        }

        return listVM;
    }

    public void setModelListCbGetter(IModelListCbGetter cbGetter)
    {
        this.getter = cbGetter;
    }
}
