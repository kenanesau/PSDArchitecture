package com.privatesecuredata.arch.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueAccessor;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueReceiver;

import java.util.ArrayList;

/**
 * Created by kenan on 2/20/15.
 */
public class NumberSelector extends FrameLayout
                            implements IWidgetValueAccessor {
    private long _number;
    private TextView _txtNumber;
    private ArrayList<IWidgetValueReceiver> valueReceivers = new ArrayList<IWidgetValueReceiver>();

    public NumberSelector(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberSelector(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NumberSelector(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr)
    {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_number_selector, 0, 0);

            //1 - Vertical, 0 - Horizontal
            int orientation = a.getInt(R.styleable.psdarch_number_selector_android_orientation, 1);
            if (0 == orientation)
                LayoutInflater.from(context).inflate(R.layout.psdarch_number_selector_horizontal, this, true);
            else
                LayoutInflater.from(context).inflate(R.layout.psdarch_number_selector, this, true);

            a.recycle();
        }
        _txtNumber = (TextView)findViewById(R.id.psdarch_number_selector_txt_number);

        Fab fabUp = (Fab)findViewById(R.id.psdarch_number_selector_btn_up);
        fabUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberSelector.this.onButtonUpClick(v);
            }
        });

        Fab fabDown = (Fab)findViewById(R.id.psdarch_number_selector_btn_down);
        fabDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NumberSelector.this.onButtonDownClick(v);
            }
        });
    }

    public void setNumber(long _num)
    {
        _number = _num;
        if (null != _txtNumber)
            _txtNumber.setText(Long.toString(_number));

        for(IWidgetValueReceiver rec : this.valueReceivers)
            rec.notifyWidgetChanged(this);
    }

    public long getNumber()
    {
        return _number;
    }

    public void onButtonUpClick(View v) {
        setNumber(getNumber() + 1);
    }

    public void onButtonDownClick(View v) {
        setNumber(getNumber() - 1);
    }

    @Override
    public void registerValueChanged(IWidgetValueReceiver valueReceiver) {
        this.valueReceivers.add(valueReceiver);
    }

    @Override
    public void unregisterValueChanged(IWidgetValueReceiver valueReceiver) {
        this.valueReceivers.remove(valueReceiver);
    }

    @Override
    public void setValue(Object val) {
        setNumber((long)val);
    }

    @Override
    public Object getValue() {
        return getNumber();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }
}
