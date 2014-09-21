package com.privatesecuredata.arch.ui.widget;

import com.privatesecuredata.arch.R;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.PathInterpolator;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class Fab extends FrameLayout implements Checkable {
	private static final String SUPERSTATE="superState";
	private static final String CHECKEDSTATE = "psdarch_fab_checked";
	
	private float _touchX;
	private float _touchY;
	//private View _view;
	
	private Drawable _checkedDrawable;
	private Drawable _defaultDrawable;
	
	private ImageView _defaultImageView;
	private ImageView _checkedImageView;
	
	private Animator _iconInAnimator;
	private Animator _iconOutAnimator;
	
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

		setClickable(true);
		setFocusable(true);
				
		LayoutInflater.from(context).inflate(R.layout.psdarch_fab, this, true);

//		_view = new View(context);
//		_view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//		addView(_view, 0);
		
		_defaultImageView = (ImageView)findViewById(R.id.psdarch_fab_icon);
		_checkedImageView = (ImageView)findViewById(R.id.psdarch_checked_fab_icon);
		_color = getResources().getColor(R.color.accent);
		this.setBackgroundColor(_color);
		
		Resources res = context.getResources();
		if (attrs!=null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Fab, 0, 0);
			
			int checkedDrawableId = a.getResourceId(R.styleable.Fab_icon_checked, R.drawable.ic_action_done);
			_checkedDrawable = res.getDrawable(checkedDrawableId);
			_checkedImageView.setImageDrawable(_checkedDrawable);
			
			int defaultDrawableId = a.getResourceId(R.styleable.Fab_icon_default, R.drawable.ic_action_add_item);
			_defaultDrawable = res.getDrawable(defaultDrawableId);
			_defaultImageView.setImageDrawable(_defaultDrawable);

			int iconOutAnimationId = a.getResourceId(R.styleable.Fab_icon_in_animation, R.animator.fab_animate_icon_out);
			_iconOutAnimator = (Animator) AnimatorInflater.loadAnimator(context, iconOutAnimationId);
			
			int iconInAnimationId = a.getResourceId(R.styleable.Fab_icon_out_animation, R.animator.fab_animate_icon_in);
			_iconInAnimator = (Animator) AnimatorInflater.loadAnimator(context, iconInAnimationId);
			
			a.recycle();
		}
	}
	
	@Override
	public boolean isChecked() {
		return _checked;
	}

	@Override
	public void setChecked(boolean checked) {
		setChecked(checked, true);
	}
	
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
//            ValueAnimator animator = ViewAnimationUtils.createCircularReveal(
//                    _view, (int) _touchX, (int) _touchY, 0, getWidth());
//            animator.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    setChecked(_checked, false);
//                }
//            });
//            animator.start();
        	

            if (_checked) {
            	_checkedImageView.setVisibility(View.VISIBLE);
            	_iconOutAnimator.setTarget(_defaultImageView);
            	_iconInAnimator.setTarget(_checkedImageView);
            	ObjectAnimator moveInAnim = getMoveInAnimFor(_checkedImageView);
            	ObjectAnimator moveOutAnim = getMoveOutAnimFor(_defaultImageView);
            	
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
            	//_defaultImageView.setVisibility(View.GONE);
            }
            else
            {
            	_defaultImageView.setVisibility(View.VISIBLE);
            	_iconOutAnimator.setTarget(_checkedImageView);
            	_iconInAnimator.setTarget(_defaultImageView);
            	_iconInAnimator.addListener(new AnimatorListenerAdapter() {
            		@Override
            		public void onAnimationEnd(Animator animation) {
            			setChecked(_checked, false);
            		}
            	});
            	ObjectAnimator moveInAnim = getMoveInAnimFor(_defaultImageView); 
            	ObjectAnimator moveOutAnim = getMoveOutAnimFor(_checkedImageView);
            	_iconOutAnimator.start();
            	moveOutAnim.start();
            	//_checkedImageView.setVisibility(View.GONE);
            	_iconInAnimator.start();
            	moveInAnim.start();
            }
//            _view.setVisibility(View.VISIBLE);
//            _view.setBackgroundColor(_checked ? Color.WHITE : _color);
        } else {
//            _view.setVisibility(View.GONE);
            int bgResourceId = (_checked ? R.drawable.fab_ripple_background_on
                    					 : R.drawable.fab_ripple_background_off);
            RippleDrawable newBackground = (RippleDrawable) getResources().getDrawable(bgResourceId);
            setBackground(newBackground);
            //TODO: Build Logic to be certain that ALL animations ended...
            if (_checked) {
            	_defaultImageView.setVisibility(View.GONE);
            }
            else
            {
            	_checkedImageView.setVisibility(View.GONE);
            }
        }
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
    public boolean performClick() {
    	toggle();
    	return super.performClick();
    }
}
