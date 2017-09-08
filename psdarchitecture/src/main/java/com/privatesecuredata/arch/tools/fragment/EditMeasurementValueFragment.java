package com.privatesecuredata.arch.tools.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;

/**
 * Created by kenan on 9/8/17.
 */

public class EditMeasurementValueFragment extends MVVMFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState !=null)
        {
            //Restore VM from activity
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.psdarch_edit_measurement_value_fragment, container, false);
    }
}
