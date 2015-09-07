package com.privatesecuredata.arch.mvvm;

import android.view.View;

/**
 * Created by kenan on 9/1/15.
 */
public class DisableViewAdapter extends ViewToModelAdapter {
    public DisableViewAdapter(IGetVMCommand<Boolean> getVMCommand)
    {
        super(Boolean.class, getVMCommand);
    }

    @Override
    protected void detectViewCommands(View view) {
        setWriteViewCommand(new IWriteViewCommand<Boolean>() {
            @Override
            public void set(View view, Boolean val) {
                if (!isVMUpdatesView())
                    view.setEnabled(val);
            }
        });
    }
}
