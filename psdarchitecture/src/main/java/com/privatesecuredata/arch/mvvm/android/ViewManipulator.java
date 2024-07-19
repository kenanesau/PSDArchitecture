package com.privatesecuredata.arch.mvvm.android;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by kenan on 9/15/15.
 */
public class ViewManipulator<T, VM> {
    private VM vm;
    private ViewGroup parent;

    protected Integer viewId;
    protected IManipulationCommand<T, VM> cmd;
    private int pos = -1;

    protected ViewManipulator() {}

    public interface IManipulationCommand<T, VM> {
        void execute(int position, T data, VM vm, View view, ViewGroup parent);
    }

    public ViewManipulator(Integer viewId, IManipulationCommand<T, VM> command) {
        this.cmd = command;
        this.viewId = viewId;
    }

    public ViewManipulator(ViewManipulator other)
    {
        this.cmd = other.cmd;
        this.setVm((VM) other.getVm());
        this.viewId = other.viewId;
        // DO NOT COPY THE VIEW-OBJECT...
    }

    public void setVm(VM vm) {
        this.vm = vm;
    }

    public VM getVm() { return this.vm; }
    public ViewGroup getParent() { return parent; }

    /**
     * Manipulate a rowView which might be part of a row-View of a list...
     * @param data
     * @param rowView
     */
    public void manipulate(int pos, T data, View rowView, ViewGroup parent)
    {
        this.parent = parent;
        this.cmd.execute(pos, data, this.getVm(), rowView, parent);
    }
}
