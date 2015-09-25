package com.privatesecuredata.arch.mvvm.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersistanceManagerLocator;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DataHive;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity which supports easier restoration and saving of instance state. It also provides
 * access to the Default-Persistance-Manager
 */
public class MVVMActivity extends FragmentActivity
    implements MVVMInstanceStateHandler.IInstanceStateHandler {
	public static final String TAG_PERSISTANCE_MANAGER = "mvvm_pm";
    private String pmUUID;
    private MVVMInstanceStateHandler instanceStateHandler = new MVVMInstanceStateHandler();
    private boolean isResumed = false;
    private List<MVVMComplexVmAdapter> adapters = new ArrayList<>();
	
	public PersistanceManager createPM(IDbDescription desc)
	{
		PersistanceManagerLocator.initializeDB(desc);
		PersistanceManagerLocator pmLoc = PersistanceManagerLocator.getInstance();
		return pmLoc.getPersistanceManager(this, desc);
	}
	
	public void setDefaultPM(PersistanceManager pm)
	{
		if (null != pmUUID)
			DataHive.getInstance().remove(pmUUID);
		pmUUID = DataHive.getInstance().put(pm);
		getIntent().putExtra(MVVMActivity.TAG_PERSISTANCE_MANAGER, pmUUID);
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
	 * This function is used by an activity to get the Default-Persistance-Manager
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
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(getClass().getSimpleName(), "onCreate");

		Intent intent = getIntent();
		
		if (null != intent)
		{
			Bundle bundle = intent.getExtras();
			if (null != bundle) {
				String pmUUID = bundle.getString(MVVMActivity.TAG_PERSISTANCE_MANAGER);
				if (null != pmUUID)
					setDefaultPM(pmUUID);
			}
		}

        if (savedInstanceState != null) {
            instanceStateHandler.restoreInstanceState(savedInstanceState);


        }

        super.onCreate(savedInstanceState);
	}

	@Override
	protected void onRestart() {
		Log.d(getClass().getSimpleName(), "onRestart");
		super.onRestart();
	}

    protected void rememberInstanceState(IViewModel... vms) {
        instanceStateHandler.rememberInstanceState(vms);
    }

    protected void rememberInstanceState(String key, IViewModel vm) {
        instanceStateHandler.rememberInstanceState(key, vm);
    }

    protected void forgetInstanceState(IViewModel... vms) {
        instanceStateHandler.forgetInstanceState(vms);
    }

    protected void forgetInstanceState(String key, IViewModel vm) {
        instanceStateHandler.forgetInstanceState(key, vm);
    }

    public <T extends IPersistable> T getModel(Class type) {
        return instanceStateHandler.getModel(getDefaultPM(), type);
    }

    public <T extends IPersistable> T  getModel(String tag) {
        return instanceStateHandler.getModel(getDefaultPM(), tag);
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
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(getClass().getSimpleName(), "restoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);

        instanceStateHandler.restoreInstanceState(savedInstanceState);
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(getClass().getSimpleName(), "saveInstanceState");
        super.onSaveInstanceState(outState);

        instanceStateHandler.saveInstanceState(outState, null);
    }

    @Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		Log.d(getClass().getSimpleName(), "saveInstanceState");
        super.onSaveInstanceState(outState, outPersistentState);

        instanceStateHandler.saveInstanceState(outState, outPersistentState);
	}

   	@Override
	public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
		Log.d(getClass().getSimpleName(), "onCreate");
		super.onCreate(savedInstanceState, persistentState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(getClass().getSimpleName(), "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		Log.d(getClass().getSimpleName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(getClass().getSimpleName(), "onPause");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(getClass().getSimpleName(), "onResume");
		super.onResume();
	}

	@Override
	protected void onResumeFragments() {
		Log.d(getClass().getSimpleName(), "onResumeFragments");
		super.onResumeFragments();
	}

	@Override
	protected void onStart() {
		Log.d(getClass().getSimpleName(), "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(getClass().getSimpleName(), "onStop");
        Log.d(getClass().getSimpleName(), "onStop");
        for(MVVMComplexVmAdapter adapter : adapters)
        {
            adapter.dispose();
        }
        adapters.clear();
		super.onStop();
	}

    /**
     * This is used to determine if the activity is resumed and the views value has to be set
     * into the VM (normally it is the other way 'round)
     *
     * @return Returns true if the activity is resumed and an instance-state is restored.
     * @see MVVMComplexVmAdapter#setModelMapping(Class, int, IGetVMCommand)
     */
    public boolean isResumedActivity() {
        return this.isResumed;
    }

    /**
     * The MVVMComplexVmAdapter register thmeselves, so the ViewToVM-Mappings which are
     * managed by the MVVMComplexVMAdapter can be disposed when the Activity stops...
     *
     * This has to be done to prevent doubled binding to a view.
     *
     * @param adapter
     */
    public void registerMVVMAdapter(MVVMComplexVmAdapter adapter)
    {
        this.adapters.add(adapter);
    }
}
