package com.privatesecuredata.arch.mvvm.android;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

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
    Hashtable<String, ViewModelState> rememberedInstances = new Hashtable<String, ViewModelState>();

    Hashtable<String, Object> modelCache = new Hashtable<>();
    Hashtable<String, IViewModel> viewModelCache = new Hashtable<>();

    protected void rememberInstanceState(ViewModelState state, IViewModel vm)
    {
        String key = state.getKey();
        rememberedInstances.put(key, state);

        if (modelCache.containsKey(key)) {
            modelCache.put(key, vm.getModel());
            viewModelCache.put(key, vm);
        }
    }

    public void rememberInstanceState(IViewModel... vms) {
        for(IViewModel vm : vms) {
            if (vm == null)
                continue;

            if ( vm.getModel() instanceof IPersistable) {
                ViewModelState state = new ViewModelState(vm);
                rememberInstanceState(state, vm);
            }
            else
                throw new ArgumentException("Cannot remember instance state. Viewmodel contains a model which is not an instance of IPersistable!!!");
        }
    }

    public void rememberInstanceState(String key, IViewModel vm)
    {
        ViewModelState state = new ViewModelState(key, vm);
        rememberInstanceState(state, vm);
    }

    protected void forgetInstanceState(ViewModelState state, IViewModel vm)
    {
        String key = state.getKey();
        rememberedInstances.remove(key);

        if (modelCache.containsKey(key)) {
            modelCache.remove(key);
            viewModelCache.remove(key);
        }
    }

    public void forgetInstanceState(String key, IViewModel vm)
    {
        if (vm.getModel() instanceof IPersistable) {
            ViewModelState state = new ViewModelState(key, vm);
            forgetInstanceState(state, vm);
        }
    }

    public void forgetInstanceState(IViewModel... vms) {
        for(IViewModel vm : vms) {
            if (null == vm)
                continue;

            if (vm.getModel() instanceof IPersistable) {
                ViewModelState state = new ViewModelState(vm);
                forgetInstanceState(state, vm);
            }
            else
                throw new ArgumentException("Cannot forget instance state. Viewmodel contains a model which is not an instance of IPersistable!!!");
        }
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        String keyList = savedInstanceState.getString(KEY_REMEMBERED_INSTANCES);
        String[] keys = (keyList != null) ? keyList.split(";") : null;

        if (null != keys) {
            Log.d(getClass().getSimpleName(), String.format("restoring Instance State for %s", keys));
            for (String key : keys) {
                ViewModelState state = savedInstanceState.getParcelable(key);

                rememberedInstances.put(state.getKey(), state);
            }
        }
    }

    public void saveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
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
            Log.d(getClass().getSimpleName(), String.format("saved Instance State for %s", keyList.toString()));
        }
    }

    public synchronized <T extends IPersistable> T getModel(PersistanceManager pm, Class type)
    {
        return getModel(pm, type.getCanonicalName());
    }

    public synchronized <T extends IPersistable> T getModel(PersistanceManager pm, String tag)
    {
        ViewModelState state = rememberedInstances.get(tag);

        if (modelCache.containsKey(tag))
            return (T) modelCache.get(tag);

        if (state != null) {
            T model = state.getModel(pm);
            modelCache.put(tag, model);
            return model;
        }
        else {
            return null;
        }
    }

    public synchronized <T extends IViewModel> T getViewModel(PersistanceManager pm, Class type)
    {
        return getViewModel(pm, type.getCanonicalName());
    }

    public synchronized <T extends IViewModel> T getViewModel(PersistanceManager pm, String tag)
    {

        if (viewModelCache.containsKey(tag)) {
            return (T) viewModelCache.get(tag);
        }

        ViewModelState state = rememberedInstances.get(tag);
        if (state != null) {
            T viewModel =  state.getVM(pm);
            modelCache.put(tag, viewModel.getModel());
            viewModelCache.put(tag, viewModel);
            return viewModel;
        }
        else {
            return null;
        }
    }
}
