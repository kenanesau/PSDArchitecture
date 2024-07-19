package com.privatesecuredata.arch.mvvm.vm;

/**
 * A simple VM for saving types of (complex) viewmodels
 */
public class SimpleTypeVM extends SimpleValueVM<Class> {
    /**
     * Creates a new SimpleTypeVM which holds the type just passed to it as
     * a parameter
     *
     * @param data The type which is saved in this object
     */
    public SimpleTypeVM(Class data) {
        super(data);
    }

    /**
     * Create a new SimpelTypeVM which holds the type of the Viewmodel which
     * was passed to it as a parameter
     *
     * @param vm A (complex) Viewmodel
     */
    public SimpleTypeVM(IViewModel vm)
    {
        super(vm.getClass());
    }
}
