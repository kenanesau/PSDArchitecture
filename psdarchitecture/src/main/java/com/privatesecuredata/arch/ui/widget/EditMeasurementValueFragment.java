package com.privatesecuredata.arch.ui.widget;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.tools.unitconversion.AbstractMeasurementSystem;
import com.privatesecuredata.arch.tools.unitconversion.Conversion;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementSysFactory;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Created by kenan on 9/15/17.
 */

public class EditMeasurementValueFragment extends MVVMFragment
    implements TabContentFactory {
    public static final String TAG_SPEC_PARAMS = "psdarch_spec_params";
    public static final String TAG = "psdarch_tag_edit_measurement_value_frag";
    public static final String ARG_SELECTED_VALUE = "arg_selected_meas_val";
    public static final String ARG_SELECTED_TAB = "arg_selected_tab";
    public static final String ARG_SPECS = "arg_specs_parameter";
    private MeasurementValue selectedValue = null;
    private int selectedTab = 0;
    private MeasurementValue.ValueSpec[] specs = null;
    private HashMap<String, View> tabViews = new HashMap<>();
    private boolean noWatch = false;
    private boolean textChanging = false;
    private boolean viewStateRestored = false;

    private int[] unitOfAccounts10 = {
            250,
            500,
            1000,
            2000,
            3000,
            5000,
            10000
    };
    private int[] unitOfAccounts4 = {
            1,
            2,
            4,
            8,
            16,
            24,
            48
    };
    private int[] unitOfAccounts3 = {
            1,
            2,
            3,
            6,
            9,
    };


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

        if (null == savedInstanceState) {
            Bundle args = getArguments();
            Parcelable[] parcelables = args.getParcelableArray(TAG_SPEC_PARAMS);
            specs = Arrays.copyOf(parcelables, parcelables.length, MeasurementValue.ValueSpec[].class);
            if ( (null == specs) || (specs.length < 1) )
                throw new ArgumentException("Add at least one MeasurementValue-Spec!");
            selectedValue = new MeasurementValue(specs[0].getSys(), specs[0].getType(), specs[0].getUnit(), -1.0d);
            selectedTab = 0;
        }
        else {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(ARG_SPECS);
            specs = Arrays.copyOf(parcelables, parcelables.length, MeasurementValue.ValueSpec[].class);
            selectedValue = savedInstanceState.getParcelable(ARG_SELECTED_VALUE);
            //selectedTab = savedInstanceState.getInt(ARG_SELECTED_TAB);
        }

        if (specs.length > 1) {
            Integer i = 0;
            for (MeasurementValue.ValueSpec spec : specs) {
                TabHost.TabSpec tab = host.newTabSpec(i.toString());

                tab.setIndicator(spec.getSys().toString());
                tab.setContent(this);
                host.addTab(tab);
                i++;
            }
        }
        else {
            FrameLayout frame = (FrameLayout)view.findViewById(R.id.psdarch_no_tabs_frame);
            host.setVisibility(View.GONE);
            frame.addView(createTabContent(new Integer(specs[0].getSys().val()).toString()));
            frame.setVisibility(View.VISIBLE);
        }

        //host.setCurrentTab(selectedTab);

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                selectedTab = Integer.parseInt(tabId);
                selectedValue.setSys(selectedTab);
                AbstractMeasurementSystem system = MeasurementSysFactory.create(selectedValue.getSys(), selectedValue.getType());
                if (selectedValue.getUnitVal() > system.getUnits().length - 1)
                    selectedValue.setUnit(system.getUnits().length - 1);

                View v = tabViews.get(tabId);
                //RadioGroup rg = (RadioGroup)v.findViewById(R.id.unit_of_account_choices);
                //if (null != rg)
                //    createOptionList(rg, specs[selectedTab]);
                EditText editVal = (EditText)v.findViewById(R.id.psdarch_measval_value);
                noWatch = true;
                if (selectedValue.getVal() >= 0.0d)
                        editVal.setText(new Double(selectedValue.getVal()).toString());
                noWatch=false;

                Spinner spinner = (Spinner)v.findViewById(R.id.psdarch_unit_spinner);
                if (null != spinner)
                    spinner.setSelection(selectedValue.getUnitVal());

            }
        });

        return view;
    }

    protected void createOptionList(final RadioGroup rg, MeasurementValue.ValueSpec spec) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rg.removeAllViews();

                AppCompatRadioButton rb = new AppCompatRadioButton(getActivity());
                rb.setText(getResources().getString(R.string.psdarch_custom_weight));
                rb.setId(R.id.custom_weight);
                rb.setChecked(true);
                rg.addView(rb);

                rb = new AppCompatRadioButton(getActivity());
                rb.setText(getResources().getString(R.string.psdarch_unspecified_weight));
                rb.setId(R.id.unspecified_weight);
                if (selectedValue.getVal() < 0.0d) {
                    rb.setChecked(true);
                }
                rg.addView(rb);

                int[] unitsOfAccount = { };
                if (selectedValue.getUnit().getFactorNextEnumerator() % 10 == 0) {
                    unitsOfAccount = unitOfAccounts10;
                }
                else if (selectedValue.getUnit().getFactorNextEnumerator() % 4 == 0) {
                    unitsOfAccount = unitOfAccounts4;
                }
                else if (selectedValue.getUnit().getFactorNextEnumerator() % 2 == 0) {
                    unitsOfAccount = unitOfAccounts4;
                }
                else if (selectedValue.getUnit().getFactorNextEnumerator() % 3 == 0) {
                    unitsOfAccount = unitOfAccounts3;
                }

                String appendix = getResources().getString(R.string.psdarch_per_piece);
                if (selectedValue.getType() == MeasurementSysFactory.Type.LIQUIDVOLUME) {
                    appendix = getResources().getString(R.string.psdarch_per_bin);
                }
                int i=0;
                for(int val : unitsOfAccount)
                {
                    rb = new AppCompatRadioButton(getActivity());

                    rb.setText(String.format("%d %s %s", val,
                            selectedValue.getUnit().getUnit(),
                            appendix));

                    rb.setTag(val);
                    rb.setId(i++);
                    rg.addView(rb);

                    if (val == (int)selectedValue.getVal()) {
                        rb.setChecked(true);
                    }
                }


            }
        });

    }

    @Override
    public View createTabContent(final String tag) {
        int activeTab = Integer.parseInt(tag);
        View view = getActivity().getLayoutInflater().inflate(R.layout.psdarch_dialog_choose_unit_of_account, null);

        RadioGroup rg = (RadioGroup)view.findViewById(R.id.unit_of_account_choices);
        MeasurementValue.ValueSpec spec = specs[activeTab];
        AbstractMeasurementSystem system = MeasurementSysFactory.create(spec.getSys(), spec.getType());

        List<String> units = new ArrayList<>();
        for (Conversion conv : system.getUnits()) {
            units.add(conv.getUnit());
        }
        Spinner spinner = (Spinner)view.findViewById(R.id.psdarch_unit_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(activeTab == selectedTab ? selectedValue.getUnitVal() : spec.getUnit());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedValue.setUnit(position);

                TabHost host = (TabHost)getActivity().findViewById(R.id.edit_measval_tabhost);
                View v = host.getCurrentTabTag() != null ? tabViews.get(host.getCurrentTabTag()) :
                        tabViews.get("0");
                RadioGroup rg = (RadioGroup)v.findViewById(R.id.unit_of_account_choices);
                createOptionList(rg, spec);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        createOptionList(rg, spec);

        EditText editVal = (EditText)view.findViewById(R.id.psdarch_measval_value);
        if (activeTab == selectedTab) {
            if (selectedValue.getVal() < 0.0d) {
                editVal.setText("0.0");
                editVal.setEnabled(false);
            } else {
                editVal.setText(new Double(selectedValue.getVal()).toString());
            }
        }
        editVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noWatch)
                    return;

                String str = s.toString();
                TabHost host = (TabHost) getActivity().findViewById(R.id.edit_measval_tabhost);
                View v = host.getCurrentTabTag() != null ? tabViews.get(host.getCurrentTabTag()) :
                        tabViews.get("0");
                RadioGroup rg = (RadioGroup) v.findViewById(R.id.unit_of_account_choices);

                if (str.isEmpty()) {
                    selectedValue.setVal(-1.0d);
                    /*RadioButton rb = (RadioButton) rg.findViewById(R.id.unspecified_weight);
                    textChanging = true;
                    rb.setChecked(true);
                    textChanging = false;
                    EditText txt = (EditText)v.findViewById(R.id.psdarch_measval_value);
                    txt.setEnabled(false);*/
                }
                else {
                    selectedValue.setVal(Double.parseDouble(s.toString()));
                    RadioButton rb = (RadioButton) rg.findViewById(R.id.custom_weight);
                    textChanging = true;
                    rb.setChecked(true);
                    textChanging = false;
                }
            }
        });

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if ( (!viewStateRestored) || (textChanging) )
                    return;

                AppCompatRadioButton rb = (AppCompatRadioButton)group.findViewById(checkedId);
                if (rb==null)
                    return;

                TabHost host = (TabHost)getActivity().findViewById(R.id.edit_measval_tabhost);
                View v = host.getCurrentTabTag() != null ? tabViews.get(host.getCurrentTabTag()) :
                        tabViews.get("0");
                EditText editVal = (EditText)v.findViewById(R.id.psdarch_measval_value);
                if (checkedId == R.id.unspecified_weight) {
                    selectedValue.setVal(-1.0);
                    editVal.setEnabled(false);
                }
                else {

                    if ( (checkedId == R.id.custom_weight) && (selectedValue.getVal() < 0.0) ) {
                        Editable s = editVal.getText();
                        String str = s.toString();
                        if (str.isEmpty())
                            selectedValue.setVal(0.0);
                        else
                            selectedValue.setVal(Double.parseDouble(str));
                    }
                    else {
                        if (rb.getTag() != null)
                            selectedValue.setVal((int) (rb.getTag()));
                    }
                    editVal.setEnabled(true);
                    noWatch = true;
                    editVal.setText(new Double(selectedValue.getVal()).toString());
                    noWatch = false;
                }
            }
        });

        if (tabViews.containsKey(tag))
            tabViews.remove(tag);
        tabViews.put(tag, view);
        return view;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        viewStateRestored = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArray(ARG_SPECS, specs);
        outState.putParcelable(ARG_SELECTED_VALUE, selectedValue);
        outState.putInt(ARG_SELECTED_TAB, selectedTab);
    }
}
