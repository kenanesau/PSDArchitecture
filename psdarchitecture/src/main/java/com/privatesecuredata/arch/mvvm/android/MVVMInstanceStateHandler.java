package com.privatesecuredata.arch.mvvm.android;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Objects of this class help in saving the state of ViewModels which store
 * IPersistable models. Objects of this type are used either by Fragments or
 * Activities to store/restore the state of their ViewModels
 */
public class MVVMInstanceStateHandler {

    public interface IInstanceStateHandler {
        void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState);
        <T extends IViewModel> T getViewModel(Class type);
        <T extends IViewModel> T getViewModel(String tag);
    }

    private static final String KEY_REMEMBERED_INSTANCES = "mvvm_key_remembered_instances";
    Dictionary<String, ViewModelState> rememberedInstances = new Hashtable<String, ViewModelState>();

    public void rememberInstanceState(IViewModel... vms) {
        for(IViewModel vm : vms) {
            if (vm.getModel() instanceof IPersistable) {
                ViewModelState state = new ViewModelState(vm);
                rememberedInstances.put(state.getKey(), state);
            }
            throw new ArgumentException("Viewmodel contains a model which is not an instance of IPersistable!!!");
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {

        String keyList = savedInstanceState.getString(KEY_REMEMBERED_INSTANCES);
        String[] keys = (keyList != null) ? keyList.split(";") : null;

        if (null != keys) {
            for (String key : keys) {
                ViewModelState state = savedInstanceState.getParcelable(key);

                rememberedInstances.put(state.getKey(), state);
            }
        }
    }

    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        StringBuilder keyList = new StringBuilder();

        if (rememberedInstances.size() > 0) {
            Enumeration<ViewModelState> states = rememberedInstances.elements();
            while (states.hasMoreElements()) {
                ViewModelState state = states.nextElement();
                outState.putParcelable(state.getKey(), state);

                keyList.append(state.getKey())
                        .append(";");
            }

            outState.putString(KEY_REMEMBERED_INSTANCES, keyList.toString());
        }
    }

    public <T extends IViewModel> T getViewModel(PersistanceManager pm, Class type)
    {
        return getViewModel(pm, type.getCanonicalName());
    }

    public <T extends IViewModel> T getViewModel(PersistanceManager pm, String tag)
    {
        ViewModelState state = rememberedInstances.get(tag);

        if (state!=null) {
            return state.getVM(pm);
        }
        else {
            return null;
        }
    }
}
