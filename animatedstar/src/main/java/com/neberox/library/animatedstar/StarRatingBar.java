/*
 * Copyright (c) 2018 Muhammad Umar (https://github.com/zelin)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.neberox.library.animatedstar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.neberox.library.animatedstar.vector.VectorDrawableCompat;

import java.util.ArrayList;

/**
 * Created by Umar on 20/06/2018.
 */

public class StarRatingBar extends RelativeLayout
{
    private class CircularPath
    {
        public float x = 0;
        public float y = 0;
        public float r = 0;

        public int value = 0;
    }

    private static final String TAG = "DiamondRatingBar";

    /* Initial fillColor used to create star lobes */
    private int mFillColor       = Color.WHITE;

    /* Selected Rating Color to fill the lobes */
    private int mSelectedColor   = Color.parseColor("#f2b01e");

    /* The text size set in sp. Default is 18sp */
    private int mTextSize     = 18;
    /* Padding used to move Text around the lobe. Ranging from 0 to 1 */
    private float mTextPadding  = 0.5f;
    /* Initial text color to be drawn over the initial lobes. Default is gray */
    private int mTextColor          = Color.LTGRAY;
    /* Final text color to be drawn over the rated lobes. Default is white */
    private int mSelectedTextColor  = Color.WHITE;

    /* Stroke width around each lobe. The stroke will work only on initial generated lobes with 0 Rating */
    private int mStrokeWidth      = 1;
    /* Color of stroke. Default is Light Gray */
    private int mStrokeColor      = Color.LTGRAY;
    /* Animation duration of adding color to selected lobs. */
    private long mAnimDuration = 200;

    private GestureDetector mTapDetector;

    private ArrayList<ImageView> mImages = new ArrayList<>();
    private ImageView mTextImgView = null;

    private ArrayList<CircularPath> circularPaths = new ArrayList<>();

    private int rating = 0;

    /**
     *
     * @param context Context
     */
    public StarRatingBar(Context context)
    {
        super(context);
        setWillNotDraw(false);
        initAttributes(context, null, 0);
        // We can not generate a bitmap with 0 width and height.
        this.post(new Runnable()
        {
            @Override
            public void run()
            {
                addViews();
            }
        });
    }

    /**
     *
     * @param context Context
     * @param attrs Attributes
     */
    public StarRatingBar(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        setWillNotDraw(false);
        initAttributes(context, attrs, 0);
        // We can not generate a bitmap with 0 width and height.
        this.post(new Runnable()
        {
            @Override
            public void run()
            {
                addViews();
            }
        });
    }

