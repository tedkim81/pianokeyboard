package com.teuskim.pianokeyboard

import android.content.Context
import android.util.AttributeSet

class BlackKeyView : KeyView {

    protected override val layoutResId: Int
        get() = R.layout.key_black

    protected override val normalImageResId: Int
        get() = R.drawable.key_black

    protected override val pressedImageResId: Int
        get() = R.drawable.key_black_pressed

    override val keyType: Int
        get() = KeyView.KEY_TYPE_BLACK

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context) : super(context) {}

}
