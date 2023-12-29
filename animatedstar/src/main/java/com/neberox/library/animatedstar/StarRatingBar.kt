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
package com.neberox.library.animatedstar

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.FloatRange
import com.neberox.library.animatedstar.vector.VectorDrawableCompat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Created by Umar on 20/06/2018.
 */
class StarRatingBar : RelativeLayout {
    private inner class CircularPath {
        var x = 0f
        var y = 0f
        var r = 0f
        var value = 0
    }

    /* Initial fillColor used to create star lobes */
    private var mFillColor = Color.WHITE

    /* Selected Rating Color to fill the lobes */
    private var mSelectedColor = Color.parseColor("#f2b01e")

    /* The text size set in sp. Default is 18sp */
    private var mTextSize = 18

    /* Padding used to move Text around the lobe. Ranging from 0 to 1 */
    private var mTextPadding = 0.5f

    /* Initial text color to be drawn over the initial lobes. Default is gray */
    private var mTextColor = Color.LTGRAY

    /* Final text color to be drawn over the rated lobes. Default is white */
    private var mSelectedTextColor = Color.WHITE

    /* Stroke width around each lobe. The stroke will work only on initial generated lobes with 0 Rating */
    private var mStrokeWidth = 1

    /* Color of stroke. Default is Light Gray */
    private var mStrokeColor = Color.LTGRAY

    /* Animation duration of adding color to selected lobs. */
    private var mAnimDuration: Long = 200
    private var mTapDetector: GestureDetector? = null
    private val mImages = ArrayList<ImageView>()
    private var mTextImgView: ImageView? = null
    private val circularPaths = ArrayList<CircularPath>()
    private var rating = 0

    /**
     *
     * @param context Context
     */
    constructor(context: Context) : super(context) {
        setWillNotDraw(false)
        initAttributes(context, null)
        // We can not generate a bitmap with 0 width and height.
        post { addViews() }
    }

    /**
     *
     * @param context Context
     * @param attrs Attributes
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)
        initAttributes(context, attrs)
        // We can not generate a bitmap with 0 width and height.
        post { addViews() }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setWillNotDraw(false)
        initAttributes(context, attrs)
        // We can not generate a bitmap with 0 width and height.
        post { addViews() }
    }

    /**
     *
     * @param mFillColor Set the Initial Fill color for generating non rated lobes. Default is white.
     */
    fun setFillColor(mFillColor: Int) {
        this.mFillColor = mFillColor
    }

    /**
     *
     * @param mSelectedColor Set the Final Fill color for generating rated lobes. Default is #f2b01e
     */
    fun setSelectedColor(mSelectedColor: Int) {
        this.mSelectedColor = mSelectedColor
    }

