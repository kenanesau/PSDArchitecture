package com.privatesecuredata.arch.mvvm.binder;

import android.view.View;

import com.privatesecuredata.arch.mvvm.IGetVMCommand;

/**
 * Created by kenan on 9/1/15.
 */
public class ViewVisibilityBinder extends ManipulateViewBinder<Boolean> {
    public ViewVisibilityBinder(IGetVMCommand<Boolean> getVMCommand)
    {
        super(Boolean.class, getVMCommand);
        setCommand(new IManipulateViewCmd<Boolean>() {

            @Override
            public void execute(View view, Boolean visible) {
                if (!ViewVisibilityBinder.this.isVMUpdatesView()) {
                    if (visible)
                        view.setVisibility(View.VISIBLE);
                    else
                        view.setVisibility(View.GONE);
                }
            }
        });
    }
}
