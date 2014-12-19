package com.privatesecuredata.arch.mvvm.vm;

import android.database.Cursor;

import com.privatesecuredata.arch.db.CollectionProxyFactory;
import com.privatesecuredata.arch.db.CursorToListAdapter;
import com.privatesecuredata.arch.db.ICursorChangedListener;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.LazyCollectionInvocationHandler;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;

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
 * TODO: It would be nice to have this database-specific stuff not direcly under arch.mvvm.vm
 *
 * @see com.privatesecuredata.arch.mvvm.vm.ComplexViewModel
 *
 * Created by kenan on 12/17/14.
 */
public class ListViewModelFactory {
    private PersistanceManager pm;
    private static ListViewModelFactory defaultFactory;

    public static void setDefaultFactory(ListViewModelFactory factory) {
        defaultFactory = factory;
    }

    public static ListViewModelFactory getDefaultFactory() { return ListViewModelFactory.defaultFactory; }

    public ListViewModelFactory(PersistanceManager pm)
    {
        this.pm = pm;
    }

    public PersistanceManager getPM() { return this.pm; }

    public interface IModelListCbGetter
    {
        <V extends IPersistable<V>> CursorToListAdapter<V> getCb();
    }

    IModelListCbGetter getter;

    public IListViewModel<?, ?> createListVM(ComplexViewModel<?> parentVM, final Field childModelField, ListVmMapping listAnno) throws IllegalAccessException {
        Class<?> viewModelType = listAnno.vmType();
        final Class<?> modelType = listAnno.modelType();
        final Class<?> parentModelType = listAnno.parentType();
        IListViewModel<?, ?> listVM;
        final IPersistable<?> model = (IPersistable<?>)parentVM.getModel();
        if (null == model) {
            listVM = new FastListViewModel(modelType, viewModelType);
        }
        else
        {
            CursorToListAdapter<?> cb = (CursorToListAdapter<?>)getter.getCb();
            listVM = new EncapsulatedListViewModel(parentModelType, modelType, viewModelType, cb);
            childModelField.setAccessible(true);
            Object lst = childModelField.get(model);
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
                        Collection lstItems = CollectionProxyFactory.getCollectionProxy(getPM(), (Class)parentModelType, model, csr.getCount(), csr);
                        childModelField.setAccessible(true);
                        childModelField.set(model, lstItems);
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
