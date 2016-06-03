package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by kenan on 6/3/16.
 */
public class SimpleDivider extends RecyclerView.ItemDecoration {

    Drawable dividerDrawable;
    int leftInset = 0;

    /**
     * Constructor for Divider
     *
     * @param drawable The drawable to use
     **/
    public SimpleDivider(Drawable drawable) {
        dividerDrawable = drawable;
    }

    /**
     * Constructor for Divider
     *
     * @param ctx             The Contextd
     * @param deviderDrawable The ressource ID
     **/
    public SimpleDivider(Context ctx, int deviderDrawable) {
        dividerDrawable = ContextCompat.getDrawable(ctx, deviderDrawable);
    }

    /**
     * Constructor for Divider
     *
     * @param ctx             The Contextd
     * @param deviderDrawable The ressource ID
     * @param dimenRessource  offset of divider to the left
     */
    public SimpleDivider(Context ctx, int deviderDrawable, int dimenRessource) {
        dividerDrawable = ContextCompat.getDrawable(ctx, deviderDrawable);
        this.leftInset = ctx.getResources().getDimensionPixelSize(dimenRessource);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        //skip for first item in list
        if (parent.getChildAdapterPosition(view) == 0) {
            return;
        }

        //add some space for the divider to draw
        outRect.bottom = dividerDrawable.getIntrinsicHeight();
    }

    /**
     * @Override public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
     * <p>
     * int left = parent.getPaddingLeft() + leftInset;
     * int right = parent.getWidth() - parent.getPaddingRight();
     * <p>
     * int childCount = parent.getChildCount();
     * for (int i = 0; i < childCount - 1; i++) {
     * View child = parent.getChildAt(i);
     * <p>
     * RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
     * <p>
     * int top = child.getBottom() + params.bottomMargin;
     * int bottom = top + dividerDrawable.getIntrinsicHeight();
     * <p>
     * dividerDrawable.setBounds(left, top, right, bottom);
     * dividerDrawable.draw(c);
     * }
     * }
     **/

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int left = parent.getPaddingLeft() + leftInset;
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + dividerDrawable.getIntrinsicHeight();

            dividerDrawable.setBounds(left, top, right, bottom);
            dividerDrawable.draw(c);
        }
    }
}
