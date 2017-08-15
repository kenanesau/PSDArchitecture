package com.privatesecuredata.arch.mvvm.android;

import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;

/**
 * Created by kenan on 8/14/17.
 */

/**
 * Interface wich has to be implemented by Views/Controls to be bindable to a Complex-Viewmodel
 *
 * @param <T> Type of the Vievmodel extends from ComplexViewModel
 */
public interface IBindableView<T extends ComplexViewModel> {
    void bind(T vm);
    void unbind();
}
