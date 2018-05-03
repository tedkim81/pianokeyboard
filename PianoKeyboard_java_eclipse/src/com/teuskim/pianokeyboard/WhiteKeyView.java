package com.teuskim.pianokeyboard;

import android.content.Context;
import android.util.AttributeSet;

public class WhiteKeyView extends KeyView {

	public WhiteKeyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public WhiteKeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WhiteKeyView(Context context) {
		super(context);
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.key_white;
	}

	@Override
	protected int getNormalImageResId() {
		return R.drawable.key_white;
	}

	@Override
	protected int getPressedImageResId() {
		return R.drawable.key_white_pressed;
	}

	@Override
	public int getKeyType() {
		return KEY_TYPE_WHITE;
	}

}
