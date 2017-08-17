package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.privatesecuredata.arch.R;

/**
 * Created by kenan on 10/28/16.
 */

public class EditableTitleDialogTitlebar extends DialogTitlebar {
    private ImageView _titleIcon;

    public EditableTitleDialogTitlebar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditableTitleDialogTitlebar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context, AttributeSet attrs, int defStyleAttr) {
        super.init(context, attrs, defStyleAttr);
        _titleIcon = (ImageView)findViewById(R.id.psdarch_dialog_titlebar_ic_title);

        if ( (attrs != null) && (!isInEditMode()) ) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_dialog_editable_title_titlebar, 0, 0);

            boolean hideTitleIcon = a.getBoolean(R.styleable.psdarch_dialog_editable_title_titlebar_hide_title_icon, false);
            _titleIcon.setVisibility(hideTitleIcon ? View.GONE : View.VISIBLE);

            a.recycle();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.psdarch_editable_title_dialog_titlebar;
    }

    public void setOnTitleClickListener(OnClickListener l) {
        View v = findViewById(R.id.psdarch_dialog_titlebar_title);
        v.setOnClickListener(l);
    }

    public void setTitleIconVisibility(int visibility)
    {
        _titleIcon.setVisibility(visibility);
    }
}
