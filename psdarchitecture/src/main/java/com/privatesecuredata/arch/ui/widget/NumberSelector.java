package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.privatesecuredata.arch.R;

/**
 * Created by kenan on 2/20/15.
 */
public class NumberSelector extends FrameLayout {
    private int _number;
    private TextView _txtNumber;

    public NumberSelector(Context context) {
        this(context, null, 0, 0);
    }

    public NumberSelector(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public NumberSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberSelector(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.psdarch_number_selector, this, true);
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

    public void setNumber(int _num)
    {
        _number = _num;
        if (null != _txtNumber)
            _txtNumber.setText(new Integer(_number).toString());
    }

    public int getNumber()
    {
        return _number;
    }

    public void onButtonUpClick(View v) {
        setNumber(getNumber() + 1);
    }

    public void onButtonDownClick(View v) {
        setNumber(getNumber() - 1);
    }
}
