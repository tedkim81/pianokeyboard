package com.teuskim.pianokeyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class PianoKeyboardView extends LinearLayout {
	
	public interface OnKeyboardActionListener {
		/**
		 * MotionEvent.ACTION_DOWN 발생시 호출
		 */
		void onTouchDown(int keyType, int index, PianoKeyboard.Key key);
		
		/**
		 * MotionEvent.ACTION_MOVE 발생시 호출
		 */
		void onTouchMove();
		
		/**
		 * MotionEvent.ACTION_UP 발생시 호출
		 */
		void onTouchUp();
	}
	
	private Context mContext;
	private List<KeyView> mWhiteKeyViewList;
	private List<KeyView> mBlackKeyViewList;
	private Map<Integer, KeyView> mPressedViewMap;
	
	private int mWhiteWidth;
	private int mBlackWidth;
	private int mSmallGap;
	private int mBigGap;
	
	private OnKeyboardActionListener mKeyboardActionListener;
	private PianoKeyboard mKeyboard;
	private Map<KeyView, PianoKeyboard.Key> mKeyMap;
	private boolean mIsRegisterMode = false;
	private int mTextSize = 0;

	public PianoKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public PianoKeyboardView(Context context) {
		super(context);
		init(context);
	}
	
	private void init(Context context){
		// xml layout 으로 뷰를 생성하고, 멤버 변수들을 초기화 한다.
		mContext = context;
		LayoutInflater.from(context).inflate(R.layout.keyboard, this);
		
		mWhiteKeyViewList = new ArrayList<KeyView>();
		mBlackKeyViewList = new ArrayList<KeyView>();
		mPressedViewMap = new HashMap<Integer, KeyView>();
		mKeyMap = new HashMap<KeyView, PianoKeyboard.Key>();
		
		fillKeyViewList(mWhiteKeyViewList, (LinearLayout) findViewById(R.id.key_white_top));
		fillKeyViewList(mBlackKeyViewList, (LinearLayout) findViewById(R.id.key_black_top));
		fillKeyViewList(mWhiteKeyViewList, (LinearLayout) findViewById(R.id.key_white_bottom));
		fillKeyViewList(mBlackKeyViewList, (LinearLayout) findViewById(R.id.key_black_bottom));
	}
	
	private void fillKeyViewList(List<KeyView> keyViewList, LinearLayout layout){
		int j=0;
		for(int i=0; i<layout.getChildCount(); i++){
			if(layout.getChildAt(j) instanceof KeyView)
				keyViewList.add((KeyView) layout.getChildAt(j));
			j++;
		}
	}
	
	private void setKeyBlackLayoutParams(KeyView keyLayout, int keyWidth, int keyHeight, int leftMargin){
		MarginLayoutParams m1 = new MarginLayoutParams(keyWidth, keyHeight);
    	m1.setMargins(leftMargin, 0, 0, 0);
    	keyLayout.setLayoutParams(new LinearLayout.LayoutParams(m1));
	}
	
	public void setIsRegisterMode(boolean isRegisterMode){
		mIsRegisterMode = isRegisterMode;
	}
	
	public void adjustLayoutParams(double portraitRatio, double horizontalRatio, double horizontalWidthRatio){
		Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int keyboardHeight;
		int keyboardWidth;
		boolean isPortrait = display.getWidth() < display.getHeight();
		if(isPortrait){  // portrait
			keyboardHeight = (int)(display.getHeight() / portraitRatio);
			keyboardWidth = display.getWidth();
		}
		else{  // horizontal
			keyboardHeight = (int)(display.getHeight() / horizontalRatio);
			keyboardWidth = (int)(display.getWidth() * horizontalWidthRatio);
		}
		LinearLayout.LayoutParams lp;
		if(!isPortrait && horizontalRatio == 1){
			lp = new LinearLayout.LayoutParams(keyboardWidth, LinearLayout.LayoutParams.FILL_PARENT);
			keyboardHeight = (int)(keyboardHeight * 0.8);
		}
		else{
			lp = new LinearLayout.LayoutParams(keyboardWidth, keyboardHeight);
		}
		setLayoutParams(lp);
    	mWhiteWidth = keyboardWidth / 8;
    	mBlackWidth = (int)((double)(keyboardWidth / 8) * 0.9);
    	mSmallGap = mWhiteWidth-mBlackWidth;
    	mBigGap = mWhiteWidth*2 - mBlackWidth;
    	
    	int blackHeight = keyboardHeight / 4;
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(0), mBlackWidth, blackHeight, (mWhiteWidth - mBlackWidth/2));
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(1), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(2), mBlackWidth, blackHeight, mBigGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(3), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(4), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(5), mBlackWidth, blackHeight, 0);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(6), mBlackWidth, blackHeight, mWhiteWidth/2);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(7), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(8), mBlackWidth, blackHeight, mBigGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(9), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(10), mBlackWidth, blackHeight, mSmallGap);
    	setKeyBlackLayoutParams(mBlackKeyViewList.get(11), mBlackWidth, blackHeight, 0);
	}
	
	public void setOnKeyboardActionListener(OnKeyboardActionListener listener){
		mKeyboardActionListener = listener;
	}
	
	public void setTextSize(int textSize){
		mTextSize = textSize;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch(action & MotionEvent.ACTION_MASK){
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			actionDown(action, event);
			break;
			
		case MotionEvent.ACTION_MOVE:
			if(mIsRegisterMode == false)
				actionMove(event);
			break;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			actionUp(action, event);
			break;
		}
		return true;
	}
	
	private void actionDown(int action, MotionEvent event){
		int pointerIndex = getPointerIndex(action);
		int pointerId = event.getPointerId(pointerIndex);
		float x = event.getX(pointerIndex);
		float y = event.getY(pointerIndex);
		int keyIndex = getKeyIndexByXY(x, y);
		KeyView pressedView = getKeyViewByXY(y, keyIndex);
		pressedView.setPressed(true);
		mPressedViewMap.put(pointerId, pressedView);
		
		if(mKeyboardActionListener != null){
			mKeyboardActionListener.onTouchDown(pressedView.getKeyType(), keyIndex, mKeyMap.get(pressedView));
		}
	}
	
	private void actionMove(MotionEvent event){
		int pointerCnt = event.getPointerCount();
		boolean pressChanged = false;
		KeyView pressedView = null;
		for(int i=0; i<pointerCnt; i++){
			float x = event.getX(i);
			float y = event.getY(i);
			int keyIndex = getKeyIndexByXY(x, y);
			pressedView = getKeyViewByXY(y, keyIndex);
			if(mPressedViewMap.containsValue(pressedView) == false){
				pressChanged = true;
				if(mKeyboardActionListener != null){
					mKeyboardActionListener.onTouchDown(pressedView.getKeyType(), keyIndex, mKeyMap.get(pressedView));
				}
				break;
			}
		}
		if(pressChanged){
			Iterator<Integer> it = mPressedViewMap.keySet().iterator();
			while(it.hasNext()){
				mPressedViewMap.get(it.next()).setPressed(false);
			}
			for(int i=0; i<pointerCnt; i++){
				float x = event.getX(i);
				float y = event.getY(i);
				int keyIndex = getKeyIndexByXY(x, y);
				pressedView = getKeyViewByXY(y, keyIndex);
				pressedView.setPressed(true);
				int pointerId = event.getPointerId(i);
				mPressedViewMap.put(pointerId, pressedView);
			}
		}
	}
		
	private void actionUp(int action, MotionEvent event){
		mPressedViewMap.remove(event.getPointerId(getPointerIndex(action))).setPressed(false);
		
		if(mKeyboardActionListener != null){
			mKeyboardActionListener.onTouchUp();
		}
	}
	
	private int getPointerIndex(int action){
		return (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	}
	
	private int getKeyIndexByXY(float x, float y){
		int hCnt = (int)y / (getHeight()/4);
		int index = 0;
		if(hCnt == 0 || hCnt == 2){  // black
			if(x <= mWhiteWidth*1.5){
				index = 0;
			}
			else if(x <= mWhiteWidth*3){
				index = 1;
			}
			else if(x <= mWhiteWidth*4.5){
				index = 2;
			}
			else if(x <= mWhiteWidth*5.5){
				index = 3;
			}
			else if(x <= mWhiteWidth*7){
				index = 4;
			}
			else{
				index = 5;
			}
			
			if(hCnt == 2)
				index += 6;
		}
		else{  // white
			index = (int)x / mWhiteWidth;
			
			if(hCnt == 3)
				index += 8;
		}
		return index;
	}
	
	private KeyView getKeyViewByXY(float y, int keyIndex){
		int hCnt = (int)y / (getHeight()/4);
		if(hCnt == 0 || hCnt == 2){
			return mBlackKeyViewList.get(keyIndex);
		}
		else{
			return mWhiteKeyViewList.get(keyIndex);
		}
	}
	
	public void setKeyboard(PianoKeyboard keyboard){
		mKeyboard = keyboard;
		List<PianoKeyboard.Key> keyList = keyboard.getKeyList();
		int i=0;
		for(KeyView kv : mWhiteKeyViewList){
			PianoKeyboard.Key key = keyList.get(i++);
			if(key != null)
				kv.setText(key.getKeyLabel());
			else
				kv.setText("");
			if(mTextSize > 0)
				kv.setTextSize(mTextSize);
			mKeyMap.put(kv, key);
		}
		for(KeyView kv : mBlackKeyViewList){
			PianoKeyboard.Key key = keyList.get(i++);
			if(key != null)
				kv.setText(key.getKeyLabel());
			else
				kv.setText("");
			if(mTextSize > 0)
				kv.setTextSize(mTextSize);
			mKeyMap.put(kv, key);
			
			kv.alignCenter();
		}
	}
	
	public PianoKeyboard getKeyboard(){
		return mKeyboard;
	}

}
