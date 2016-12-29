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
    private int layoutId = R.layout.psdarch_dialog_titlebar;
    private int cancelId = R.id.psdarch_btn_cancel;
    private int titleId = R.id.psdarch_dialog_titlebar_txt_title;
    private int okId = R.id.psdarch_btn_ok;

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

    protected int getLayoutId() {
        return layoutId;
    }

    protected int getCancelId() {
        return cancelId;
    }

    protected int getOkId() {
        return okId;
    }

    protected int getTitleId() {
        return titleId;
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr)
    {
        if (!isInEditMode())
            LayoutInflater.from(context).inflate(getLayoutId(), this, true);
        _defaultCancelIcon = (ImageView)findViewById(getCancelId());
        _txtTitle = (TextView)findViewById(getTitleId());
        _txtActionOk = (TextView)findViewById(getOkId());

        if ( (attrs != null) && (!isInEditMode()) ) {
            Resources res = context.getResources();

            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_dialog_titlebar, 0, 0);

            int cancelDrawableId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_icon_cancel, R.drawable.ic_cancel);
            Drawable cancelDrawable = res.getDrawable(cancelDrawableId);
            _defaultCancelIcon.setImageDrawable(cancelDrawable);

            int txtTitleId = a.getResourceId(R.styleable.psdarch_dialog_titlebar_txt_title, R.string.psdarch_dialog_titlebar_title);
            String title = res.getString(txtTitleId);
            _txtTitle.setText(title);

            boolean hideOk = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_ok, false);
            _txtActionOk.setVisibility(hideOk ? View.GONE : View.VISIBLE);

            boolean hideCancel = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_cancel, false);
            _defaultCancelIcon.setVisibility(hideCancel ? View.GONE :View.VISIBLE);

            boolean hideTitle = a.getBoolean(R.styleable.psdarch_dialog_titlebar_hide_title, false);
            _txtTitle.setVisibility(hideTitle ? View.GONE :View.VISIBLE);

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
