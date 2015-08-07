package com.privatesecuredata.arch.mvvm.android;

import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersistanceManagerLocator;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DataHive;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import android.app.Activity;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MVVMFragment extends Fragment {
    private final static String KEY_DEFAULT_PM_UUID = "PSDARCH_MVVMFRAGMENT_PM_UUID";
    private String pmUUID;
    private MVVMInstanceStateHandler instanceStateHandler = new MVVMInstanceStateHandler();
	
	public PersistanceManager createPM(IDbDescription desc)
	{
		PersistanceManagerLocator.initializeDB(desc);
		PersistanceManagerLocator pmLoc = PersistanceManagerLocator.getInstance();
		return pmLoc.getPersistanceManager(getActivity(), desc); 
	}
	
	public void setDefaultPM(String uuid)
	{
		PersistanceManager pm = (PersistanceManager) DataHive.getInstance().get(uuid);
		
		if (null == pm)
			throw new ArgumentException(String.format("Did not find any PersistanceManager wit UUID=%s", uuid));
		
		if ( (null != pmUUID) && (!uuid.equals(pmUUID)) ) {
			DataHive.getInstance().remove(pmUUID); 
		}
		
		this.pmUUID = uuid;
	}
	
	/**
	 * This function is used by the main activity which also set the default PM
	 * 
	 * @return The current Default-PersistanceManager
	 */
	public PersistanceManager getDefaultPM()
	{
		if (null == pmUUID)
			throw new ArgumentException("No default PersistanceManager set yet!");
					
		return (PersistanceManager)DataHive.getInstance().get(pmUUID);
	}
	
	public String getPMUUID() { return pmUUID; }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);

        Intent intent = getActivity().getIntent();

        if (null != intent)
		{
            Bundle bundle = intent.getExtras();
            if (null != bundle) {
                String pmUUID = bundle.getString(MVVMActivity.TAG_PERSISTANCE_MANAGER);
                if (null != pmUUID) {
                    setDefaultPM(pmUUID);
                }
            }
        }

        if (savedInstanceState != null) {
            instanceStateHandler.onRestoreInstanceState(savedInstanceState);
            //setDefaultPM(savedInstanceState.getString(KEY_DEFAULT_PM_UUID));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected void doViewToVMMapping() {}

    protected void rememberInstanceState(IViewModel... vms) {
        instanceStateHandler.rememberInstanceState(vms);
    }

    protected void forgetInstanceState(IViewModel... vms) {
        instanceStateHandler.forgetInstanceState(vms);
    }

    public <T extends IViewModel> T getViewModel(Class type)
    {
        return instanceStateHandler.getViewModel(getDefaultPM(), type);
    }

    public <T extends IViewModel> T getViewModel(String tag)
    {
        return instanceStateHandler.getViewModel(getDefaultPM(), tag);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(getClass().getSimpleName(), "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        doViewToVMMapping();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(getClass().getSimpleName(), "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        instanceStateHandler.onSaveInstanceState(outState, null);
        //outState.putString(KEY_DEFAULT_PM_UUID, getPMUUID());
    }

	@Override
	public void onAttach(Activity activity) {
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
