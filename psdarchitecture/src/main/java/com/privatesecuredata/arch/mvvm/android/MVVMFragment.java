package com.privatesecuredata.arch.mvvm.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersistanceManagerLocator;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.util.HashMap;

public class MVVMFragment extends Fragment {
    private final static String KEY_DEFAULT_PM_UUID = "PSDARCH_MVVMFRAGMENT_PM_UUID";
    private Context attachedActivity;
    private IViewModel[] rememberedInstanceStateChache;
    private HashMap<String, IViewModel> rememberedInstanceStateDictCache;

	public PersistanceManager createPM(IDbDescription desc)
	{
		PersistanceManagerLocator.initializePM(desc);
		PersistanceManagerLocator pmLoc = PersistanceManagerLocator.getInstance();
		return pmLoc.getPersistanceManager(getActivity(), desc); 
	}

	/**
	 * This function is used by the main activity which also set the default PM
	 * 
	 * @return The current Default-PersistanceManager
	 */
	public PersistanceManager getDefaultPM()
	{
		return getMVVMActivity().getDefaultPM();
	}
	
	public String getPMUUID() { return getMVVMActivity().getPMUUID(); }

    public MVVMActivity getMVVMActivity() { return (MVVMActivity)attachedActivity; }
    protected void setMVVMActivity(MVVMActivity activity) { attachedActivity = activity; }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected void doViewToVMMapping() {}

    protected void rememberInstanceState(IViewModel... vms) {
        MVVMActivity act = getMVVMActivity();

        if (act != null)
            act.rememberInstanceState(vms);
        else
        {
            rememberedInstanceStateChache = vms;
        }
    }

    protected void rememberInstanceState(String key, IViewModel vm) {
        MVVMActivity act = getMVVMActivity();

        if (null != act)
            act.rememberInstanceState(key, vm);
        else {
            if (rememberedInstanceStateDictCache == null)
                rememberedInstanceStateDictCache = new HashMap<>();
            rememberedInstanceStateDictCache.put(key, vm);
        }
    }

    @Override
    public void onAttach(Context context) {
        this.attachedActivity = context;
        Log.d(getClass().getSimpleName(), "onAttach");

        if (rememberedInstanceStateChache != null) {
            getMVVMActivity().rememberInstanceState(rememberedInstanceStateChache);
            rememberedInstanceStateChache = null;
        }
        if (rememberedInstanceStateDictCache != null) {
            for(String key : rememberedInstanceStateDictCache.keySet())
            {
            getMVVMActivity().rememberInstanceState(key, rememberedInstanceStateDictCache.get(key));
            }
            rememberedInstanceStateDictCache = null;
        }
        super.onAttach(context);
    }

    protected void forgetInstanceState(IViewModel... vms) {
        if (getMVVMActivity() != null)
            getMVVMActivity().forgetInstanceState(vms);
    }

    protected void forgetInstanceState(String key, IViewModel vm) {
        if (getMVVMActivity() != null)
            getMVVMActivity().forgetInstanceState(key, vm);
    }

    public <T extends IPersistable> T getModel(Class type) {
        return getMVVMActivity().getModel(type);
    }

    public <T extends IPersistable> T getModel(String tag) {
        return getMVVMActivity().getModel(tag);
    }

    public <T extends IViewModel> T getViewModel(Class type)
    {
        return getMVVMActivity().getViewModel(type);
    }

    public <T extends IViewModel> T getViewModel(String tag)
    {
        return getMVVMActivity().getViewModel(tag);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(getClass().getSimpleName(), "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(getClass().getSimpleName(), "saveInstanceState");
        super.onSaveInstanceState(outState);
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(getClass().getSimpleName(), "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDestroy() {
        Log.d(getClass().getSimpleName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDetach() {
        Log.d(getClass().getSimpleName(), "onDetach");
		super.onDetach();
	}

	@Override
	public void onPause() {
        Log.d(getClass().getSimpleName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
        Log.d(getClass().getSimpleName(), "onResume");
        super.onResume();
        doViewToVMMapping();
	}

	@Override
	public void onStart() {
        Log.d(getClass().getSimpleName(), "onStart");
		super.onStart();
	}

	@Override
	public void onStop() {
        Log.d(getClass().getSimpleName(), "onStop");
		super.onStop();
	}
}
