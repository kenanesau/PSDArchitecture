package com.privatesecuredata.arch.ui.widget;

import android.os.Bundle;
import android.renderscript.Sampler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;

/**
 * Created by kenan on 9/15/17.
 */

public class EditMeasurementValueFragment extends MVVMFragment
    implements TabContentFactory {
    public static final String TAG_SPEC_PARAMS = "psdarch_spec_params";
    public static final String TAG = "psdarch_tag_edit_measurement_value_frag";

    public static EditMeasurementValueFragment newInstance(MeasurementValue.ValueSpec ... specs) {

        Bundle params = new Bundle();
        params.putParcelableArray(TAG_SPEC_PARAMS, specs);

        EditMeasurementValueFragment f = new EditMeasurementValueFragment();
        f.setArguments(params);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.psdarch_edit_measurement_value_fragment, container, false);
        TabHost host = (TabHost)view.findViewById(R.id.edit_measval_tabhost);
        host.setup();

        Bundle args = getArguments();
        MeasurementValue.ValueSpec[] specs = (MeasurementValue.ValueSpec[])args.getParcelableArray(TAG_SPEC_PARAMS);

        if (specs.length > 1) {
            for (MeasurementValue.ValueSpec spec : specs) {
                TabHost.TabSpec tab = host.newTabSpec(spec.getSys().toString());

                tab.setIndicator(spec.getSys().toString());
                tab.setContent(this);
                host.addTab(tab);
            }
        }
        else {
            FrameLayout frame = (FrameLayout)view.findViewById(R.id.psdarch_no_tabs_frame);
            host.setVisibility(View.GONE);
            frame.addView(createTabContent(specs[0].getSys().toString()));
            frame.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public View createTabContent(String tag) {
        TextView view = new TextView(getActivity());
        view.setText(tag);
        return view;
    }
}
