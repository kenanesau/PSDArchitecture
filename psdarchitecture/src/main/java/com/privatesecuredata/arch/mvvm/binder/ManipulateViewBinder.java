package com.privatesecuredata.arch.mvvm.binder;

import android.view.View;

import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.IWriteViewCommand;

/**
 * Created by kenan on 9/1/15.
 */
public class ManipulateViewBinder<T> extends ViewToVmBinder<T> {
    private IManipulateViewCmd _cmd;

    public interface IManipulateViewCmd<T> {
        void execute(View view, T data);
    }

    protected ManipulateViewBinder(Class<T> type, IGetVMCommand<T> getVMCommand)
    {
        super(type, getVMCommand);
    }

    protected void setCommand(IManipulateViewCmd cmd)
    {
        _cmd = cmd;
    }

    public ManipulateViewBinder(Class<T> type, IGetVMCommand<T> getVMCommand, IManipulateViewCmd cmd)
    {
        this(type, getVMCommand);
        setCommand(cmd);
    }

    @Override
    protected void detectViewCommands(View view) {
        setWriteViewCommand(new IWriteViewCommand<T>() {
            @Override
            public void set(View view, T val) {
                //if (!isVMUpdatesView())
                _cmd.execute(view, val);
            }
        });
    }
}
