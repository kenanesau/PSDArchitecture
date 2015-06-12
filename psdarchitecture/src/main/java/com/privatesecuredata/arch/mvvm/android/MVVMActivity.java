package com.privatesecuredata.arch.mvvm.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersistanceManagerLocator;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DataHive;

public class MVVMActivity extends FragmentActivity {
	public static final String TAG_PERSISTANCE_MANAGER = "pm";
	private String pmUUID;
	
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
		super.onCreate(savedInstanceState);
		
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
	}
}