    /**
     *
     * @param mTextSize Sets the text size of the rating points to be drawn on the lobes. Default is 18sp
     */
    fun setTextSize(mTextSize: Int) {
        this.mTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            mTextSize.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    /**
     *
     * @param mTextPadding Padding given to text to be placed inside lobe. Default is 0.5 (Middle of the lobe).
     * Range should be less or equal to 1.0f
     */
    fun setTextPadding(@FloatRange(from = 0.0, to = 1.0) mTextPadding: Float) {
        this.mTextPadding = mTextPadding
    }

    /**
     *
     * @param mTextColor Color of the text to be drawn over non rated lobes. Default is light gray
     */
    fun setTextColor(mTextColor: Int) {
        this.mTextColor = mTextColor
    }

    /**
     *
     * @param mSelectedTextColor Color of the text to be drawn over rated lobes. Default is white
     */
    fun setSelectedTextColor(mSelectedTextColor: Int) {
        this.mSelectedTextColor = mSelectedTextColor
    }

    /**
     *
     * @param mStrokeWidth  Stroke width size over the un rated lobes. Default is light gray.
     */
    fun setStrokeWidth(mStrokeWidth: Int) {
        this.mStrokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            mStrokeWidth.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    /**
     *
     * @param mStrokeColor Stroke color over the un rated lobes. Default is light gray.
     */
    fun setStrokeColor(mStrokeColor: Int) {
        this.mStrokeColor = mStrokeColor
    }

    /**
     *
     * @param mAnimDuration Animation duration over which rated lobes are generated. Default is 200 milliseconds
     */
    fun setAnimDuration(mAnimDuration: Long) {
        this.mAnimDuration = mAnimDuration
    }

    /**
     *
     * @param rating Set current rating
     */
    fun setRating(rating: Int) {
        this.rating = rating
        for (i in mImages.indices) {
            mImages[i].clearAnimation()
            mImages[i].alpha = 0f
        }
        removeView(mTextImgView)
        createAnimation(rating - 1)
    }

    fun getRating(): Int {
        return rating
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* We need to make sure that the View is a perfect Square */
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.StarRatingBar)
        mFillColor = a.getColor(R.styleable.StarRatingBar_fillColor, mFillColor)
        mSelectedColor = a.getColor(R.styleable.StarRatingBar_selectedColor, mSelectedColor)
        mStrokeColor = a.getColor(R.styleable.StarRatingBar_strokeColor, mStrokeColor)
        mTextColor = a.getColor(R.styleable.StarRatingBar_textColor, mTextColor)
        mSelectedTextColor =
            a.getColor(R.styleable.StarRatingBar_selectedTextColor, mSelectedTextColor)
        mTextPadding = a.getFloat(R.styleable.StarRatingBar_textPadding, mTextPadding)
        if (mTextPadding > 1) mTextPadding = 0.5f else if (mTextPadding < 0) mTextPadding = 0.5f
        mStrokeWidth = a.getDimensionPixelSize(R.styleable.StarRatingBar_strokeWidth, mStrokeWidth)
        mTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            mTextSize.toFloat(),
            resources.displayMetrics
        ).toInt()
        mTextSize = a.getDimensionPixelSize(R.styleable.StarRatingBar_textSize, mTextSize)
        a.recycle()
        mTapDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                for (i in mImages.indices) {
                    mImages[i].clearAnimation()
                    mImages[i].alpha = 0f
                }
                rating = 0
                removeView(mTextImgView)
                val x = e.x
                val y = e.y
                for (i in circularPaths.indices) {
                    val path = circularPaths[i]
                    val distanceX = (x - path.x).toDouble()
                    val distanceY = (y - path.y).toDouble()
                    val isInside =
                        distanceX * distanceX + distanceY * distanceY <= path.r * path.r // removing negative values
                    if (isInside) {
                        Log.d(TAG, path.value.toString())
                        val position = path.value - 1
                        createAnimation(position)
                        rating = position
                        break
                    }
                }
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mTapDetector!!.onTouchEvent(event)
        return true
    }

    /** Once any dynamic value is set. We need to call regenerate star for changes to take effect  */
    fun regenerateStar() {
        addViews()
    }

    private fun addViews() {
        mTextImgView = null
        mImages.clear()
        circularPaths.clear()
        removeAllViews()
        if (width <= 0 || height <= 0) return
        val mX = (width / 2).toFloat()
        val mY = (height / 2).toFloat()
        val paint = Paint()
        paint.textSize = mTextSize.toFloat()
        paint.color = mTextColor
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        paint.alpha = 200
        var angle = -18 // 54 Points is the base Difference angle sub by 72 the lobe angle arc
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        for (i in 0..4) {
            var drawableResource = -1
            if (i == 0) drawableResource =
                R.drawable.star_lobe_two else if (i == 1) drawableResource =
                R.drawable.star_lobe_three else if (i == 2) drawableResource =
                R.drawable.star_lobe_four else if (i == 3) drawableResource =
                R.drawable.star_lobe_five else drawableResource =
                R.drawable.star_lobe_one
            val drawable: VectorDrawableCompat = VectorDrawableCompat.create(
                context.resources, drawableResource, null
            )!!
            drawable.setBounds(0, 0, width, height)
            val path = drawable.getTargetByName("path") as VectorDrawableCompat.VFullPath
            path.fillColor = mFillColor
            path.strokeColor = mStrokeColor
            path.strokeWidth = mStrokeWidth.toFloat()
            drawable.draw(canvas)
            /**
             *
             */

            // Separating the text to avoid overlapping on Other lobes causing the text of previous lobes to hide.
            val x = mX + Math.cos(Math.toRadians(angle.toDouble()))
                .toFloat() * (mX * 0.5f) //convert angle to radians for x and y coordinates
            val y = mY + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * (mY * 0.5f)
            //canvas.drawLine(mX, mY, x, y, paint); //draw a line from center point back to the point
            val coords = CircularPath()
            val xMax = max(mX, x) - min(mX, x)
            val yMax = max(mY, y) - min(mY, y)
            coords.r = max(xMax, yMax) * 0.6f
            coords.x = x
            coords.y = y
            coords.value = i + 1
            circularPaths.add(coords)
            val text = (i + 1).toString()
            val bounds = Rect()
            paint.getTextBounds(text, 0, 1, bounds)
            val textWidth = (bounds.width() / 2).toFloat()
            val textHeight = (bounds.height() / 2).toFloat()
            var lobX = mX + cos(Math.toRadians(angle.toDouble()))
                .toFloat() * (mX * mTextPadding) //convert angle to radians for x and y coordinates
            var lobY =
                mY + sin(Math.toRadians(angle.toDouble())).toFloat() * (mY * mTextPadding)
            lobX -= textWidth
            lobY += textHeight
            canvas.drawText(text, lobX, lobY, paint)
            angle += 72 // 72 being base separation.
            /**
             *
             *
             *
             */
            val mDrawable: VectorDrawableCompat = VectorDrawableCompat.Companion.create(
                context.resources, drawableResource, null
            )!!
            mDrawable.setBounds(0, 0, width, height)
            val mPath = mDrawable.getTargetByName("path") as VectorDrawableCompat.VFullPath
            mPath.fillColor = mSelectedColor
            val selectedImgView = ImageView(context)
            val mParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            selectedImgView.layoutParams = mParams
            selectedImgView.setImageDrawable(mDrawable)
            selectedImgView.alpha = 0f
            mImages.add(selectedImgView)
        }
        val mainDrawable = BitmapDrawable(resources, bitmap)
        val imageView = ImageView(context)
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        imageView.layoutParams = params
        imageView.setImageDrawable(mainDrawable)
        this.addView(imageView)
        for (i in mImages.indices) addView(mImages[i])
    }

