package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.privatesecuredata.arch.R;

/**
 * Created by kenan on 4/17/15.
 */
public class DialogTitlebar extends FrameLayout {
    private ImageView _defaultCancelIcon;
    private TextView  _txtTitle;
    private TextView  _txtActionOk;

    public DialogTitlebar(Context context) {
        this(context, null, 0, 0);
    }

    public DialogTitlebar(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public DialogTitlebar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DialogTitlebar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(R.layout.psdarch_dialog_titlebar, this, true);
        _defaultCancelIcon = (ImageView)findViewById(R.id.psdarch_btn_cancel);
        _txtTitle = (TextView)findViewById(R.id.psdarch_dialog_titlebar_txt_title);
        _txtActionOk = (TextView)findViewById(R.id.psdarch_btn_ok);

        Resources res = context.getResources();

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_dialog_titlebar, 0, 0);

            int cancelDrawableId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_icon_cancel, R.drawable.ic_cancel);
            Drawable cancelDrawable = res.getDrawable(cancelDrawableId);
            _defaultCancelIcon.setImageDrawable(cancelDrawable);

            int txtTitleId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_txt_title, R.string.psdarch_dialog_titlebar_title);
            String title = res.getString(txtTitleId);
            _txtTitle.setText(title);

            int txtOkId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_txt_ok, R.string.psdarch_dialog_titlebar_txt_ok);
            String action = res.getString(txtOkId);
            _txtActionOk.setText(action);

            a.recycle();
        }
    }
}
