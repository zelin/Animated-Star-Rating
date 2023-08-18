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
object AndroidResources {
    // Resources ID generated in the latest R.java for framework.
    val styleable_VectorDrawableTypeArray = intArrayOf(
        android.R.attr.name, android.R.attr.tint, android.R.attr.height,
        android.R.attr.width, android.R.attr.alpha, android.R.attr.autoMirrored,
        android.R.attr.tintMode, android.R.attr.viewportWidth, android.R.attr.viewportHeight
    )
    const val styleable_VectorDrawable_alpha = 4
    const val styleable_VectorDrawable_autoMirrored = 5
    const val styleable_VectorDrawable_height = 2
    const val styleable_VectorDrawable_name = 0
    const val styleable_VectorDrawable_tint = 1
    const val styleable_VectorDrawable_tintMode = 6
    const val styleable_VectorDrawable_viewportHeight = 8
    const val styleable_VectorDrawable_viewportWidth = 7
    const val styleable_VectorDrawable_width = 3
    val styleable_VectorDrawableGroup = intArrayOf(
        android.R.attr.name, android.R.attr.pivotX, android.R.attr.pivotY,
        android.R.attr.scaleX, android.R.attr.scaleY, android.R.attr.rotation,
        android.R.attr.translateX, android.R.attr.translateY
    )
    const val styleable_VectorDrawableGroup_name = 0
    const val styleable_VectorDrawableGroup_pivotX = 1
    const val styleable_VectorDrawableGroup_pivotY = 2
    const val styleable_VectorDrawableGroup_rotation = 5
    const val styleable_VectorDrawableGroup_scaleX = 3
    const val styleable_VectorDrawableGroup_scaleY = 4
    const val styleable_VectorDrawableGroup_translateX = 6
    const val styleable_VectorDrawableGroup_translateY = 7
    val styleable_VectorDrawablePath = intArrayOf(
        android.R.attr.name, android.R.attr.fillColor, android.R.attr.pathData,
        android.R.attr.strokeColor, android.R.attr.strokeWidth, android.R.attr.trimPathStart,
        android.R.attr.trimPathEnd, android.R.attr.trimPathOffset, android.R.attr.strokeLineCap,
        android.R.attr.strokeLineJoin, android.R.attr.strokeMiterLimit,
        android.R.attr.strokeAlpha, android.R.attr.fillAlpha
    )
    const val styleable_VectorDrawablePath_fillAlpha = 12
    const val styleable_VectorDrawablePath_fillColor = 1
    const val styleable_VectorDrawablePath_name = 0
    const val styleable_VectorDrawablePath_pathData = 2
    const val styleable_VectorDrawablePath_strokeAlpha = 11
    const val styleable_VectorDrawablePath_strokeColor = 3
    const val styleable_VectorDrawablePath_strokeLineCap = 8
    const val styleable_VectorDrawablePath_strokeLineJoin = 9
    const val styleable_VectorDrawablePath_strokeMiterLimit = 10
    const val styleable_VectorDrawablePath_strokeWidth = 4
    const val styleable_VectorDrawablePath_trimPathEnd = 6
    const val styleable_VectorDrawablePath_trimPathOffset = 7
    const val styleable_VectorDrawablePath_trimPathStart = 5
    val styleable_VectorDrawableClipPath = intArrayOf(
        android.R.attr.name, android.R.attr.pathData
    )
    const val styleable_VectorDrawableClipPath_name = 0
    const val styleable_VectorDrawableClipPath_pathData = 1
    val styleable_AnimatedVectorDrawable = intArrayOf(
        android.R.attr.drawable
    )
    const val styleable_AnimatedVectorDrawable_drawable = 0
    val styleable_AnimatedVectorDrawableTarget = intArrayOf(
        android.R.attr.name, android.R.attr.animation
    )
    const val styleable_AnimatedVectorDrawableTarget_animation = 1
    const val styleable_AnimatedVectorDrawableTarget_name = 0
}