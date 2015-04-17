package com.privatesecuredata.arch.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.privatesecuredata.arch.R;


public class Fab extends FrameLayout 
{
	private Drawable _defaultDrawable;
	private ImageView _defaultImageView;
	private Animator _defaultAnimator;
	private int _color;
    private boolean _disableOutline;
	
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
		super(context, attrs, defStyleAttr, defStyleRes);
		
		setClickable(true);
		setFocusable(true);
				
		LayoutInflater.from(context).inflate(R.layout.psdarch_fab, this, true);
		_defaultImageView = (ImageView)findViewById(R.id.psdarch_fab_icon);
		
		Resources res = context.getResources();

		_color = getResources().getColor(R.color.accent_light);

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.psdarch_fab, 0, 0);
			
			int defaultDrawableId = a.getResourceId(R.styleable.psdarch_fab_icon_default, R.drawable.ic_action_add_small);
			_defaultDrawable = res.getDrawable(defaultDrawableId);
			getDefaultImageView().setImageDrawable(_defaultDrawable);
            getDefaultImageView().setColorFilter(_color, PorterDuff.Mode.DST);
			
			int defaultAnimationId = a.getResourceId(R.styleable.psdarch_fab_default_animation, R.animator.fab_animation_default);
			_defaultAnimator = (Animator) AnimatorInflater.loadAnimator(context, defaultAnimationId);

            _disableOutline = a.getBoolean(R.styleable.psdarch_fab_disable_outline, false);

			a.recycle();
		}
	}
	
	protected Drawable getDefaultDrawable() { return _defaultDrawable; }
	protected ImageView getDefaultImageView() { return _defaultImageView; }
	protected Animator getDefaultAnimator() { return _defaultAnimator; }
	protected int getBgColor() { return _color; }
	
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!_disableOutline) {
            setOutlineProvider(new ViewOutlineProvider() {

                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });

            setClipToOutline(true);
        }
    }

    protected void onSizeChangedNoOutline(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
    protected void doAnimation()
    {
    	getDefaultAnimator().setTarget(getDefaultImageView());
    	getDefaultAnimator().start();
    }
    
    @Override
    public boolean performClick() {
    	doAnimation();
    	return super.performClick();
    }
}
