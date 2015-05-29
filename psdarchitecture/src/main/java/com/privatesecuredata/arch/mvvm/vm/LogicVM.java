package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.exceptions.ArgumentException;

/**
 * With this VM you can execute code whenever the data of this VM changes or is
 * accesed. One can use this for validating the data which is written to this VM.
 *
 * Whenever the data should not be written to the VM the ISetDataCB has to return false.
 **/
public class LogicVM<T> extends SimpleValueVM<T> {

    public interface ISetData<T> {
        boolean setCB(T currentValue, T newValue) throws ArgumentException;
    }
    public interface IGetData<T> {
        T getCB(T currentValue);
    }

    private ISetData<T> setDataCB;
    private IGetData<T> getDataCB;

    public LogicVM(Class<T> clazz, T data, ISetData<T> setDataCB, IGetData<T> getDataCB) {
        super(clazz, data);

        this.setDataCB = setDataCB;
        this.getDataCB = getDataCB;
    }

    public LogicVM(T data, ISetData<T> setDataCB, IGetData<T> getDataCB) {
        super(data);

        this.setDataCB = setDataCB;
        this.getDataCB = getDataCB;
    }

    @Override
    public void set(T newData) throws ArgumentException{
        T data = super.get();

        if ( (null == data) || (!data.equals(newData)) ) {
            if (null != this.setDataCB) {
                if (this.setDataCB.setCB(super.get(), newData))
                    super.set(newData);
                else
                    throw new ArgumentException(String.format("Setting value %s to viewmodel failed", data.toString()));
            }
            else
                super.set(newData);
        }
    }

    @Override
    public T get() {
        T data = super.get();
        if (null != this.getDataCB) {
            data = (this.getDataCB.getCB(data));
        }

        return data;
    }

}
