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
package com.neberox.library.animatedstar.vector

/**
 * Created by Umar on 20/06/2018.
 */
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.LayoutDirection
import android.util.Log
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.collection.ArrayMap
import androidx.core.graphics.drawable.DrawableCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Stack
import kotlin.math.min

class VectorDrawableCompat : VectorDrawableCommon {
    private var mVectorState: VectorDrawableCompatState?
    private var mTintFilter: PorterDuffColorFilter? = null
    private var mColorFilter: ColorFilter? = null
    private var mMutated = false

    // AnimatedVectorDrawable needs to turn off the cache all the time, otherwise,
    // caching the bitmap by default is allowed.
    private var mAllowCaching = true

    // The Constant state associated with the <code>mDelegateDrawable</code>.
    private val mCachedConstantStateDelegate: ConstantState? = null

    // Temp variable, only for saving "new" operation at the draw() time.
    private val mTmpFloats = FloatArray(9)
    private val mTmpMatrix = Matrix()
    private val mTmpBounds = Rect()

    internal constructor() {
        mVectorState = VectorDrawableCompatState()
    }

    private constructor(state: VectorDrawableCompatState) {
        mVectorState = state
        mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode)
    }

    override fun mutate(): Drawable {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.mutate()
            return this
        }
        if (!mMutated && super.mutate() === this) {
            mVectorState = VectorDrawableCompatState(mVectorState)
            mMutated = true
        }
        return this
    }

    fun getTargetByName(name: String?): Any? {
        return mVectorState!!.mVPathRenderer!!.mVGTargetsMap[name]
    }

    override fun getConstantState(): ConstantState? {
        if (mDelegateDrawable != null) {
            // Such that the configuration can be refreshed.
            return VectorDrawableDelegateState(
                mDelegateDrawable!!.constantState
            )
        }
        mVectorState!!.mChangingConfigurations = changingConfigurations
        return mVectorState
    }

    override fun draw(canvas: Canvas) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.draw(canvas)
            return
        }
        // We will offset the bounds for drawBitmap, so copyBounds() here instead
        // of getBounds().
        copyBounds(mTmpBounds)
        if (mTmpBounds.width() <= 0 || mTmpBounds.height() <= 0) {
            // Nothing to draw
            return
        }

        // Color filters always override tint filters.
        val colorFilter = if (mColorFilter == null) mTintFilter else mColorFilter

        // The imageView can scale the canvas in different ways, in order to
        // avoid blurry scaling, we have to draw into a bitmap with exact pixel
        // size first. This bitmap size is determined by the bounds and the
        // canvas scale.
        canvas.getMatrix(mTmpMatrix)
        mTmpMatrix.getValues(mTmpFloats)
        var canvasScaleX = Math.abs(mTmpFloats[Matrix.MSCALE_X])
        var canvasScaleY = Math.abs(mTmpFloats[Matrix.MSCALE_Y])
        val canvasSkewX = Math.abs(mTmpFloats[Matrix.MSKEW_X])
        val canvasSkewY = Math.abs(mTmpFloats[Matrix.MSKEW_Y])

        // When there is any rotation / skew, then the scale value is not valid.
        if (canvasSkewX != 0f || canvasSkewY != 0f) {
            canvasScaleX = 1.0f
            canvasScaleY = 1.0f
        }
        var scaledWidth = (mTmpBounds.width() * canvasScaleX).toInt()
        var scaledHeight = (mTmpBounds.height() * canvasScaleY).toInt()
        scaledWidth = Math.min(MAX_CACHED_BITMAP_SIZE, scaledWidth)
        scaledHeight = Math.min(MAX_CACHED_BITMAP_SIZE, scaledHeight)
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return
        }
        val saveCount = canvas.save()
        canvas.translate(mTmpBounds.left.toFloat(), mTmpBounds.top.toFloat())

        // Handle RTL mirroring.
        val needMirroring = needMirroring()
        if (needMirroring) {
            canvas.translate(mTmpBounds.width().toFloat(), 0f)
            canvas.scale(-1.0f, 1.0f)
        }

        // At this point, canvas has been translated to the right position.
        // And we use this bound for the destination rect for the drawBitmap, so
        // we offset to (0, 0);
        mTmpBounds.offsetTo(0, 0)
        mVectorState!!.createCachedBitmapIfNeeded(scaledWidth, scaledHeight)
        if (!mAllowCaching) {
            mVectorState!!.updateCachedBitmap(scaledWidth, scaledHeight)
        } else {
            if (!mVectorState!!.canReuseCache()) {
                mVectorState!!.updateCachedBitmap(scaledWidth, scaledHeight)
                mVectorState!!.updateCacheStates()
            }
        }
        mVectorState!!.drawCachedBitmapWithRootAlpha(canvas, colorFilter, mTmpBounds)
        canvas.restoreToCount(saveCount)
    }

    override fun getAlpha(): Int {
        return if (mDelegateDrawable != null) {
            DrawableCompat.getAlpha(mDelegateDrawable!!)
        } else mVectorState!!.mVPathRenderer?.rootAlpha ?: 0
    }

    override fun setAlpha(alpha: Int) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.alpha = alpha
            return
        }

        mVectorState?.let {
            it.mVPathRenderer?.let { mVPathRenderer ->
                if (mVPathRenderer.rootAlpha != alpha) {
                    mVPathRenderer.rootAlpha = alpha
                    invalidateSelf()
                }
            }
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.colorFilter = colorFilter
            return
        }
        mColorFilter = colorFilter
        invalidateSelf()
    }

    /**
     * Ensures the tint filter is consistent with the current tint color and
     * mode.
     */
    fun updateTintFilter(
        tintFilter: PorterDuffColorFilter?, tint: ColorStateList?,
        tintMode: PorterDuff.Mode?
    ): PorterDuffColorFilter? {
        if (tint == null || tintMode == null) {
            return null
        }
        // setMode, setColor of PorterDuffColorFilter are not public method in SDK v7.
        // Therefore we create a new one all the time here. Don't expect this is called often.
        val color: Int = tint.getColorForState(state, Color.TRANSPARENT)
        return PorterDuffColorFilter(color, tintMode)
    }

    @SuppressLint("NewApi")
    override fun setTint(tint: Int) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTint(mDelegateDrawable!!, tint)
            return
        }
        setTintList(ColorStateList.valueOf(tint))
    }

    override fun setTintList(tint: ColorStateList?) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintList(mDelegateDrawable!!, tint)
            return
        }
        val state = mVectorState
        if (state!!.mTint !== tint) {
            state!!.mTint = tint
            mTintFilter = updateTintFilter(mTintFilter, tint, state.mTintMode)
            invalidateSelf()
        }
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintMode(mDelegateDrawable!!, tintMode)
            return
        }
        val state = mVectorState
        if (state!!.mTintMode != tintMode) {
            state.mTintMode = tintMode
            mTintFilter = updateTintFilter(mTintFilter, state.mTint, tintMode)
            invalidateSelf()
        }
    }

    override fun isStateful(): Boolean {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.isStateful
        } else super.isStateful() || mVectorState != null && mVectorState!!.mTint != null && mVectorState!!.mTint!!.isStateful()
    }

    override fun onStateChange(stateSet: IntArray): Boolean {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable!!.setState(stateSet)
        }
        val state = mVectorState
        if (state!!.mTint != null && state.mTintMode != null) {
            mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode)
            invalidateSelf()
            return true
        }
        return false
    }

    override fun getOpacity(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.opacity
        } else PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.intrinsicWidth
        } else mVectorState!!.mVPathRenderer!!.mBaseWidth.toInt()
    }

    override fun getIntrinsicHeight(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.intrinsicHeight
        } else mVectorState!!.mVPathRenderer!!.mBaseHeight.toInt()
    }

    // Don't support re-applying themes. The initial theme loading is working.
    override fun canApplyTheme(): Boolean {
        if (mDelegateDrawable != null) {
            DrawableCompat.canApplyTheme(mDelegateDrawable!!)
        }
        return false
    }

    override fun isAutoMirrored(): Boolean {
        return if (mDelegateDrawable != null) {
            DrawableCompat.isAutoMirrored(mDelegateDrawable!!)
        } else mVectorState!!.mAutoMirrored
    }

    override fun setAutoMirrored(mirrored: Boolean) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setAutoMirrored(mDelegateDrawable!!, mirrored)
            return
        }
        mVectorState!!.mAutoMirrored = mirrored
    }

    private val pixelSize: Float
        /**
         * The size of a pixel when scaled from the intrinsic dimension to the viewport dimension. This
         * is used to calculate the path animation accuracy.
         *
         * @hide
         */
        get() {
            if (mVectorState == null && mVectorState!!.mVPathRenderer == null || mVectorState!!.mVPathRenderer!!.mBaseWidth == 0f || mVectorState!!.mVPathRenderer!!.mBaseHeight == 0f || mVectorState!!.mVPathRenderer!!.mViewportHeight == 0f || mVectorState!!.mVPathRenderer!!.mViewportWidth == 0f) {
                return 1.0f // fall back to 1:1 pixel mapping.
            }
            val intrinsicWidth = mVectorState!!.mVPathRenderer!!.mBaseWidth
            val intrinsicHeight = mVectorState!!.mVPathRenderer!!.mBaseHeight
            val viewportWidth = mVectorState!!.mVPathRenderer!!.mViewportWidth
            val viewportHeight = mVectorState!!.mVPathRenderer!!.mViewportHeight
            val scaleX = viewportWidth / intrinsicWidth
            val scaleY = viewportHeight / intrinsicHeight
            return min(scaleX, scaleY)
        }

    @SuppressLint("NewApi")
    @Throws(XmlPullParserException::class, IOException::class)
    override fun inflate(res: Resources, parser: XmlPullParser, attrs: AttributeSet) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.inflate(res, parser, attrs)
            return
        }
        inflate(res, parser, attrs, null)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun inflate(
        res: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Theme?
    ) {
        if (mDelegateDrawable != null) {
            DrawableCompat.inflate(mDelegateDrawable!!, res, parser, attrs, theme)
            return
        }
        val state = mVectorState
        val pathRenderer = VPathRenderer()
        state!!.mVPathRenderer = pathRenderer
        val a: TypedArray = obtainAttributes(
            res, theme, attrs,
            AndroidResources.styleable_VectorDrawableTypeArray
        )
        updateStateFromTypedArray(a, parser)
        a.recycle()
        state.mChangingConfigurations = changingConfigurations
        state.mCacheDirty = true
        inflateInternal(res, parser, attrs, theme)
        mTintFilter = updateTintFilter(mTintFilter, state.mTint, state.mTintMode)
    }

    @Throws(XmlPullParserException::class)
    private fun updateStateFromTypedArray(a: TypedArray, parser: XmlPullParser) {
        val state = mVectorState
        val pathRenderer = state!!.mVPathRenderer

        // Account for any configuration changes.
        // state.mChangingConfigurations |= Utils.getChangingConfigurations(a);
        val mode = TypedArrayUtils.getNamedInt(
            a, parser, "tintMode",
            AndroidResources.styleable_VectorDrawable_tintMode, -1
        )
        state.mTintMode = parseTintModeCompat(mode, PorterDuff.Mode.SRC_IN)
        val tint: ColorStateList? =
            a.getColorStateList(AndroidResources.styleable_VectorDrawable_tint)
        if (tint != null) {
            state.mTint = tint
        }
        state.mAutoMirrored = TypedArrayUtils.getNamedBoolean(
            a, parser, "autoMirrored",
            AndroidResources.styleable_VectorDrawable_autoMirrored, state.mAutoMirrored
        )
        pathRenderer!!.mViewportWidth = TypedArrayUtils.getNamedFloat(
            a, parser, "viewportWidth",
            AndroidResources.styleable_VectorDrawable_viewportWidth,
            pathRenderer.mViewportWidth
        )
        pathRenderer.mViewportHeight = TypedArrayUtils.getNamedFloat(
            a, parser, "viewportHeight",
            AndroidResources.styleable_VectorDrawable_viewportHeight,
            pathRenderer.mViewportHeight
        )
        if (pathRenderer.mViewportWidth <= 0) {
            throw XmlPullParserException(
                a.getPositionDescription() +
                        "<vector> tag requires viewportWidth > 0"
            )
        } else if (pathRenderer.mViewportHeight <= 0) {
            throw XmlPullParserException(
                a.getPositionDescription() +
                        "<vector> tag requires viewportHeight > 0"
            )
        }
        pathRenderer.mBaseWidth = a.getDimension(
            AndroidResources.styleable_VectorDrawable_width, pathRenderer.mBaseWidth
        )
        pathRenderer.mBaseHeight = a.getDimension(
            AndroidResources.styleable_VectorDrawable_height, pathRenderer.mBaseHeight
        )
        if (pathRenderer.mBaseWidth <= 0) {
            throw XmlPullParserException(
                a.getPositionDescription() +
                        "<vector> tag requires width > 0"
            )
        } else if (pathRenderer.mBaseHeight <= 0) {
            throw XmlPullParserException(
                a.getPositionDescription() +
                        "<vector> tag requires height > 0"
            )
        }

        // shown up from API 11.
        val alphaInFloat = TypedArrayUtils.getNamedFloat(
            a, parser, "alpha",
            AndroidResources.styleable_VectorDrawable_alpha, pathRenderer.alpha
        )
        pathRenderer.alpha = (alphaInFloat)
        val name: String? = a.getString(AndroidResources.styleable_VectorDrawable_name)
        if (name != null) {
            pathRenderer.mRootName = name
            pathRenderer.mVGTargetsMap.put(name, pathRenderer)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun inflateInternal(
        res: Resources, parser: XmlPullParser, attrs: AttributeSet,
        theme: Theme?
    ) {
        val state = mVectorState
        val pathRenderer = state!!.mVPathRenderer
        var noPathTag = true

        // Use a stack to help to build the group tree.
        // The top of the stack is always the current group.
        val groupStack = Stack<VGroup>()
        groupStack.push(pathRenderer!!.mRootGroup)
        var eventType = parser.eventType
        val innerDepth = parser.depth + 1

        // Parse everything until the end of the vector element.
        while (eventType != XmlPullParser.END_DOCUMENT
            && (parser.depth >= innerDepth || eventType != XmlPullParser.END_TAG)
        ) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name
                val currentGroup = groupStack.peek()
                if (SHAPE_PATH == tagName) {
                    val path = VFullPath()
                    path.inflate(res, attrs, theme, parser)
                    currentGroup.mChildren.add(path)
                    if (path.pathName != null) {
                        pathRenderer.mVGTargetsMap.put(path.pathName, path)
                    }
                    noPathTag = false
                    state.mChangingConfigurations =
                        state.mChangingConfigurations or path.mChangingConfigurations
                } else if (SHAPE_CLIP_PATH == tagName) {
                    val path = VClipPath()
                    path.inflate(res, attrs, theme, parser)
                    currentGroup.mChildren.add(path)
                    if (path.pathName != null) {
                        pathRenderer.mVGTargetsMap.put(path.pathName, path)
                    }
                    state.mChangingConfigurations =
                        state.mChangingConfigurations or path.mChangingConfigurations
                } else if (SHAPE_GROUP == tagName) {
                    val newChildGroup = VGroup()
                    newChildGroup.inflate(res, attrs, theme, parser)
                    currentGroup.mChildren.add(newChildGroup)
                    groupStack.push(newChildGroup)
                    if (newChildGroup.groupName != null) {
                        pathRenderer.mVGTargetsMap.put(
                            newChildGroup.groupName,
                            newChildGroup
                        )
                    }
                    state.mChangingConfigurations =
                        state.mChangingConfigurations or newChildGroup.mChangingConfigurations
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tagName = parser.name
                if (SHAPE_GROUP == tagName) {
                    groupStack.pop()
                }
            }
            eventType = parser.next()
        }

        // Print the tree out for debug.
        if (DBG_VECTOR_DRAWABLE) {
            printGroupTree(pathRenderer.mRootGroup, 0)
        }
        if (noPathTag) {
            val tag = StringBuffer()
            if (tag.length > 0) {
                tag.append(" or ")
            }
            tag.append(SHAPE_PATH)
            throw XmlPullParserException("no $tag defined")
        }
    }

    private fun printGroupTree(currentGroup: VGroup, level: Int) {
        var indent = ""
        for (i in 0 until level) {
            indent += "    "
        }
        // Print the current node
        Log.v(
            LOGTAG, indent + "current group is :" + currentGroup.groupName
                    + " rotation is " + currentGroup.mRotate
        )
        Log.v(LOGTAG, indent + "matrix is :" + currentGroup.localMatrix.toString())
        // Then print all the children groups
        for (i in currentGroup.mChildren.indices) {
            val child = currentGroup.mChildren[i]
            if (child is VGroup) {
                printGroupTree(child, level + 1)
            } else {
                (child as VPath).printVPath(level + 1)
            }
        }
    }

    fun setAllowCaching(allowCaching: Boolean) {
        mAllowCaching = allowCaching
    }

    // We don't support RTL auto mirroring since the getLayoutDirection() is for API 17+.
    @SuppressLint("NewApi", "WrongConstant")
    private fun needMirroring(): Boolean {
        return if (Build.VERSION.SDK_INT < 17) {
            false
        } else {
            isAutoMirrored && layoutDirection == LayoutDirection.RTL
        }
    }

    // Extra override functions for delegation for SDK >= 7.
    override fun onBoundsChange(bounds: Rect) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.bounds = bounds
        }
    }

    override fun getChangingConfigurations(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.changingConfigurations
        } else super.getChangingConfigurations() or mVectorState!!.changingConfigurations
    }

    override fun invalidateSelf() {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.invalidateSelf()
            return
        }
        super.invalidateSelf()
    }

    override fun scheduleSelf(what: Runnable, `when`: Long) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.scheduleSelf(what, `when`)
            return
        }
        super.scheduleSelf(what, `when`)
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.setVisible(visible, restart)
        } else super.setVisible(visible, restart)
    }

    override fun unscheduleSelf(what: Runnable) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.unscheduleSelf(what)
            return
        }
        super.unscheduleSelf(what)
    }

    /**
     * Constant state for delegating the creating drawable job for SDK >= 24.
     * Instead of creating a VectorDrawable, create a VectorDrawableCompat instance which contains
     * a delegated VectorDrawable instance.
     */
    private class VectorDrawableDelegateState(private val mDelegateState: ConstantState?) :
        ConstantState() {
        override fun newDrawable(): Drawable {
            val drawableCompat = VectorDrawableCompat()
            drawableCompat.mDelegateDrawable = mDelegateState!!.newDrawable() as VectorDrawable
            return drawableCompat
        }

        override fun newDrawable(res: Resources?): Drawable {
            val drawableCompat = VectorDrawableCompat()
            drawableCompat.mDelegateDrawable = mDelegateState!!.newDrawable(res) as VectorDrawable
            return drawableCompat
        }

        override fun newDrawable(res: Resources?, theme: Theme?): Drawable {
            val drawableCompat = VectorDrawableCompat()
            drawableCompat.mDelegateDrawable =
                mDelegateState!!.newDrawable(res, theme) as VectorDrawable
            return drawableCompat
        }

        override fun canApplyTheme(): Boolean {
            return mDelegateState!!.canApplyTheme()
        }

        override fun getChangingConfigurations(): Int {
            return mDelegateState!!.changingConfigurations
        }
    }

    private class VectorDrawableCompatState : ConstantState {
        var mChangingConfigurations = 0
        var mVPathRenderer: VPathRenderer? = null
        var mTint: ColorStateList? = null
        var mTintMode: PorterDuff.Mode? = DEFAULT_TINT_MODE
        var mAutoMirrored = false
        var mCachedBitmap: Bitmap? = null

        var mCachedTint: ColorStateList? = null
        var mCachedTintMode: PorterDuff.Mode? = null
        var mCachedRootAlpha = 0
        var mCachedAutoMirrored = false
        var mCacheDirty = false

        /**
         * Temporary paint object used to draw cached bitmaps.
         */
        var mTempPaint: Paint? = null

        // Deep copy for mutate() or implicitly mutate.
        constructor(copy: VectorDrawableCompatState?) {
            if (copy != null) {
                mChangingConfigurations = copy.mChangingConfigurations
                mVPathRenderer = VPathRenderer(copy.mVPathRenderer)
                if (copy.mVPathRenderer!!.mFillPaint != null) {
                    mVPathRenderer!!.mFillPaint = Paint(copy.mVPathRenderer!!.mFillPaint)
                }
                if (copy.mVPathRenderer!!.mStrokePaint != null) {
                    mVPathRenderer!!.mStrokePaint = Paint(copy.mVPathRenderer!!.mStrokePaint)
                }
                mTint = copy.mTint
                mTintMode = copy.mTintMode
                mAutoMirrored = copy.mAutoMirrored
            }
        }

        fun drawCachedBitmapWithRootAlpha(
            canvas: Canvas, filter: ColorFilter?,
            originalBounds: Rect?
        ) {
            // The bitmap's size is the same as the bounds.
            val p = getPaint(filter)
            canvas.drawBitmap(mCachedBitmap!!, null, originalBounds!!, p)
        }

        fun hasTranslucentRoot(): Boolean {
            return mVPathRenderer?.rootAlpha ?: 255 < 255
        }

        /**
         * @return null when there is no need for alpha paint.
         */
        fun getPaint(filter: ColorFilter?): Paint? {
            if (!hasTranslucentRoot() && filter == null) {
                return null
            }
            if (mTempPaint == null) {
                mTempPaint = Paint()
                mTempPaint!!.isFilterBitmap = true
            }
            mTempPaint!!.alpha = mVPathRenderer?.rootAlpha ?: 255
            mTempPaint!!.setColorFilter(filter)
            return mTempPaint
        }

        fun updateCachedBitmap(width: Int, height: Int) {
            mCachedBitmap!!.eraseColor(Color.TRANSPARENT)
            val tmpCanvas = Canvas(mCachedBitmap!!)
            mVPathRenderer!!.draw(tmpCanvas, width, height, null)
        }

        fun createCachedBitmapIfNeeded(width: Int, height: Int) {
            if (mCachedBitmap == null || !canReuseBitmap(width, height)) {
                mCachedBitmap = Bitmap.createBitmap(
                    width, height,
                    Bitmap.Config.ARGB_8888
                )
                mCacheDirty = true
            }
        }

        fun canReuseBitmap(width: Int, height: Int): Boolean {
            return if (width == mCachedBitmap!!.width
                && height == mCachedBitmap!!.height
            ) {
                true
            } else false
        }

        fun canReuseCache(): Boolean {
            return !mCacheDirty && mCachedTint === mTint && mCachedTintMode == mTintMode && mCachedAutoMirrored == mAutoMirrored && mCachedRootAlpha == mVPathRenderer?.rootAlpha
        }

        fun updateCacheStates() {
            // Use shallow copy here and shallow comparison in canReuseCache(),
            // likely hit cache miss more, but practically not much difference.
            mCachedTint = mTint
            mCachedTintMode = mTintMode
            mCachedRootAlpha = mVPathRenderer?.rootAlpha ?: 255
            mCachedAutoMirrored = mAutoMirrored
            mCacheDirty = false
        }

        constructor() {
            mVPathRenderer = VPathRenderer()
        }

        override fun newDrawable(): Drawable {
            return VectorDrawableCompat(this)
        }

        override fun newDrawable(res: Resources?): Drawable {
            return VectorDrawableCompat(this)
        }

        override fun getChangingConfigurations(): Int {
            return mChangingConfigurations
        }
    }

    private class VPathRenderer {
        /* Right now the internal data structure is organized as a tree.
         * Each node can be a group node, or a path.
         * A group node can have groups or paths as children, but a path node has
         * no children.
         * One example can be:
         *                 Root Group
         *                /    |     \
         *           Group    Path    Group
         *          /     \             |
         *         Path   Path         Path
         *
         */
        // Variables that only used temporarily inside the draw() call, so there
        // is no need for deep copying.
        private val mPath: Path
        private val mRenderPath: Path
        private val mFinalPathMatrix = Matrix()
        var mStrokePaint: Paint? = null
        var mFillPaint: Paint? = null
        private var mPathMeasure: PathMeasure? = null

        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        private var mChangingConfigurations = 0
        val mRootGroup: VGroup
        var mBaseWidth = 0f
        var mBaseHeight = 0f
        var mViewportWidth = 0f
        var mViewportHeight = 0f
        var rootAlpha = 0xFF
        var mRootName: String? = null
        val mVGTargetsMap: ArrayMap<String?, Any?> = ArrayMap<String?, Any?>()

        constructor() {
            mRootGroup = VGroup()
            mPath = Path()
            mRenderPath = Path()
        }

        @get:Suppress("unused")
        var alpha: Float
            get() = rootAlpha / 255.0f
            // setAlpha() and getAlpha() are used mostly for animation purpose, since
            set(alpha) {
                rootAlpha = ((alpha * 255).toInt())
            }

        constructor(copy: VPathRenderer?) {
            mRootGroup = VGroup(copy!!.mRootGroup, mVGTargetsMap)
            mPath = Path(copy.mPath)
            mRenderPath = Path(copy.mRenderPath)
            mBaseWidth = copy.mBaseWidth
            mBaseHeight = copy.mBaseHeight
            mViewportWidth = copy.mViewportWidth
            mViewportHeight = copy.mViewportHeight
            mChangingConfigurations = copy.mChangingConfigurations
            rootAlpha = copy.rootAlpha
            mRootName = copy.mRootName
            if (copy.mRootName != null) {
                mVGTargetsMap.put(copy.mRootName, this)
            }
        }

        private fun drawGroupTree(
            currentGroup: VGroup, currentMatrix: Matrix,
            canvas: Canvas, w: Int, h: Int, filter: ColorFilter?
        ) {
            // Calculate current group's matrix by preConcat the parent's and
            // and the current one on the top of the stack.
            // Basically the Mfinal = Mviewport * M0 * M1 * M2;
            // Mi the local matrix at level i of the group tree.
            currentGroup.mStackedMatrix.set(currentMatrix)
            currentGroup.mStackedMatrix.preConcat(currentGroup.localMatrix)

            // Save the current clip information, which is local to this group.
            canvas.save()

            // Draw the group tree in the same order as the XML file.
            for (i in currentGroup.mChildren.indices) {
                val child = currentGroup.mChildren[i]
                if (child is VGroup) {
                    drawGroupTree(
                        child, currentGroup.mStackedMatrix,
                        canvas, w, h, filter
                    )
                } else if (child is VPath) {
                    drawPath(currentGroup, child, canvas, w, h, filter)
                }
            }
            canvas.restore()
        }

        fun draw(canvas: Canvas, w: Int, h: Int, filter: ColorFilter?) {
            // Traverse the tree in pre-order to draw.
            drawGroupTree(mRootGroup, IDENTITY_MATRIX, canvas, w, h, filter)
        }

        private fun drawPath(
            vGroup: VGroup, vPath: VPath, canvas: Canvas, w: Int, h: Int,
            filter: ColorFilter?
        ) {
            val scaleX = w / mViewportWidth
            val scaleY = h / mViewportHeight
            val minScale = Math.min(scaleX, scaleY)
            val groupStackedMatrix = vGroup.mStackedMatrix
            mFinalPathMatrix.set(groupStackedMatrix)
            mFinalPathMatrix.postScale(scaleX, scaleY)
            val matrixScale = getMatrixScale(groupStackedMatrix)
            if (matrixScale == 0f) {
                // When either x or y is scaled to 0, we don't need to draw anything.
                return
            }
            vPath.toPath(mPath)
            val path = mPath
            mRenderPath.reset()
            if (vPath.isClipPath) {
                mRenderPath.addPath(path, mFinalPathMatrix)
                canvas.clipPath(mRenderPath)
            } else {
                val fullPath = vPath as VFullPath
                if (fullPath.trimPathStart != 0.0f || fullPath.trimPathEnd != 1.0f) {
                    var start = (fullPath.trimPathStart + fullPath.trimPathOffset) % 1.0f
                    var end = (fullPath.trimPathEnd + fullPath.trimPathOffset) % 1.0f
                    if (mPathMeasure == null) {
                        mPathMeasure = PathMeasure()
                    }
                    mPathMeasure!!.setPath(mPath, false)
                    val len = mPathMeasure!!.length
                    start = start * len
                    end = end * len
                    path.reset()
                    if (start > end) {
                        mPathMeasure!!.getSegment(start, len, path, true)
                        mPathMeasure!!.getSegment(0f, end, path, true)
                    } else {
                        mPathMeasure!!.getSegment(start, end, path, true)
                    }
                    path.rLineTo(0f, 0f) // fix bug in measure
                }
                mRenderPath.addPath(path, mFinalPathMatrix)
                if (fullPath.fillColor != Color.TRANSPARENT) {
                    if (mFillPaint == null) {
                        mFillPaint = Paint()
                        mFillPaint!!.style = Paint.Style.FILL
                        mFillPaint!!.isAntiAlias = true
                    }
                    val fillPaint = mFillPaint!!
                    fillPaint.color =
                        applyAlpha(
                            fullPath.fillColor,
                            fullPath.fillAlpha
                        )
                    fillPaint.setColorFilter(filter)
                    canvas.drawPath(mRenderPath, fillPaint)
                }
                if (fullPath.strokeColor != Color.TRANSPARENT) {
                    if (mStrokePaint == null) {
                        mStrokePaint = Paint()
                        mStrokePaint!!.style = Paint.Style.STROKE
                        mStrokePaint!!.isAntiAlias = true
                    }
                    val strokePaint = mStrokePaint!!
                    if (fullPath.mStrokeLineJoin != null) {
                        strokePaint.strokeJoin = fullPath.mStrokeLineJoin
                    }
                    if (fullPath.mStrokeLineCap != null) {
                        strokePaint.strokeCap = fullPath.mStrokeLineCap
                    }
                    strokePaint.strokeMiter = fullPath.mStrokeMiterlimit
                    strokePaint.color =
                        applyAlpha(
                            fullPath.strokeColor,
                            fullPath.strokeAlpha
                        )
                    strokePaint.setColorFilter(filter)
                    val finalStrokeScale = minScale * matrixScale
                    strokePaint.strokeWidth = fullPath.strokeWidth * finalStrokeScale
                    canvas.drawPath(mRenderPath, strokePaint)
                }
            }
        }

        private fun getMatrixScale(groupStackedMatrix: Matrix): Float {
            // Given unit vectors A = (0, 1) and B = (1, 0).
            // After matrix mapping, we got A' and B'. Let theta = the angel b/t A' and B'.
            // Therefore, the final scale we want is min(|A'| * sin(theta), |B'| * sin(theta)),
            // which is (|A'| * |B'| * sin(theta)) / max (|A'|, |B'|);
            // If  max (|A'|, |B'|) = 0, that means either x or y has a scale of 0.
            //
            // For non-skew case, which is most of the cases, matrix scale is computing exactly the
            // scale on x and y axis, and take the minimal of these two.
            // For skew case, an unit square will mapped to a parallelogram. And this function will
            // return the minimal height of the 2 bases.
            val unitVectors = floatArrayOf(0f, 1f, 1f, 0f)
            groupStackedMatrix.mapVectors(unitVectors)
            val scaleX = Math.hypot(unitVectors[0].toDouble(), unitVectors[1].toDouble()).toFloat()
            val scaleY = Math.hypot(unitVectors[2].toDouble(), unitVectors[3].toDouble()).toFloat()
            val crossProduct = cross(
                unitVectors[0], unitVectors[1], unitVectors[2],
                unitVectors[3]
            )
            val maxScale = Math.max(scaleX, scaleY)
            var matrixScale = 0f
            if (maxScale > 0) {
                matrixScale = Math.abs(crossProduct) / maxScale
            }
            if (DBG_VECTOR_DRAWABLE) {
                Log.d(LOGTAG, "Scale x $scaleX y $scaleY final $matrixScale")
            }
            return matrixScale
        }

        companion object {
            private val IDENTITY_MATRIX = Matrix()
            private fun cross(v1x: Float, v1y: Float, v2x: Float, v2y: Float): Float {
                return v1x * v2y - v1y * v2x
            }
        }
    }

    class VGroup {
        // mStackedMatrix is only used temporarily when drawing, it combines all
        // the parents' local matrices with the current one.
        val mStackedMatrix = Matrix()

        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        val mChildren = ArrayList<Any>()
        var mRotate = 0f
        private var mPivotX = 0f
        private var mPivotY = 0f
        private var mScaleX = 1f
        private var mScaleY = 1f
        private var mTranslateX = 0f
        private var mTranslateY = 0f

        // mLocalMatrix is updated based on the update of transformation information,
        // either parsed from the XML or by animation.
        val localMatrix = Matrix()
        var mChangingConfigurations = 0
        private var mThemeAttrs: IntArray? = null
        var groupName: String? = null
            private set

        constructor(copy: VGroup, targetsMap: ArrayMap<String?, Any?>) {
            mRotate = copy.mRotate
            mPivotX = copy.mPivotX
            mPivotY = copy.mPivotY
            mScaleX = copy.mScaleX
            mScaleY = copy.mScaleY
            mTranslateX = copy.mTranslateX
            mTranslateY = copy.mTranslateY
            mThemeAttrs = copy.mThemeAttrs
            groupName = copy.groupName
            mChangingConfigurations = copy.mChangingConfigurations
            if (groupName != null) {
                targetsMap.put(groupName, this)
            }
            localMatrix.set(copy.localMatrix)
            val children = copy.mChildren
            for (i in children.indices) {
                val copyChild = children[i]
                if (copyChild is VGroup) {
                    mChildren.add(
                        VGroup(
                            copyChild, targetsMap
                        )
                    )
                } else {
                    var newPath: VPath? = null
                    newPath = if (copyChild is VFullPath) {
                        VFullPath(copyChild)
                    } else if (copyChild is VClipPath) {
                        VClipPath(copyChild)
                    } else {
                        throw IllegalStateException("Unknown object in the tree!")
                    }
                    mChildren.add(newPath)
                    if (newPath.pathName != null) {
                        targetsMap.put(newPath.pathName, newPath)
                    }
                }
            }
        }

        constructor()

        fun inflate(res: Resources?, attrs: AttributeSet?, theme: Theme?, parser: XmlPullParser) {
            val a: TypedArray = obtainAttributes(
                res!!, theme, attrs,
                AndroidResources.styleable_VectorDrawableGroup
            )
            updateStateFromTypedArray(a, parser)
            a.recycle()
        }

        private fun updateStateFromTypedArray(a: TypedArray, parser: XmlPullParser) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);

            // Extract the theme attributes, if any.
            mThemeAttrs = null // TODO TINT THEME Not supported yet a.extractThemeAttrs();

            // This is added in API 11
            mRotate = TypedArrayUtils.getNamedFloat(
                a, parser, "rotation",
                AndroidResources.styleable_VectorDrawableGroup_rotation, mRotate
            )
            mPivotX = a.getFloat(AndroidResources.styleable_VectorDrawableGroup_pivotX, mPivotX)
            mPivotY = a.getFloat(AndroidResources.styleable_VectorDrawableGroup_pivotY, mPivotY)

            // This is added in API 11
            mScaleX = TypedArrayUtils.getNamedFloat(
                a, parser, "scaleX",
                AndroidResources.styleable_VectorDrawableGroup_scaleX, mScaleX
            )

            // This is added in API 11
            mScaleY = TypedArrayUtils.getNamedFloat(
                a, parser, "scaleY",
                AndroidResources.styleable_VectorDrawableGroup_scaleY, mScaleY
            )
            mTranslateX = TypedArrayUtils.getNamedFloat(
                a, parser, "translateX",
                AndroidResources.styleable_VectorDrawableGroup_translateX, mTranslateX
            )
            mTranslateY = TypedArrayUtils.getNamedFloat(
                a, parser, "translateY",
                AndroidResources.styleable_VectorDrawableGroup_translateY, mTranslateY
            )
            val groupName: String? = a.getString(AndroidResources.styleable_VectorDrawableGroup_name)
            if (groupName != null) {
                this.groupName = groupName
            }
            updateLocalMatrix()
        }

        private fun updateLocalMatrix() {
            // The order we apply is the same as the
            // RenderNode.cpp::applyViewPropertyTransforms().
            localMatrix.reset()
            localMatrix.postTranslate(-mPivotX, -mPivotY)
            localMatrix.postScale(mScaleX, mScaleY)
            localMatrix.postRotate(mRotate, 0f, 0f)
            localMatrix.postTranslate(mTranslateX + mPivotX, mTranslateY + mPivotY)
        }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var rotation: Float
            /* Setters and Getters, used by animator from AnimatedVectorDrawable. */ get() = mRotate
            set(rotation) {
                if (rotation != mRotate) {
                    mRotate = rotation
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var pivotX: Float
            get() = mPivotX
            set(pivotX) {
                if (pivotX != mPivotX) {
                    mPivotX = pivotX
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var pivotY: Float
            get() = mPivotY
            set(pivotY) {
                if (pivotY != mPivotY) {
                    mPivotY = pivotY
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var scaleX: Float
            get() = mScaleX
            set(scaleX) {
                if (scaleX != mScaleX) {
                    mScaleX = scaleX
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var scaleY: Float
            get() = mScaleY
            set(scaleY) {
                if (scaleY != mScaleY) {
                    mScaleY = scaleY
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var translateX: Float
            get() = mTranslateX
            set(translateX) {
                if (translateX != mTranslateX) {
                    mTranslateX = translateX
                    updateLocalMatrix()
                }
            }

        @get:Suppress("unused")
        @set:Suppress("unused")
        var translateY: Float
            get() = mTranslateY
            set(translateY) {
                if (translateY != mTranslateY) {
                    mTranslateY = translateY
                    updateLocalMatrix()
                }
            }
    }

    /**
     * Common Path information for clip path and normal path.
     */
    open class VPath {
        protected var mNodes: Array<PathParser.PathDataNode?>? = null
        var pathName: String? = null
        var mChangingConfigurations = 0

        constructor()

        fun printVPath(level: Int) {
            var indent = ""
            for (i in 0 until level) {
                indent += "    "
            }
            Log.v(
                LOGTAG, indent + "current path is :" + pathName +
                        " pathData is " + NodesToString(mNodes)
            )
        }

        fun NodesToString(nodes: Array<PathParser.PathDataNode?>?): String {
            var result = " "
            for (i in nodes!!.indices) {
                result += nodes[i]!!.type.toString() + ":"
                val params = nodes[i]!!.params
                for (j in params.indices) {
                    result += params[j].toString() + ","
                }
            }
            return result
        }

        constructor(copy: VPath) {
            pathName = copy.pathName
            mChangingConfigurations = copy.mChangingConfigurations
            mNodes = PathParser.deepCopyNodes(copy.mNodes)
        }

        fun toPath(path: Path) {
            path.reset()
            if (mNodes != null) {
                PathParser.PathDataNode.nodesToPath(mNodes!!, path)
            }
        }

        open fun canApplyTheme(): Boolean {
            return false
        }

        open fun applyTheme(t: Theme?) {}
        open val isClipPath: Boolean
            get() = false

        @get:Suppress("unused")
        @set:Suppress("unused")
        var pathData: Array<PathParser.PathDataNode?>?
            /* Setters and Getters, used by animator from AnimatedVectorDrawable. */ get() = mNodes
            set(nodes) {
                if (!PathParser.canMorph(mNodes, nodes)) {
                    // This should not happen in the middle of animation.
                    mNodes = PathParser.deepCopyNodes(nodes)
                } else {
                    PathParser.updateNodes(mNodes, nodes!!)
                }
            }
    }

    /**
     * Clip path, which only has name and pathData.
     */
    private class VClipPath : VPath {
        constructor()
        constructor(copy: VClipPath) : super(copy)

        fun inflate(r: Resources?, attrs: AttributeSet?, theme: Theme?, parser: XmlPullParser?) {
            // TODO TINT THEME Not supported yet
            val hasPathData = TypedArrayUtils.hasAttribute(
                parser!!, "pathData"
            )
            if (!hasPathData) {
                return
            }
            val a: TypedArray = obtainAttributes(
                r!!, theme, attrs,
                AndroidResources.styleable_VectorDrawableClipPath
            )
            updateStateFromTypedArray(a)
            a.recycle()
        }

        private fun updateStateFromTypedArray(a: TypedArray) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);;
            val pathName: String? =
                a.getString(AndroidResources.styleable_VectorDrawableClipPath_name)
            if (pathName != null) {
                this.pathName = pathName
            }
            val pathData: String? =
                a.getString(AndroidResources.styleable_VectorDrawableClipPath_pathData)
            if (pathData != null) {
                mNodes = PathParser.createNodesFromPathData(pathData)
            }
        }

        override val isClipPath: Boolean
            get() = true
    }

    /**
     * Normal path, which contains all the fill / paint information.
     */
    class VFullPath : VPath {
        /////////////////////////////////////////////////////
        // Variables below need to be copied (deep copy if applicable) for mutation.
        private var mThemeAttrs: IntArray? = null

        /* Setters and Getters, used by animator from AnimatedVectorDrawable. */
        @get:Suppress("unused")
        @set:Suppress("unused")
        var strokeColor = Color.TRANSPARENT

        @get:Suppress("unused")
        @set:Suppress("unused")
        var strokeWidth = 0f

        @get:Suppress("unused")
        @set:Suppress("unused")
        var fillColor = Color.TRANSPARENT

        @get:Suppress("unused")
        @set:Suppress("unused")
        var strokeAlpha = 1.0f
        var mFillRule = 0

        @get:Suppress("unused")
        @set:Suppress("unused")
        var fillAlpha = 1.0f

        @get:Suppress("unused")
        @set:Suppress("unused")
        var trimPathStart = 0f

        @get:Suppress("unused")
        @set:Suppress("unused")
        var trimPathEnd = 1f

        @get:Suppress("unused")
        @set:Suppress("unused")
        var trimPathOffset = 0f
        var mStrokeLineCap: Cap? = Paint.Cap.BUTT
        var mStrokeLineJoin: Join? = Paint.Join.MITER
        var mStrokeMiterlimit = 4f

        constructor()
        constructor(copy: VFullPath) : super(copy) {
            mThemeAttrs = copy.mThemeAttrs
            strokeColor = copy.strokeColor
            strokeWidth = copy.strokeWidth
            strokeAlpha = copy.strokeAlpha
            fillColor = copy.fillColor
            mFillRule = copy.mFillRule
            fillAlpha = copy.fillAlpha
            trimPathStart = copy.trimPathStart
            trimPathEnd = copy.trimPathEnd
            trimPathOffset = copy.trimPathOffset
            mStrokeLineCap = copy.mStrokeLineCap
            mStrokeLineJoin = copy.mStrokeLineJoin
            mStrokeMiterlimit = copy.mStrokeMiterlimit
        }

        private fun getStrokeLineCap(id: Int, defValue: Cap?): Cap? {
            return when (id) {
                LINECAP_BUTT -> Paint.Cap.BUTT
                LINECAP_ROUND -> Paint.Cap.ROUND
                LINECAP_SQUARE -> Paint.Cap.SQUARE
                else -> defValue
            }
        }

        private fun getStrokeLineJoin(id: Int, defValue: Join?): Join? {
            return when (id) {
                LINEJOIN_MITER -> Paint.Join.MITER
                LINEJOIN_ROUND -> Paint.Join.ROUND
                LINEJOIN_BEVEL -> Paint.Join.BEVEL
                else -> defValue
            }
        }

        override fun canApplyTheme(): Boolean {
            return mThemeAttrs != null
        }

        fun inflate(r: Resources?, attrs: AttributeSet?, theme: Theme?, parser: XmlPullParser) {
            val a: TypedArray = obtainAttributes(
                r!!, theme, attrs,
                AndroidResources.styleable_VectorDrawablePath
            )
            updateStateFromTypedArray(a, parser)
            a.recycle()
        }

        private fun updateStateFromTypedArray(a: TypedArray, parser: XmlPullParser) {
            // Account for any configuration changes.
            // mChangingConfigurations |= Utils.getChangingConfigurations(a);

            // Extract the theme attributes, if any.
            mThemeAttrs = null // TODO TINT THEME Not supported yet a.extractThemeAttrs();

            // In order to work around the conflicting id issue, we need to double check the
            // existence of the attribute.
            // B/c if the attribute existed in the compiled XML, then calling TypedArray will be
            // safe since the framework will look up in the XML first.
            // Note that each getAttributeValue take roughly 0.03ms, it is a price we have to pay.
            val hasPathData = TypedArrayUtils.hasAttribute(parser, "pathData")
            if (!hasPathData) {
                // If there is no pathData in the <path> tag, then this is an empty path,
                // nothing need to be drawn.
                return
            }
            val pathName: String? = a.getString(AndroidResources.styleable_VectorDrawablePath_name)
            if (pathName != null) {
                this.pathName = pathName
            }
            val pathData: String? =
                a.getString(AndroidResources.styleable_VectorDrawablePath_pathData)
            if (pathData != null) {
                mNodes = PathParser.createNodesFromPathData(pathData)
            }
            fillColor = TypedArrayUtils.getNamedColor(
                a, parser, "fillColor",
                AndroidResources.styleable_VectorDrawablePath_fillColor, fillColor
            )
            fillAlpha = TypedArrayUtils.getNamedFloat(
                a, parser, "fillAlpha",
                AndroidResources.styleable_VectorDrawablePath_fillAlpha, fillAlpha
            )
            val lineCap = TypedArrayUtils.getNamedInt(
                a, parser, "strokeLineCap",
                AndroidResources.styleable_VectorDrawablePath_strokeLineCap, -1
            )
            mStrokeLineCap = getStrokeLineCap(lineCap, mStrokeLineCap)
            val lineJoin = TypedArrayUtils.getNamedInt(
                a, parser, "strokeLineJoin",
                AndroidResources.styleable_VectorDrawablePath_strokeLineJoin, -1
            )
            mStrokeLineJoin = getStrokeLineJoin(lineJoin, mStrokeLineJoin)
            mStrokeMiterlimit = TypedArrayUtils.getNamedFloat(
                a, parser, "strokeMiterLimit",
                AndroidResources.styleable_VectorDrawablePath_strokeMiterLimit,
                mStrokeMiterlimit
            )
            strokeColor = TypedArrayUtils.getNamedColor(
                a, parser, "strokeColor",
                AndroidResources.styleable_VectorDrawablePath_strokeColor, strokeColor
            )
            strokeAlpha = TypedArrayUtils.getNamedFloat(
                a, parser, "strokeAlpha",
                AndroidResources.styleable_VectorDrawablePath_strokeAlpha, strokeAlpha
            )
            strokeWidth = TypedArrayUtils.getNamedFloat(
                a, parser, "strokeWidth",
                AndroidResources.styleable_VectorDrawablePath_strokeWidth, strokeWidth
            )
            trimPathEnd = TypedArrayUtils.getNamedFloat(
                a, parser, "trimPathEnd",
                AndroidResources.styleable_VectorDrawablePath_trimPathEnd, trimPathEnd
            )
            trimPathOffset = TypedArrayUtils.getNamedFloat(
                a, parser, "trimPathOffset",
                AndroidResources.styleable_VectorDrawablePath_trimPathOffset, trimPathOffset
            )
            trimPathStart = TypedArrayUtils.getNamedFloat(
                a, parser, "trimPathStart",
                AndroidResources.styleable_VectorDrawablePath_trimPathStart, trimPathStart
            )
        }

        override fun applyTheme(t: Theme?) {
            if (mThemeAttrs == null) {
                return
            }

            /*
             * TODO TINT THEME Not supported yet final TypedArray a =
             * t.resolveAttributes(mThemeAttrs, styleable_VectorDrawablePath);
             * updateStateFromTypedArray(a); a.recycle();
             */
        }
    }

    companion object {
        const val LOGTAG = "VectorDrawableCompat"
        val DEFAULT_TINT_MODE: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
        private const val SHAPE_CLIP_PATH = "clip-path"
        private const val SHAPE_GROUP = "group"
        private const val SHAPE_PATH = "path"
        private const val SHAPE_VECTOR = "vector"
        private const val LINECAP_BUTT = 0
        private const val LINECAP_ROUND = 1
        private const val LINECAP_SQUARE = 2
        private const val LINEJOIN_MITER = 0
        private const val LINEJOIN_ROUND = 1
        private const val LINEJOIN_BEVEL = 2

        // Cap the bitmap size, such that it won't hurt the performance too much
        // and it won't crash due to a very large scale.
        // The drawable will look blurry above this size.
        private const val MAX_CACHED_BITMAP_SIZE = 2048
        private const val DBG_VECTOR_DRAWABLE = false

        /**
         *
         *
         *
         */
        fun create(
            res: Resources,
            @DrawableRes resId: Int,
            tintColor: Int,
            strokeColor: Int,
            strokeWidth: Int
        ): VectorDrawableCompat? {
            return create(res, resId, null)
        }

        /**
         * Create a VectorDrawableCompat object.
         *
         * @param res   the resources.
         * @param resId the resource ID for VectorDrawableCompat object.
         * @param theme the theme of this vector drawable, it can be null.
         * @return a new VectorDrawableCompat or null if parsing error is found.
         */
        fun create(
            res: Resources, @DrawableRes resId: Int,
            theme: Theme?
        ): VectorDrawableCompat? {
//        if (Build.VERSION.SDK_INT >= 24) {
//            final VectorDrawableCompat drawable = new VectorDrawableCompat();
//            drawable.mDelegateDrawable = ResourcesCompat.getDrawable(res, resId, theme);
//            drawable.mCachedConstantStateDelegate = new VectorDrawableCompat.VectorDrawableDelegateState(
//                    drawable.mDelegateDrawable.getConstantState());
//            return drawable;
//        }
            try {
                @SuppressLint("ResourceType") val parser: XmlPullParser = res.getXml(resId)
                val attrs: AttributeSet = Xml.asAttributeSet(parser)
                var type: Int
                while (parser.next().also { type = it } != XmlPullParser.START_TAG &&
                    type != XmlPullParser.END_DOCUMENT) {
                    // Empty loop
                }
                if (type != XmlPullParser.START_TAG) {
                    throw XmlPullParserException("No start tag found")
                }
                return createFromXmlInner(res, parser, attrs, theme)
            } catch (e: XmlPullParserException) {
                Log.e(LOGTAG, "parser error", e)
            } catch (e: IOException) {
                Log.e(LOGTAG, "parser error", e)
            }
            return null
        }

        /**
         * Create a VectorDrawableCompat from inside an XML document using an optional
         * [Resources.Theme]. Called on a parser positioned at a tag in an XML
         * document, tries to create a Drawable from that tag. Returns `null`
         * if the tag is not a valid drawable.
         */
        @SuppressLint("NewApi")
        @Throws(XmlPullParserException::class, IOException::class)
        fun createFromXmlInner(
            r: Resources?,
            parser: XmlPullParser?,
            attrs: AttributeSet?,
            theme: Theme?
        ): VectorDrawableCompat {
            val drawable = VectorDrawableCompat()
            drawable.inflate(r!!, parser!!, attrs!!, theme)
            return drawable
        }

        fun applyAlpha(color: Int, alpha: Float): Int {
            var color = color
            val alphaBytes = Color.alpha(color)
            color = color and 0x00FFFFFF
            color = color or ((alphaBytes * alpha).toInt() shl 24)
            return color
        }

        /**
         * Parses a [PorterDuff.Mode] from a tintMode
         * attribute's enum value.
         */
        private fun parseTintModeCompat(value: Int, defaultMode: PorterDuff.Mode): PorterDuff.Mode {
            return when (value) {
                3 -> PorterDuff.Mode.SRC_OVER
                5 -> PorterDuff.Mode.SRC_IN
                9 -> PorterDuff.Mode.SRC_ATOP
                14 -> PorterDuff.Mode.MULTIPLY
                15 -> PorterDuff.Mode.SCREEN
                16 -> if (Build.VERSION.SDK_INT >= 11) {
                    PorterDuff.Mode.ADD
                } else {
                    defaultMode
                }

                else -> defaultMode
            }
        }
    }
}
