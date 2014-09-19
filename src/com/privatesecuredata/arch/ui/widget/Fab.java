package com.privatesecuredata.arch.ui.widget;

import com.privatesecuredata.arch.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class Fab extends FrameLayout implements Checkable {
	private float _touchX;
	private float _touchY;
	private View _view;
	private int _color;
	
	private boolean _checked = false;
	private static final int[] CheckedStateSet = {
		android.R.attr.state_checked
	};


	public Fab(Context context) {
		this(context, null, 0, 0);
	}

	public Fab(Context context, AttributeSet attrs) {
		this(context, attrs, 0, 0);
	}

	public Fab(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public Fab(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr);

		_view = new View(context);
		_view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		addView(_view, 0);
		_color = getResources().getColor(R.color.accent);
	}


	@Override
	public boolean isChecked() {
		return _checked;
	}

	@Override
	public void setChecked(boolean checked) {
		setChecked(checked, true);
	}

	@Override
	public void toggle() {
		setChecked(!_checked);
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Outline outline = new Outline();
        outline.setOval(0, 0, w, h);
        setOutline(outline);
        setClipToOutline(true);
    }

    public void setChecked(boolean checked, boolean allowAnimate) {
    	_checked = checked;
		refreshDrawableState();
        
        if (allowAnimate) {
            ValueAnimator animator = ViewAnimationUtils.createCircularReveal(
                    _view, (int) _touchX, (int) _touchY, 0, getWidth());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setChecked(_checked, false);
                }
            });
            animator.start();
            _view.setVisibility(View.VISIBLE);
            _view.setBackgroundColor(_checked ? Color.WHITE : _color);
        } else {
            _view.setVisibility(View.GONE);
            int bgResourceId = (_checked ? R.drawable.fab_ripple_background_on
                    					 : R.drawable.fab_ripple_background_off);
            RippleDrawable newBackground = (RippleDrawable) getResources().getDrawable(bgResourceId);
            setBackground(newBackground);
        }
    }

}
