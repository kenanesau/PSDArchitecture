package com.privatesecuredata.arch.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;

import com.privatesecuredata.arch.R;

public class CheckableFab extends Fab implements Checkable {
	private static final String SUPERSTATE="superState";
	private static final String CHECKEDSTATE = "psdarch_fab_checked";
	
	private float _touchX;
	private float _touchY;

	private Drawable _checkedDrawable;
	private ImageView _checkedImageView;

    private int _checkedColor;
    //private Drawable _checkedBg;
    //private Drawable _uncheckedBg;
	
	private Animator _iconInAnimator;
	private Animator _iconOutAnimator;
	
	private boolean _checked = false;

	private static final int[] CheckedStateSet = {
		android.R.attr.state_checked
	};
	
	public CheckableFab(Context context, AttributeSet attrs) {
		this(context, attrs, 0, 0);
	}

	public CheckableFab(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CheckableFab(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr);

		_checkedImageView = (ImageView)findViewById(R.id.psdarch_checked_fab_icon);
		
		if (attrs!=null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_fab, 0, 0);

            int checkedBgColor = a.getResourceId(R.styleable.psdarch_fab_checked_bgcolor, R.color.accent);
            _checkedColor = ContextCompat.getColor(context, checkedBgColor);

            int checkedDrawableId = a.getResourceId(R.styleable.psdarch_fab_icon_checked, R.drawable.ic_action_done);
			_checkedDrawable = ContextCompat.getDrawable(context, checkedDrawableId);
			_checkedImageView.setImageDrawable(_checkedDrawable);
            _checkedImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				int iconOutAnimationId = a.getResourceId(R.styleable.psdarch_fab_icon_in_animation, R.animator.fab_animate_icon_out);
				_iconOutAnimator = (Animator) AnimatorInflater.loadAnimator(context, iconOutAnimationId);

				int iconInAnimationId = a.getResourceId(R.styleable.psdarch_fab_icon_out_animation, R.animator.fab_animate_icon_in);
				_iconInAnimator = (Animator) AnimatorInflater.loadAnimator(context, iconInAnimationId);
			}
/*
            int checkedBgDrawableId = a.getResourceId(R.styleable.psdarch_fab_checked_bg_drawable, R.drawable.fab_ripple_background_on);
            _checkedBg = ContextCompat.getDrawable(context, checkedBgDrawableId);

            int uncheckedBgDrawableId = a.getResourceId(R.styleable.psdarch_fab_unchecked_bg_drawable, R.drawable.fab_ripple_background_off);
            _uncheckedBg = ContextCompat.getDrawable(context, uncheckedBgDrawableId);*/
			a.recycle();
		}

        setChecked(_checked, false);
	}
	
	@Override
	public boolean isChecked() {
		return _checked;
	}

	@Override
	public void setChecked(boolean checked) {
		setChecked(checked, true);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private ObjectAnimator getMoveInAnimFor(ImageView target)
	{
    	Path path = new Path();
    	path.rQuadTo(0.0f, 0.4f, 1.0f, 1.0f);

		ObjectAnimator moveInAnim = ObjectAnimator.ofFloat(target, "X", "Y", path);
		PropertyValuesHolder[] vals = moveInAnim.getValues();
		vals[0].setFloatValues(-target.getWidth(), 0.0f);
		vals[1].setFloatValues(-target.getHeight(), 0.0f);
    	return moveInAnim;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private ObjectAnimator getMoveOutAnimFor(ImageView target)
	{
    	Path path = new Path();
    	path.rQuadTo(0.0f, 0.4f, 1.0f, 1.0f);

		ObjectAnimator moveInAnim = ObjectAnimator.ofFloat(target, "X", "Y", path);
		PropertyValuesHolder[] vals = moveInAnim.getValues();
		vals[0].setFloatValues(0.0f, target.getWidth());
		vals[1].setFloatValues(0.0f, target.getHeight());
    	return moveInAnim;
	}
	
	public void setChecked(boolean checked, boolean allowAnimate) {
    	_checked = checked;
		refreshDrawableState();
        
        if (allowAnimate) {
            if (_checked) {
                _checkedImageView.setVisibility(View.VISIBLE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    _iconOutAnimator.setTarget(getDefaultImageView());
                    _iconInAnimator.setTarget(_checkedImageView);
                    ObjectAnimator moveInAnim = getMoveInAnimFor(_checkedImageView);
                    ObjectAnimator moveOutAnim = getMoveOutAnimFor(getDefaultImageView());

                    _iconInAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setChecked(_checked, false);
                        }
                    });
                    _iconInAnimator.start();
                    moveInAnim.start();
                    _iconOutAnimator.start();
                    moveOutAnim.start();
                }
                else {
                    setChecked(_checked, false);
                }

            	getDefaultImageView().setVisibility(View.GONE);
            }
            else {
                getDefaultImageView().setVisibility(View.VISIBLE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    _iconOutAnimator.setTarget(_checkedImageView);
                    _iconInAnimator.setTarget(getDefaultImageView());
                    _iconInAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setChecked(_checked, false);
                        }
                    });
                    ObjectAnimator moveInAnim = getMoveInAnimFor(getDefaultImageView());
                    ObjectAnimator moveOutAnim = getMoveOutAnimFor(_checkedImageView);
                    _iconOutAnimator.start();
                    moveOutAnim.start();
                    _iconInAnimator.start();
                    moveInAnim.start();
                } else {
                    setChecked(_checked, false);
                }
                _checkedImageView.setVisibility(View.GONE);
            }
        }
        else {
            //Drawable bg = (_checked ? _checkedBg : _uncheckedBg);
            int col = (_checked ? _checkedColor : _color);
            FloatingActionButton fabView = (FloatingActionButton)findViewById(R.id.psdarch_fab_widget);
            //ViewCompat.setBackground(fabView, bg);
            fabView.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{0}}, new int[]{col}));
        }
    }

	@Override
	public void toggle() {
		setChecked(!_checked);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CheckedStateSet);
		}
		return drawableState;
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            _touchX = event.getX();
            _touchY = event.getY();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
    	Bundle state = new Bundle();
    	state.putParcelable(SUPERSTATE, super.onSaveInstanceState());
    	state.putBoolean(CHECKEDSTATE, isChecked());
    	return state;
    }
    
    @Override
    public void onRestoreInstanceState(Parcelable ss) {
    	Bundle state=(Bundle)ss;
    	super.onRestoreInstanceState(state.getParcelable(SUPERSTATE));
    	setChecked(state.getBoolean(CHECKEDSTATE));
    }
    
    @Override
    protected void doAnimation() {
    	toggle();
    }
}
