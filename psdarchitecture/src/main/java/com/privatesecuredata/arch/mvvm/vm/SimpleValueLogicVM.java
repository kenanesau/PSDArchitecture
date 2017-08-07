package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.exceptions.ArgumentException;

/**
 * Created by kenan on 5/29/15.
 */
public class SimpleValueLogicVM<T> extends SimpleValueVM<T> {
    public interface ISetData<T> {
        boolean setCB(T currentValue, T newValue) throws ArgumentException;
    }
    public interface IGetData<T> {
        T get(T currentValue);
    }
    private SimpleValueVM[] _valueVMs;
    private ISetData<T> setDataCB;
    private IGetData<T> getDataCB;

    public SimpleValueLogicVM(T defVal, SimpleValueVM... valueVMs)
    {
        super(defVal);

        this._valueVMs = valueVMs;
        if (null != _valueVMs) {
            for (SimpleValueVM valVM : _valueVMs)
                valVM.addViewModelListener(this);
        }
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
            data = (this.getDataCB.get(data));
        }

        return data;
    }

    public SimpleValueVM<?> getParam(int i)
    {
        return _valueVMs[i];
    }

    public void setDataCBs(SimpleValueLogicVM.ISetData<T> setter, SimpleValueLogicVM.IGetData<T> getter)
    {
        this.setDataCB = setter;
        this.getDataCB = getter;
    }
}
