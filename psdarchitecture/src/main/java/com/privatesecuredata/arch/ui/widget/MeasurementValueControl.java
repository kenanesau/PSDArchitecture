package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by kenan on 8/7/17.
 */

public class MeasurementValueControl extends FrameLayout{
    public MeasurementValueControl(@NonNull Context context) {
        super(context);
    }

    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MeasurementValueControl(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setReadOnly(boolean ro) {
        //TODO Implement me
    }
}
