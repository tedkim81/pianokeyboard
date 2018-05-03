package com.teuskim.pianokeyboard;

import android.content.Context;
import android.util.AttributeSet;

public class BlackKeyView extends KeyView {

	public BlackKeyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BlackKeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BlackKeyView(Context context) {
		super(context);
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.key_black;
	}

	@Override
	protected int getNormalImageResId() {
		return R.drawable.key_black;
	}

	@Override
	protected int getPressedImageResId() {
		return R.drawable.key_black_pressed;
	}

	@Override
	public int getKeyType() {
		return KEY_TYPE_BLACK;
	}

}
