package com.teuskim.pianokeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class KeyView extends RelativeLayout {
	
	public static final int KEY_TYPE_WHITE = 1;
	public static final int KEY_TYPE_BLACK = 2;
	
	private ImageView mImageView;
	private TextView mTextView;

	public KeyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public KeyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public KeyView(Context context) {
		super(context);
		init(context);
	}
	
	protected abstract int getLayoutResId();
	protected abstract int getNormalImageResId();
	protected abstract int getPressedImageResId();
	public abstract int getKeyType();
	
	private void init(Context context){
		LayoutInflater.from(context).inflate(getLayoutResId(), this);
		mImageView = (ImageView) findViewById(R.id.key_image);
		mTextView = (TextView) findViewById(R.id.key);
	}
	
	public void alignCenter(){
		RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mTextView.getLayoutParams();
		rl.addRule(RelativeLayout.CENTER_HORIZONTAL);
	}
	
	public void paddingLeft(int left){
		mTextView.setPadding(left, 0, 0, 0);
	}
	
	public void setTextSize(int textSize){
		mTextView.setTextSize(textSize);
	}

	@Override
	public void setPressed(boolean pressed) {
		if(pressed){
			mImageView.setBackgroundResource(getPressedImageResId());
		}
		else{
			mImageView.setBackgroundResource(getNormalImageResId());
		}
	}
	
	public void setText(String text){
		mTextView.setText(text);
	}
	
}
