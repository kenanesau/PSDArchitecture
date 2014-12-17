package com.privatesecuredata.arch.mvvm.android;

import com.privatesecuredata.arch.db.IDbDescription;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.PersistanceManagerLocator;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DataHive;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

public class MVVMFragment extends Fragment {
    private String pmUUID;
	
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
		super.onCreate(savedInstanceState);
		
		Intent intent = getActivity().getIntent();
		
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
