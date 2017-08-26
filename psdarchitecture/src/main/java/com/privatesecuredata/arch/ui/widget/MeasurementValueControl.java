package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.android.IBindableView;
import com.privatesecuredata.arch.mvvm.android.MVVMActivity;
import com.privatesecuredata.arch.mvvm.android.MVVMComplexVmAdapter;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.vm.MeasurementValueVM;

/**
 * Created by kenan on 8/7/17.
 */
public class MeasurementValueControl extends FrameLayout implements IBindableView<MeasurementValueVM>{
    private String _hint = "";
    private String _formatStr = "%.2f";
    private String _label = "Label";
    private String _unit_postfix = null;
    private boolean _hideLabel = false;
    private boolean _readonly = true;
    private MeasurementValueVM _value;
    private MVVMComplexVmAdapter<MeasurementValueVM> adapter;

    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    public void setReadOnly(boolean ro) {
        _readonly = ro;
        EditText valueEdit = (EditText) findViewById(R.id.psdarch_measvalctrl_edit_value);
        TextView valueLabel = (TextView) findViewById(R.id.psdarch_measvalctrl_lbl_value);
        valueEdit.setVisibility(_readonly ? GONE : VISIBLE);
        valueLabel.setVisibility(_readonly ? VISIBLE : GONE);
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr)
    {
        if ((attrs != null)) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_measvalctrl, 0, 0);

            //1 - Vertical, 0 - Horizontal
            int orientation = a.getInt(R.styleable.psdarch_number_selector_android_orientation, 1);
            if (0 == orientation)
                LayoutInflater.from(context).inflate(R.layout.psdarch_measurement_value_control, this, true);
            else
                LayoutInflater.from(context).inflate(R.layout.psdarch_measurement_value_control, this, true);

            _readonly = a.getBoolean(R.styleable.psdarch_measvalctrl_readonly, true);
            _hint = a.getString(R.styleable.psdarch_measvalctrl_android_hint);
            String formatStr = a.getString(R.styleable.psdarch_measvalctrl_format_string);
            _formatStr = formatStr != null ? formatStr : _formatStr;
            _label = a.getString(R.styleable.psdarch_measvalctrl_label);
            _hideLabel = a.getBoolean(R.styleable.psdarch_measvalctrl_hide_label, false);
            _unit_postfix = a.getString(R.styleable.psdarch_measvalctrl_unit_postfix);
            a.recycle();
        }

        TextView labelView = (TextView) findViewById(R.id.psdarch_measvalctrl_label);
        labelView.setText(_label);
        labelView.setVisibility(_hideLabel ? View.GONE : View.VISIBLE);

        EditText valueEdit = (EditText) findViewById(R.id.psdarch_measvalctrl_edit_value);
        TextView valueLabel = (TextView) findViewById(R.id.psdarch_measvalctrl_lbl_value);
        valueEdit.setHint(_hint);
        valueLabel.setHint(_hint);
        setReadOnly(_readonly);

        if (isInEditMode()) {
            valueLabel.setText("99,99");
        }
    }

    @Override
    public void bind(MeasurementValueVM vm) {
        if (null != adapter)
            adapter.dispose();

        _value = vm;

        if (null != _value) {
            adapter = new MVVMComplexVmAdapter<MeasurementValueVM>((MVVMActivity) getContext(), this, _value);

            vm.getStrValueVM().setFormatString(_formatStr);

            if (_unit_postfix != null) {
                vm.getUnitPostfixVM().set(_unit_postfix);
            }
            if (!_readonly)
                adapter.setMapping(String.class, R.id.psdarch_measvalctrl_edit_value,
                        new IGetVMCommand<String>() {
                            @Override
                            public SimpleValueVM<String> getVM(IViewModel<?> vm1) {
                                return vm.getStrValueVM();
                            }
                        });
            else
                adapter.setMapping(String.class, R.id.psdarch_measvalctrl_lbl_value,
                        new IGetVMCommand<String>() {
                            @Override
                            public SimpleValueVM<String> getVM(IViewModel<?> vm1) {
                                return vm.getStrValueVM();
                            }
                        });

            adapter.setMapping(String.class, R.id.psdarch_measvalctrl_lbl_unit,
                    new IGetVMCommand<String>() {
                        @Override
                        public SimpleValueVM<String> getVM(IViewModel<?> vm1) {
                            return vm.getUnitVM();
                        }
                    });

        }

    }

    @Override
    public void unbind() {
        if (null != adapter)
            adapter.dispose();

        this._value = null;
        this.adapter = null;
    }
}
