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

import android.content.res.TypedArray
import org.xmlpull.v1.XmlPullParser

/**
 * Created by Umar on 20/06/2018.
 */
object TypedArrayUtils {
    private const val NAMESPACE = "http://schemas.android.com/apk/res/android"
    fun hasAttribute(parser: XmlPullParser, attrName: String?): Boolean {
        return parser.getAttributeValue(NAMESPACE, attrName) != null
    }

    fun getNamedFloat(
        a: TypedArray, parser: XmlPullParser, attrName: String?,
        resId: Int, defaultValue: Float
    ): Float {
        val hasAttr = hasAttribute(parser, attrName)
        return if (!hasAttr) {
            defaultValue
        } else {
            a.getFloat(resId, defaultValue)
        }
    }

    fun getNamedBoolean(
        a: TypedArray, parser: XmlPullParser, attrName: String?,
        resId: Int, defaultValue: Boolean
    ): Boolean {
        val hasAttr = hasAttribute(parser, attrName)
        return if (!hasAttr) {
            defaultValue
        } else {
            a.getBoolean(resId, defaultValue)
        }
    }

    fun getNamedInt(
        a: TypedArray, parser: XmlPullParser, attrName: String?,
        resId: Int, defaultValue: Int
    ): Int {
        val hasAttr = hasAttribute(parser, attrName)
        return if (!hasAttr) {
            defaultValue
        } else {
            a.getInt(resId, defaultValue)
        }
    }

    fun getNamedColor(
        a: TypedArray, parser: XmlPullParser, attrName: String?,
        resId: Int, defaultValue: Int
    ): Int {
        val hasAttr = hasAttribute(parser, attrName)
        return if (!hasAttr) {
            defaultValue
        } else {
            a.getColor(resId, defaultValue)
        }
    }
}