    private fun createAnimation(position: Int) {
        val mainAnimator = AnimatorSet()
        val animations = ArrayList<Animator>()
        for (i in 0..position) {
            val imgView = mImages[i]
            val scaleDownX = ObjectAnimator.ofFloat(imgView, "scaleX", 0f, 1.04f)
            val scaleDownY = ObjectAnimator.ofFloat(imgView, "scaleY", 0f, 1.04f)
            val alphaScale = ObjectAnimator.ofFloat(imgView, "alpha", 0f, 1f)
            alphaScale.setDuration(5)
            scaleDownX.setDuration(mAnimDuration)
            scaleDownY.setDuration(mAnimDuration)
            val scaleDown = AnimatorSet()
            scaleDown.startDelay = (i * 50).toLong()
            scaleDown.interpolator = AccelerateInterpolator()
            scaleDown.play(alphaScale).with(scaleDownX).with(scaleDownY)
            scaleDown.start()
            scaleDown.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    val scaleDownX = ObjectAnimator.ofFloat(imgView, "scaleX", 1.04f, 1.0f)
                    val scaleDownY = ObjectAnimator.ofFloat(imgView, "scaleY", 1.04f, 1.0f)
                    scaleDownX.setDuration(90)
                    scaleDownY.setDuration(90)
                    val scaleDown = AnimatorSet()
                    scaleDown.play(scaleDownX).with(scaleDownY)
                    scaleDown.start()
                }
            })
            animations.add(scaleDown)
        }
        mainAnimator.playTogether(animations)
        var mDuration = position * mAnimDuration
        mDuration /= 3
        Handler().postDelayed({ addSelectedTextBitmap(position) }, mDuration)
    }

    private fun addSelectedTextBitmap(position: Int) {
        val mX = (width / 2).toFloat()
        val mY = (height / 2).toFloat()
        var angle = -18 // 54 Points is the base Difference angle sub by 72 the lobe angle arc
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        for (i in 0..4) {
            val paint = Paint()
            paint.textSize = mTextSize.toFloat()
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            val text = (i + 1).toString()
            val bounds = Rect()
            paint.getTextBounds(text, 0, 1, bounds)
            val textWidth = (bounds.width() / 2).toFloat()
            val textHeight = (bounds.height() / 2).toFloat()
            var lobX = mX + Math.cos(Math.toRadians(angle.toDouble()))
                .toFloat() * (mX * mTextPadding) //convert angle to radians for x and y coordinates
            var lobY =
                mY + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * (mY * mTextPadding)
            lobX = lobX - textWidth
            lobY = lobY + textHeight
            if (i <= position) {
                paint.color = mSelectedTextColor
                paint.alpha = 200
            } else {
                paint.color = mTextColor
            }
            canvas.drawText(text, lobX, lobY, paint)
            angle = angle + 72 // 72 being base separation.
        }
        val mainDrawable = BitmapDrawable(resources, bitmap)
        mTextImgView = ImageView(context)
        val mParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mTextImgView!!.layoutParams = mParams
        mTextImgView!!.setImageDrawable(mainDrawable)
        this.addView(mTextImgView)
    }

    companion object {
        private const val TAG = "DiamondRatingBar"
    }
}
