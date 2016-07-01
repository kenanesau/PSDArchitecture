package com.privatesecuredata.arch.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
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
        this(context, attrs, 0);
    }

    public DialogTitlebar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DialogTitlebar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr);
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr)
    {
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

            boolean hideOk = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_ok, false);
            if (hideOk)
                _txtActionOk.setVisibility(View.GONE);
            else
                _txtActionOk.setVisibility(View.VISIBLE);

            boolean hideCancel = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_cancel, false);
            if (hideCancel)
                _defaultCancelIcon.setVisibility(View.GONE);
            else
                _defaultCancelIcon.setVisibility(View.VISIBLE);

            boolean hideTitle = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_title, false);
            if (hideTitle)
                _txtTitle.setVisibility(View.GONE);
            else
                _txtTitle.setVisibility(View.VISIBLE);

            int txtOkId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_txt_ok, R.string.psdarch_dialog_titlebar_txt_ok);
            String action = res.getString(txtOkId);
            _txtActionOk.setText(action);

            a.recycle();
        }
    }

    public void setTitle(CharSequence text) {
        _txtTitle.setText(text);
    }

    public void setTitleVisibility(int visibility)
    {
        _txtTitle.setVisibility(visibility);
    }

    public void setOKVisibility(int visibility)
    {
        _txtActionOk.setVisibility(visibility);
    }

    public void setCancelVisibility(int visibility)
    {
        _defaultCancelIcon.setVisibility(visibility);
    }

    public void setActionText(CharSequence text) {
        _txtActionOk.setText(text);
    }
}
