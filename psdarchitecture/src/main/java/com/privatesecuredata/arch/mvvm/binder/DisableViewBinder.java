package com.privatesecuredata.arch.mvvm.binder;

import android.view.View;

import com.privatesecuredata.arch.mvvm.IGetVMCommand;

/**
 * Created by kenan on 9/1/15.
 */
public class DisableViewBinder extends ManipulateViewBinder<Boolean> {
    public DisableViewBinder(IGetVMCommand<Boolean> getVMCommand)
    {
        super(Boolean.class, getVMCommand);
        setCommand(new ManipulateViewBinder.IManipulateViewCmd<Boolean>() {

            @Override
            public void execute(View view, Boolean data) {
                if (!DisableViewBinder.this.isVMUpdatesView())
                    view.setEnabled(data);
            }
        });
    }
}
