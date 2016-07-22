package com.privatesecuredata.arch.mvvm;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModelFactory;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.ListViewModelFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by kenan on 12/21/14.
 */
public class MVVM {
    private static SparseArray<Object> cfgObjs = new SparseArray<>();
    private static SparseArray<MVVM> mvvmInsts = new SparseArray<MVVM>();
    private HashMap<Class, ComplexViewModel.VmFactory> vmProviders = new HashMap<>();

    private Resources res;
    private Context ctx;

    private static int instCnt = -1;
    private int _handle;

    private IListViewModelFactory listVmFactory = new ListViewModelFactory();
    private Collection<IViewModelCommitListener> globalCommitListeners = new ArrayList<IViewModelCommitListener>();


    private MVVM(Object cfg) {
        instCnt++;
        setHandle(instCnt);
        this.mvvmInsts.put(instCnt, this);
        if (null!=cfg)
            this.cfgObjs.put(instCnt, cfg);

    }

    public static MVVM createMVVM() {
        return createMVVM(null);
    }


    /**
     * This method returns an MVVM-instance. If there was already an MVVM-instance registered
     * for the cfgObj this instance is returned, otherwise a new one is created.
     *
     * ATTENTION: Usually you want to use PersistanceManager.createMVVM() if you do use this
     * function instead you have to do some extra things...
     *
     * @param cfgObj Configuration object (e.g. PersitanceManager)
     * @return
     */
    public static MVVM createMVVM(Object cfgObj)
    {
        MVVM mvvm = getMVVM(cfgObj);
        if (null == mvvm)
            mvvm = new MVVM(cfgObj);

        return mvvm;
    }


    public static MVVM getMVVM(ComplexViewModel vm)
    {
        return mvvmInsts.get(vm.getHandle(), null);
    }


    public static MVVM getMVVM(Object cfgObj)
    {
        int idx = cfgObjs.indexOfValue(cfgObj);
        if (idx >= 0) {
            int key = cfgObjs.keyAt(idx);
            return mvvmInsts.get(key, null);
        }
        else
            return null;
    }

    public static <T> T getCfgObj(MVVM mvvm)
    {
        return (T)cfgObjs.get(mvvm.getHandle(), null);
    }

    public int getHandle() {return _handle; }
    private void setHandle(int handle) { _handle = handle; }

    public <V extends ComplexViewModel<?>, M> V createVM (M model) throws ArgumentException
    {
        V vm = null;

        Class<?> modelType = model.getClass();
        vm = createVM(modelType, model);

        return vm;
    }

    public <T extends ComplexViewModel, M> T createVM(Class modelType, M model)
    {
        T ret = null;
        ComplexViewModel.VmFactory provider = vmProviders.get(modelType);
        if (null != provider) {
            ret = (T) provider.create(this, model);
        } else {
            try {
                ComplexVmMapping anno = (ComplexVmMapping) modelType.getAnnotation(ComplexVmMapping.class);
                if (null == anno)
                    throw new ArgumentException(
                            String.format("Type \"%s\" does not have a ComplexVmMapping-Annotation. Please provide one!", modelType.getName()));
                Class<?> vmType = anno.vmType();
                Constructor constructor = vmType.getConstructor(MVVM.class, modelType);
                ret = (T) constructor.newInstance(this, model);
            }
            catch (IllegalArgumentException ex) {
                throw new ArgumentException("Wrong argument!", ex);
            }
            catch (IllegalAccessException ex) {
                throw new ArgumentException("Error accessing method!", ex);
            }
            catch (InvocationTargetException ex) {
                throw new ArgumentException("Error invoking constructor!", ex);
            }
            catch (NoSuchMethodException ex) {
                throw new ArgumentException("Could not find constructor", ex);
            }
            catch (InstantiationException ex) {
                throw new ArgumentException("Error instantiating complex viewmodel", ex);
            }
        }
        ret.setHandle(this.getHandle());

        return ret;
    }

    public void setListViewModelFactory(IListViewModelFactory lstFactory) { this.listVmFactory = lstFactory; }
    public IListViewModelFactory getListViewModelFactory() { return this.listVmFactory; }

    public void notifyCommit(IViewModel<?> vm) {
        for(IViewModelCommitListener listener : globalCommitListeners)
            listener.notifyCommit(vm);
    }

    public void addGlobalCommitListener(IViewModelCommitListener listener)
    {
        globalCommitListeners.add(listener);
    }

    public void delGlobalCommitListener(IViewModelCommitListener listener)
    {
        globalCommitListeners.remove(listener);
    }

    public void setResources(Resources res) { this.res = res; }
    public Resources getResources() { return res; }
    public void setContext(Context ctx) {
        this.ctx = ctx;
        this.res = ctx.getResources();
    }
    public Context getContext() { return ctx; }

    public void registerVmProvider(Class modelType, Class providerType) throws IllegalAccessException, InstantiationException {
        if (!Object.class.equals(providerType)) {
            ComplexViewModel.VmFactory provider = (ComplexViewModel.VmFactory)providerType.newInstance();
            vmProviders.put(modelType, provider);
        }
    }

}
