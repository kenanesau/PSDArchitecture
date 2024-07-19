package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;

import java.lang.reflect.Field;

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
public class ListViewModelFactory implements IListViewModelFactory{

    public ListViewModelFactory() {}

    public IListViewModel<?> createListVM(ComplexViewModel<?> parentVM, final Field modelField, ListVmMapping listAnno) throws IllegalAccessException {
        Class<?> viewModelType = listAnno.vmType();
        final Class<?> modelType = listAnno.modelType();

        IListViewModel<?> listVM;
        listVM = new FastListViewModel(parentVM.getMVVM(), parentVM, modelType, viewModelType);
        return listVM;
    }
}
