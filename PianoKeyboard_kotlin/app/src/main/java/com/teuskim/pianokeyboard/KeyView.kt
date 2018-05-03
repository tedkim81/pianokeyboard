package com.teuskim.pianokeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

abstract class KeyView : RelativeLayout {

    private var mImageView: ImageView? = null
    private var mTextView: TextView? = null

    protected abstract val layoutResId: Int
    protected abstract val normalImageResId: Int
    protected abstract val pressedImageResId: Int
    abstract val keyType: Int

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(layoutResId, this)
        mImageView = findViewById<ImageView>(R.id.key_image)
        mTextView = findViewById<TextView>(R.id.key)
    }

    fun alignCenter() {
        val rl = mTextView!!.layoutParams as RelativeLayout.LayoutParams
        rl.addRule(RelativeLayout.CENTER_HORIZONTAL)
    }

    fun paddingLeft(left: Int) {
        mTextView!!.setPadding(left, 0, 0, 0)
    }

    fun setTextSize(textSize: Int) {
        mTextView!!.textSize = textSize.toFloat()
    }

    override fun setPressed(pressed: Boolean) {
        if (pressed) {
            mImageView!!.setBackgroundResource(pressedImageResId)
        } else {
            mImageView!!.setBackgroundResource(normalImageResId)
        }
    }

    fun setText(text: String) {
        mTextView!!.text = text
    }

    companion object {

        val KEY_TYPE_WHITE = 1
        val KEY_TYPE_BLACK = 2
    }

}
