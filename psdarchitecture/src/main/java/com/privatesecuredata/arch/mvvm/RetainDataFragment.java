package com.privatesecuredata.arch.mvvm;

import android.support.v4.app.Fragment;
import android.os.Bundle;

public class RetainDataFragment extends Fragment {
    // data object we want to retain
    private String uuid;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void saveData(Object data) {
        uuid = DataHive.getInstance().put(data);
    }

    public Object loadData() {
    	if (uuid!=null)
    		return DataHive.getInstance().get(uuid);
    	else
    		return null;
    }
}
