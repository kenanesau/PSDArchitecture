package com.privatesecuredata.arch.mvvm.vm;

/**
 * Interface which can be implemented by custom widgets, so that MVVMComplexVmAdapter and
 * ViewToVmBinder can access the value embedded in the widget.
 */
public interface IWidgetValueAccessor {
    void registerValueChanged(IWidgetValueReceiver valueReceiver);
    void unregisterValueChanged(IWidgetValueReceiver valueReceiver);

    void setValue(Object val);
    Object getValue();
}
