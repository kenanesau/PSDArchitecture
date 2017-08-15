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
import android.widget.FrameLayout;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.android.IBindableView;
import com.privatesecuredata.arch.mvvm.android.MVVMActivity;
import com.privatesecuredata.arch.mvvm.android.MVVMComplexVmAdapter;
import com.privatesecuredata.arch.tools.unitconversion.MeasurementValue;
import com.privatesecuredata.arch.tools.vm.MeasurementValueVM;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValueControl extends FrameLayout implements IBindableView<MeasurementValueVM>{
    private String _hint = "";
    private String _formatStr = "%.2f";
    private boolean _readonly = true;
    private MeasurementValueVM _value;
    private MVVMComplexVmAdapter<MeasurementValueVM> adapter;

    public MeasurementValueControl(@NonNull Context context) {
        this(context, null, 0);
    }

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
        //TODO Implement me
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr)
    {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_measvalctrl, 0, 0);

            //1 - Vertical, 0 - Horizontal
            int orientation = a.getInt(R.styleable.psdarch_number_selector_android_orientation, 1);
            if (0 == orientation)
                LayoutInflater.from(context).inflate(R.layout.psdarch_measueremen_value_control, this, true);
            else
                LayoutInflater.from(context).inflate(R.layout.psdarch_measueremen_value_control, this, true);

            _readonly = a.getBoolean(R.styleable.psdarch_measvalctrl_readonly, true);
            _hint = a.getString(R.styleable.psdarch_measvalctrl_android_hint);
            _formatStr = a.getString(R.styleable.psdarch_measvalctrl_format_string);
            a.recycle();
        }
    }

    @Override
    public void bind(MeasurementValueVM vm) {

        if (null != _value) {
            if (null != adapter)
                adapter.dispose();

            _value = vm;
            adapter = new MVVMComplexVmAdapter<MeasurementValueVM>((MVVMActivity) getContext(), this, _value);

            if (!_readonly)
                adapter.setMapping(Double.class, R.id.psdarch_measvalctrl_edit_value,
                        vm1 -> vm.getValueVM());
            else
                adapter.setMapping(Double.class,  R.id.psdarch_measvalctrl_edit_value,
                        vm1 -> vm.getValueVM());


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
