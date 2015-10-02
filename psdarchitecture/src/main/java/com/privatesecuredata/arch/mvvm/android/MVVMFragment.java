package com.privatesecuredata.arch.mvvm.android;

import android.app.Activity;
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

public class MVVMFragment extends Fragment {
    private final static String KEY_DEFAULT_PM_UUID = "PSDARCH_MVVMFRAGMENT_PM_UUID";
    private Activity attachedActivity;

	public PersistanceManager createPM(IDbDescription desc)
	{
		PersistanceManagerLocator.initializeDB(desc);
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
        getMVVMActivity().rememberInstanceState(vms);
    }

    protected void rememberInstanceState(String key, IViewModel vm) {
        getMVVMActivity().rememberInstanceState(key, vm);
    }

    protected void forgetInstanceState(IViewModel... vms) {
        getMVVMActivity().forgetInstanceState(vms);
    }

    protected void forgetInstanceState(String key, IViewModel vm) {
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
	public void onAttach(Activity activity) {
        this.attachedActivity = activity;
        Log.d(getClass().getSimpleName(), "onAttach");
		super.onAttach(activity);
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
