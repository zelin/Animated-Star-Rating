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

import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.content.res.TypedArray
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

/**
 * Created by Umar on 20/06/2018.
 */
/**
 * Internal common delegation shared by VectorDrawableCompat and AnimatedVectorDrawableCompat
 */

interface StarTintAwareDrawable {
    fun setTint(@ColorInt tint: Int)
    fun setTintList(tint: ColorStateList?)
    fun setTintMode(tintMode: PorterDuff.Mode?)
}

abstract class VectorDrawableCommon : Drawable(), StarTintAwareDrawable {
    // Drawable delegation for Lollipop and above.
    var mDelegateDrawable: Drawable? = null
    @Deprecated("Deprecated-STAR-001")
    override fun setColorFilter(color: Int, mode: PorterDuff.Mode) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.setColorFilter(color, mode)
            return
        }
        super.setColorFilter(color, mode)
    }

    override fun getColorFilter(): ColorFilter? {
        return if (mDelegateDrawable != null) {
            DrawableCompat.getColorFilter(mDelegateDrawable!!)
        } else null
    }

    override fun onLevelChange(level: Int): Boolean {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.setLevel(level)
        } else super.onLevelChange(level)
    }

    override fun onBoundsChange(bounds: Rect) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.bounds = bounds
            return
        }
        super.onBoundsChange(bounds)
    }

    override fun setHotspot(x: Float, y: Float) {
        // API >= 21 only.
        if (mDelegateDrawable != null) {
            DrawableCompat.setHotspot(mDelegateDrawable!!, x, y)
        }
        return
    }

    override fun setHotspotBounds(left: Int, top: Int, right: Int, bottom: Int) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setHotspotBounds(mDelegateDrawable!!, left, top, right, bottom)
            return
        }
    }

    override fun setFilterBitmap(filter: Boolean) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.isFilterBitmap = filter
            return
        }
    }

    override fun jumpToCurrentState() {
        if (mDelegateDrawable != null) {
            DrawableCompat.jumpToCurrentState(mDelegateDrawable!!)
            return
        }
    }

    override fun applyTheme(t: Theme) {
        // API >= 21 only.
        if (mDelegateDrawable != null) {
            DrawableCompat.applyTheme(mDelegateDrawable!!, t)
            return
        }
    }

    override fun clearColorFilter() {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.clearColorFilter()
            return
        }
        super.clearColorFilter()
    }

    override fun getCurrent(): Drawable {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.current
        } else super.getCurrent()
    }

    override fun getMinimumWidth(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.minimumWidth
        } else super.getMinimumWidth()
    }

    override fun getMinimumHeight(): Int {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.minimumHeight
        } else super.getMinimumHeight()
    }

    override fun getPadding(padding: Rect): Boolean {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.getPadding(padding)
        } else super.getPadding(padding)
    }

    override fun getState(): IntArray {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.state
        } else super.getState()
    }

    override fun getTransparentRegion(): Region? {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.transparentRegion
        } else super.getTransparentRegion()
    }

    override fun setChangingConfigurations(configs: Int) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable!!.changingConfigurations = configs
            return
        }
        super.setChangingConfigurations(configs)
    }

    override fun setState(stateSet: IntArray): Boolean {
        return if (mDelegateDrawable != null) {
            mDelegateDrawable!!.setState(stateSet)
        } else super.setState(stateSet)
    }

    companion object {
        /**
         * Obtains styled attributes from the theme, if available, or unstyled
         * resources if the theme is null.
         *
         * @hide
         */
        @JvmStatic
        protected fun obtainAttributes(
            res: Resources, theme: Theme?, set: AttributeSet?, attrs: IntArray?
        ): TypedArray {
            return theme?.obtainStyledAttributes(set, attrs!!, 0, 0) ?: res.obtainAttributes(
                set,
                attrs
            )
        }
    }
}
