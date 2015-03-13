package com.privatesecuredata.arch.mvvm.vm;

/**
 * Created by kenan on 3/13/15.
 */
public interface IWidgetValueReceiver {
    void notifyWidgetChanged(IWidgetValueAccessor accessor);
}
