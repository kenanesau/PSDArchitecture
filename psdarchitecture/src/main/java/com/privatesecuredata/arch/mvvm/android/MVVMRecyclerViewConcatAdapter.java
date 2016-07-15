package com.privatesecuredata.arch.mvvm.android;

import android.app.Activity;

import com.privatesecuredata.arch.mvvm.vm.IListViewModel;

/**
 * Created by kenan on 7/10/16.
 */
public class MVVMRecyclerViewConcatAdapter<M, COMPLEXVM> extends MVVMRecyclerViewModelAdapter {
    public MVVMRecyclerViewConcatAdapter(Activity ctx, IViewHolderFactory strategy) {
        super(ctx, strategy);
    }

    public void setData(IListViewModel... data) {

    }
}
