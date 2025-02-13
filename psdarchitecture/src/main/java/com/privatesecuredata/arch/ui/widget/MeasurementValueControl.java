package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.android.IBindableView;
import com.privatesecuredata.arch.mvvm.android.MVVMActivity;
import com.privatesecuredata.arch.mvvm.android.MVVMComplexVmAdapter;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementSysFactory;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;
import com.privatesecuredata.arch.tools.vm.MeasurementValueVM;

/**
 * Created by kenan on 8/7/17.
 */
public class MeasurementValueControl extends FrameLayout implements IBindableView<MeasurementValueVM>,
        EditMeasurementValueFragment.IMeasurementValueListener
{
    public enum EditMode {
        READONLY(0), /* just ro */
        SIMPLE(1),   /* Enable EditText */
        COMPLEX(2),  /* Not implemented yet -- SIMPLE + add a spinner to change the unit */
        FULL(3);     /* Open EditMeasurementValueFragment as Dialog */

        private int value;

        private EditMode(int value) {
            this.value = value;
        }
    }

    private String _hint = "";
    private String _formatStr = "%.2f";
    private String _label = "Label";
    private String _unitPostfix = null;
    private boolean _hideLabel = false;
    private EditMode _editMode = EditMode.READONLY;
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

    public void setEditMode(EditMode editMode) {
        _editMode = editMode;
        EditText valueEdit = (EditText) findViewById(R.id.psdarch_measvalctrl_edit_value);
        TextView valueLabel = (TextView) findViewById(R.id.psdarch_measvalctrl_lbl_value);

        switch (_editMode) {
            case READONLY:
                valueEdit.setVisibility(GONE);
                valueLabel.setVisibility(VISIBLE);
                break;
            case SIMPLE:
                valueEdit.setVisibility(VISIBLE);
                valueLabel.setVisibility(GONE);
                break;
            case COMPLEX:
                throw new ArgumentException("Not implemented yet!");
            case FULL:
                valueEdit.setVisibility(GONE);
                valueLabel.setVisibility(VISIBLE);
                this.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MeasurementValue.ValueSpec[] specs = MeasurementSysFactory.createSpecs(_value.getType());

                        EditMeasurementValueFragment frag = EditMeasurementValueFragment.newInstance(_value.get(), specs);
                        frag.setValueListener(MeasurementValueControl.this);
                        frag.show(((AppCompatActivity)getContext()).getSupportFragmentManager(), EditMeasurementValueFragment.TAG);
                    }
                });
                break;
        }
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

            _editMode = EditMode.values()[(a.getInt(R.styleable.psdarch_measvalctrl_edit_mode, 0))];
            _hint = a.getString(R.styleable.psdarch_measvalctrl_android_hint);
            String formatStr = a.getString(R.styleable.psdarch_measvalctrl_format_string);
            _formatStr = formatStr != null ? formatStr : _formatStr;
            _label = a.getString(R.styleable.psdarch_measvalctrl_label);
            _hideLabel = a.getBoolean(R.styleable.psdarch_measvalctrl_hide_label, false);
            _unitPostfix = a.getString(R.styleable.psdarch_measvalctrl_unit_postfix);
            a.recycle();
        }

        TextView labelView = (TextView) findViewById(R.id.psdarch_measvalctrl_label);
        labelView.setText(_label);
        labelView.setVisibility(_hideLabel ? View.GONE : View.VISIBLE);

        EditText valueEdit = (EditText) findViewById(R.id.psdarch_measvalctrl_edit_value);
        TextView valueLabel = (TextView) findViewById(R.id.psdarch_measvalctrl_lbl_value);
        valueEdit.setHint(_hint);
        valueLabel.setHint(_hint);
        setEditMode(_editMode);

        if (isInEditMode()) {
            valueLabel.setText(String.format(_formatStr, 99.99d));
        }
    }

    public void setFormatString(String str) {
        _formatStr = str;
        _value.getFormatStringVM().set(str);
        invalidate();
    }

    public void setUnitPostfix(String str) {
        _unitPostfix = str;
        _value.getUnitPostfixVM().set(str);
        invalidate();
    }

    @Override
    public void bind(MeasurementValueVM vm) {
        if (null != adapter)
            adapter.dispose();

        _value = vm;

        if (null != _value) {
            adapter = new MVVMComplexVmAdapter<MeasurementValueVM>((MVVMActivity) getContext(), this, _value);

            if (_formatStr != null)
                _value.getFormatStringVM().set(_formatStr);
            if (_unitPostfix != null)
                vm.getUnitPostfixVM().set(_unitPostfix);

            if (_editMode==EditMode.SIMPLE)
                adapter.setMapping(String.class, R.id.psdarch_measvalctrl_edit_value,
                        new IGetVMCommand<String>() {
                            @Override
                            public SimpleValueVM<String> getVM(IViewModel<?> vm1) {
                                return vm.getStrValueVM();
                            }
                        });
            else if ( (_editMode==EditMode.READONLY) || (_editMode == EditMode.FULL) ) {
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

    }

    @Override
    public void commitValue(MeasurementValue value) {
        this._value.set(value);
    }

    @Override
    public void unbind() {
        if (null != adapter)
            adapter.dispose();

        this._value = null;
        this.adapter = null;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        AppCompatActivity activity = (AppCompatActivity)getContext();
        EditMeasurementValueFragment frag = (EditMeasurementValueFragment)activity.getSupportFragmentManager().findFragmentByTag(EditMeasurementValueFragment.TAG);
        if (null != frag)
            frag.setValueListener(this);
    }
}
