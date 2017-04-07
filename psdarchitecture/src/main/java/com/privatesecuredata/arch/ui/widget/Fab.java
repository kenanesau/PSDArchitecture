package com.privatesecuredata.arch.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.privatesecuredata.arch.R;


public class Fab extends FrameLayout
{
	protected Drawable _defaultDrawable;
    protected ImageView _defaultImageView;
    protected Animator _defaultAnimator;
    protected int _color;
    protected boolean _disableOutline;
	
	public Fab(Context context) {
		this(context, null, 0);
	}

	public Fab(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public Fab(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

        if (isInEditMode()) {
            return;
        }
        setClickable(true);
        setFocusable(true);

        inflate(context, R.layout.psdarch_fab, this);
        _defaultImageView = (ImageView)findViewById(R.id.psdarch_fab_icon);
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.psdarch_fab_widget);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            _defaultImageView.setElevation(fab.getElevation() +
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            6,
                            context.getResources().getDisplayMetrics()));
            _defaultImageView.setTranslationZ(fab.getTranslationZ());
            _defaultImageView.setStateListAnimator(fab.getStateListAnimator());
            fab.setBackground(this.getBackground());
        }
        else {
            ViewCompat.setElevation(_defaultImageView, ViewCompat.getElevation(fab) + 6);
            ViewCompat.setTranslationZ(_defaultImageView, ViewCompat.getTranslationZ(fab));
            ViewCompat.setBackground(fab, this.getBackground());
        }
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_fab, 0, 0);
            int bgColor = a.getResourceId(R.styleable.psdarch_fab_bgcolor, R.color.accent);
            _color = ContextCompat.getColor(context, bgColor);
            fab.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{0}}, new int[]{_color}));

            int defaultDrawableId = a.getResourceId(R.styleable.psdarch_fab_icon_default, R.drawable.ic_action_add_small);
            _defaultDrawable = ContextCompat.getDrawable(context, defaultDrawableId);


            int defaultAnimationId = a.getResourceId(R.styleable.psdarch_fab_default_animation, R.animator.fab_animation_default);
            _defaultAnimator = (Animator) AnimatorInflater.loadAnimator(context, defaultAnimationId);
            _disableOutline = a.getBoolean(R.styleable.psdarch_fab_disable_outline, false);
            if (_disableOutline)
            {
                fab.setVisibility(GONE);
            }

            a.recycle();
            setDefaultDrawable(_defaultDrawable);
        }

        setAlpha(1.0f);
	}

    public void setDefaultDrawable(Drawable drawable) {
        ImageView imgView = getDefaultImageView();
        imgView.setImageDrawable(drawable);
        imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //imgView.setColorFilter(_color, PorterDuff.Mode.DST);
        getDefaultAnimator().setTarget(imgView);
    }

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Fab(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	protected Drawable getDefaultDrawable() { return _defaultDrawable; }
    public ImageView getDefaultImageView() { return _defaultImageView; }
	public Animator getDefaultAnimator() { return _defaultAnimator; }
	protected int getBgColor() { return _color; }
	
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
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

        super.onSizeChanged(w, h, oldw, oldh);
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
