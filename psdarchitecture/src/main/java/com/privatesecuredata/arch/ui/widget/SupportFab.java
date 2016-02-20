package com.privatesecuredata.arch.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.privatesecuredata.arch.R;


public class SupportFab extends FrameLayout
{
	private Drawable _defaultDrawable;
	private FloatingActionButton _defaultImageView;
	private Animator _defaultAnimator;
	private int _color;
    private boolean _disableOutline;

	public SupportFab(Context context) {
		this(context, null, 0);
	}

	public SupportFab(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SupportFab(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

        setClickable(true);
        setFocusable(true);
        setBackgroundResource(android.R.color.transparent);

        LayoutInflater.from(context).inflate(R.layout.psdarch_support_fab, this, true);
        _defaultImageView = (FloatingActionButton)findViewById(R.id.psdarch_support_fab_icon);

        _color = ContextCompat.getColor(context, R.color.accent_light);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_fab, 0, 0);

            int defaultDrawableId = a.getResourceId(R.styleable.psdarch_fab_icon_default, R.drawable.ic_action_add_small);
            _defaultDrawable = ContextCompat.getDrawable(context, defaultDrawableId);
            getDefaultImageView().setImageDrawable(_defaultDrawable);
            getDefaultImageView().setRippleColor(_color);

            int defaultAnimationId = a.getResourceId(R.styleable.psdarch_fab_default_animation, R.animator.fab_animation_default);
            _defaultAnimator = (Animator) AnimatorInflater.loadAnimator(context, defaultAnimationId);

            _disableOutline = a.getBoolean(R.styleable.psdarch_fab_disable_outline, false);

            a.recycle();

            getDefaultAnimator().setTarget(_defaultDrawable);
        }
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SupportFab(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	protected Drawable getDefaultDrawable() { return _defaultDrawable; }
	public FloatingActionButton getDefaultImageView() { return _defaultImageView; }
	public Animator getDefaultAnimator() { return _defaultAnimator; }
	protected int getBgColor() { return _color; }
	
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!_disableOutline) {
                setOutlineProvider(new ViewOutlineProvider() {

                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
            }

            setClipToOutline(true);
        }
    }

    protected void onSizeChangedNoOutline(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    protected void doAnimation()
    {
        getDefaultAnimator().start();
    }

    @Override
    public boolean performClick() {
    	doAnimation();
        return super.performClick();
    }
}