    public StarRatingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        initAttributes(context, attrs, defStyleAttr);
        // We can not generate a bitmap with 0 width and height.
        this.post(new Runnable()
        {
            @Override
            public void run()
            {
                addViews();
            }
        });
    }

    /**
     *
     * @param mFillColor Set the Initial Fill color for generating non rated lobes. Default is white.
     */
    public void setFillColor(int mFillColor)
    {
        this.mFillColor = mFillColor;
    }

    /**
     *
     * @param mSelectedColor Set the Final Fill color for generating rated lobes. Default is #f2b01e
     */
    public void setSelectedColor(int mSelectedColor)
    {
        this.mSelectedColor = mSelectedColor;
    }

    /**
     *
     * @param mTextSize Sets the text size of the rating points to be drawn on the lobes. Default is 18sp
     */
    public void setTextSize(int mTextSize)
    {
        this.mTextSize = (int ) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize, getResources().getDisplayMetrics());
    }

    /**
     *
     * @param mTextPadding Padding given to text to be placed inside lobe. Default is 0.5 (Middle of the lobe).
     *                     Range should be less or equal to 1.0f
     */
    public void setTextPadding(@FloatRange(from = 0, to = 1.0) float mTextPadding)
    {
        this.mTextPadding = mTextPadding;
    }

    /**
     *
     * @param mTextColor Color of the text to be drawn over non rated lobes. Default is light gray
     */
    public void setTextColor(int mTextColor)
    {
        this.mTextColor = mTextColor;
    }

    /**
     *
     * @param mSelectedTextColor Color of the text to be drawn over rated lobes. Default is white
     */
    public void setSelectedTextColor(int mSelectedTextColor)
    {
        this.mSelectedTextColor = mSelectedTextColor;
    }

    /**
     *
     * @param mStrokeWidth  Stroke width size over the un rated lobes. Default is light gray.
     */
    public void setStrokeWidth(int mStrokeWidth)
    {
        this.mStrokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mStrokeWidth, getResources().getDisplayMetrics());
    }

    /**
     *
     * @param mStrokeColor Stroke color over the un rated lobes. Default is light gray.
     */
    public void setStrokeColor(int mStrokeColor)
    {
        this.mStrokeColor = mStrokeColor;
    }

    /**
     *
     * @param mAnimDuration Animation duration over which rated lobes are generated. Default is 200 milliseconds
     */
    public void setAnimDuration(long mAnimDuration)
    {
        this.mAnimDuration = mAnimDuration;
    }

    /**
     *
     * @param rating Set current rating
     */
    public void setRating(int rating)
    {
        this.rating = rating;
        for (int i = 0; i < mImages.size(); i++)
        {
            mImages.get(i).clearAnimation();
            mImages.get(i).setAlpha(0f);
        }
        removeView(mTextImgView);
        createAnimation(rating - 1);
    }

    public int getRating()
    {
        return rating;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        /* We need to make sure that the View is a perfect Square */
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttr)
    {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StarRatingBar);

        mFillColor      = a.getColor(R.styleable.StarRatingBar_fillColor, mFillColor);
        mSelectedColor  = a.getColor(R.styleable.StarRatingBar_selectedColor, mSelectedColor);
        mStrokeColor    = a.getColor(R.styleable.StarRatingBar_strokeColor, mStrokeColor);
        mTextColor      = a.getColor(R.styleable.StarRatingBar_textColor, mTextColor);
        mSelectedTextColor = a.getColor(R.styleable.StarRatingBar_selectedTextColor, mSelectedTextColor);

        mTextPadding = a.getFloat(R.styleable.StarRatingBar_textPadding, mTextPadding);
        if (mTextPadding > 1)
            mTextPadding = 0.5f;
        else if (mTextPadding < 0)
            mTextPadding = 0.5f;

        mStrokeWidth = a.getDimensionPixelSize(R.styleable.StarRatingBar_strokeWidth, mStrokeWidth);

        mTextSize = (int ) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize, getResources().getDisplayMetrics());
        mTextSize = a.getDimensionPixelSize(R.styleable.StarRatingBar_textSize, mTextSize);
        a.recycle();

        mTapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                for (int i = 0; i < mImages.size(); i++)
                {
                    mImages.get(i).clearAnimation();
                    mImages.get(i).setAlpha(0f);
                }
                rating = 0;
                removeView(mTextImgView);

                float x = e.getX();
                float y = e.getY();

                for (int i = 0; i < circularPaths.size(); i++)
                {
                    CircularPath path = circularPaths.get(i);
                    double distanceX = x - path.x;
                    double distanceY = y - path.y;

                    boolean isInside =  (distanceX * distanceX) + (distanceY * distanceY) <= path.r * path.r; // removing negative values
                    if (isInside)
                    {
                        Log.d(TAG, String.valueOf(path.value));
                        int position = path.value - 1;
                        createAnimation(position);
                        rating = position;

                        break;
                    }
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mTapDetector.onTouchEvent(event);
        return true;
    }

    /** Once any dynamic value is set. We need to call regenerate star for changes to take effect */
    public void regenerateStar()
    {
        addViews();
    }

    private void addViews()
    {
        mTextImgView = null;
        mImages.clear();
        circularPaths.clear();
        removeAllViews();

        if (getWidth() <= 0 || getHeight() <= 0)
            return;

        float mX = getWidth()/2;
        float mY = getHeight()/2;

        Paint paint = new Paint();
        paint.setTextSize(mTextSize);
        paint.setColor(mTextColor);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setAlpha(200);

        int angle = -18; // 54 Points is the base Difference angle sub by 72 the lobe angle arc

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < 5; i++)
        {
            int drawableResource = -1;
            if (i == 0)
                drawableResource = R.drawable.star_lobe_two;
            else if (i == 1)
                drawableResource = R.drawable.star_lobe_three;
            else if (i == 2)
                drawableResource = R.drawable.star_lobe_four;
            else if (i == 3)
                drawableResource = R.drawable.star_lobe_five;
            else if (i == 4)
                drawableResource = R.drawable.star_lobe_one;

            VectorDrawableCompat drawable = VectorDrawableCompat.create(getContext().getResources(), drawableResource, null);
            drawable.setBounds(0, 0, getWidth(), getHeight());

            VectorDrawableCompat.VFullPath path = (VectorDrawableCompat.VFullPath) drawable.getTargetByName("path");
            path.setFillColor(mFillColor);
            path.setStrokeColor(mStrokeColor);
            path.setStrokeWidth(mStrokeWidth);

            drawable.draw(canvas);

            /**
             *
             */

            // Separating the text to avoid overlapping on Other lobes causing the text of previous lobes to hide.
            float x = mX + ((float) Math.cos(Math.toRadians(angle)) * (mX * 0.5f)); //convert angle to radians for x and y coordinates
            float y = mY + ((float) Math.sin(Math.toRadians(angle)) * (mY * 0.5f));
            //canvas.drawLine(mX, mY, x, y, paint); //draw a line from center point back to the point

            CircularPath coords = new CircularPath();
            float xMax = Math.max(mX, x) - Math.min(mX, x);
            float yMax = Math.max(mY, y) - Math.min(mY, y);
            coords.r = Math.max(xMax, yMax) * 0.6f;
            coords.x = x;
            coords.y = y;
            coords.value = i+1;
            circularPaths.add(coords);

            String text = String.valueOf(i+1);

            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, 1, bounds);

            float textWidth  = bounds.width()/2;
            float textHeight = bounds.height()/2;

            float lobX = mX + ((float) Math.cos(Math.toRadians(angle)) * (mX * mTextPadding)); //convert angle to radians for x and y coordinates
            float lobY = mY + ((float) Math.sin(Math.toRadians(angle)) * (mY * mTextPadding));

            lobX = lobX - textWidth;
            lobY = lobY + textHeight;
            canvas.drawText(text, lobX, lobY, paint);

            angle = angle + 72; // 72 being base separation.

            /**
             *
             *
             *
             */

            VectorDrawableCompat mDrawable = VectorDrawableCompat.create(getContext().getResources(), drawableResource, null);
            mDrawable.setBounds(0, 0, getWidth(), getHeight());

            VectorDrawableCompat.VFullPath mPath = (VectorDrawableCompat.VFullPath) mDrawable.getTargetByName("path");
            mPath.setFillColor(mSelectedColor);

            ImageView selectedImgView = new ImageView(getContext());
            LayoutParams mParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            selectedImgView.setLayoutParams(mParams);
            selectedImgView.setImageDrawable(mDrawable);
            selectedImgView.setAlpha(0f);
            mImages.add(selectedImgView);
        }

        BitmapDrawable mainDrawable = new BitmapDrawable(getResources(), bitmap);

        ImageView imageView = new ImageView(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(params);
        imageView.setImageDrawable(mainDrawable);
        this.addView(imageView);

        for (int i = 0 ; i < mImages.size(); i++)
            addView(mImages.get(i));
    }

    private void createAnimation(final int position)
    {
        AnimatorSet mainAnimator = new AnimatorSet();
        ArrayList<Animator> animations = new ArrayList<>();

        for (int i = 0; i <= position; i++)
        {
            final ImageView imgView = mImages.get(i);

            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(imgView, "scaleX", 0f, 1.04f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(imgView, "scaleY", 0f, 1.04f);
            ObjectAnimator alphaScale = ObjectAnimator.ofFloat(imgView, "alpha", 0f, 1f);
            alphaScale.setDuration(5);

            scaleDownX.setDuration(mAnimDuration);
            scaleDownY.setDuration(mAnimDuration);

            AnimatorSet scaleDown = new AnimatorSet();
            scaleDown.setStartDelay(i * 50);
            scaleDown.setInterpolator(new AccelerateInterpolator());
            scaleDown.play(alphaScale).with(scaleDownX).with(scaleDownY);
            scaleDown.start();
            scaleDown.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);

                    ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(imgView, "scaleX", 1.04f, 1.0f);
                    ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(imgView, "scaleY", 1.04f, 1.0f);
                    scaleDownX.setDuration(90);
                    scaleDownY.setDuration(90);
                    AnimatorSet scaleDown = new AnimatorSet();
                    scaleDown.play(scaleDownX).with(scaleDownY);
                    scaleDown.start();
                }
            });

            animations.add(scaleDown);
        }

        mainAnimator.playTogether(animations);

        long mDuration = position * mAnimDuration;
        mDuration = mDuration/3;

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                addSelectedTextBitmap(position);
            }
        }, mDuration);
    }
    
    private void addSelectedTextBitmap(int position)
    {
        float mX = getWidth()/2;
        float mY = getHeight()/2;

        int angle = -18; // 54 Points is the base Difference angle sub by 72 the lobe angle arc

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < 5; i++)
        {
            Paint paint = new Paint();
            paint.setTextSize(mTextSize);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            String text = String.valueOf(i+1);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, 1, bounds);

            float textWidth  = bounds.width()/2;
            float textHeight = bounds.height()/2;

            float lobX = mX + ((float) Math.cos(Math.toRadians(angle)) * (mX * mTextPadding)); //convert angle to radians for x and y coordinates
            float lobY = mY + ((float) Math.sin(Math.toRadians(angle)) * (mY * mTextPadding));

            lobX = lobX - textWidth;
            lobY = lobY + textHeight;

            if (i <= position)
            {
                paint.setColor(mSelectedTextColor);
                paint.setAlpha(200);
            } 
            else
            {
                paint.setColor(mTextColor);
            }
            
            canvas.drawText(text, lobX, lobY, paint);

            angle = angle + 72; // 72 being base separation.
        }

        BitmapDrawable mainDrawable = new BitmapDrawable(getResources(), bitmap);

        mTextImgView = new ImageView(getContext());
        LayoutParams mParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mTextImgView.setLayoutParams(mParams);
        mTextImgView.setImageDrawable(mainDrawable);

        this.addView(mTextImgView);
    }
}
