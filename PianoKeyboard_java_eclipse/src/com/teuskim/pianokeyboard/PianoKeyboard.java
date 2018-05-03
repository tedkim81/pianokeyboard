package com.teuskim.pianokeyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

/**
 * 키보드 모델 클래스
 * 1. xml 로부터 키 데이터를 만들어 저장하고,
 * 2. 필요한 키 데이터를 리턴해준다. 
 * 
 * @author kim5724
 *
 */
public class PianoKeyboard {
	
	// Keyboard XML Tags
	private static final String TAG_KEYBOARD = "Keyboard";
	private static final String TAG_KEY = "Key";
	
	public static final int WHITE_NUM = 16;
	public static final int BLACK_NUM = 12;
	
	private Context mContext;
	private int mXmlLayoutResId = 0;
	private List<Key> mKeyList;
	private boolean mIsCustom = false;
	private int mMaxLabelSize = 2;

	public PianoKeyboard(Context context, int xmlLayoutResId){
		// 필요한 멤버 변수 초기화하고, xml 로딩한다.
		mContext = context;
		mXmlLayoutResId = xmlLayoutResId;		
		loadKeyboard();
	}
	
	public PianoKeyboard(Context context, Map<Integer, String> keyStringMap, int maxLabelSize){
		mContext = context;
		mMaxLabelSize = maxLabelSize;
		setIsCustom(true);
		loadKeyboard(keyStringMap);
	}
	
	public PianoKeyboard(Context context, Map<Integer, String> keyStringMap){
		this(context, keyStringMap, 2);
	}
	
	public void loadKeyboard() {
		if(mXmlLayoutResId == 0)
			return;
		
		// xml 파싱하여 키 데이터를 만들어서 저장한다.
		mKeyList = new ArrayList<Key>();
		XmlResourceParser parser = mContext.getResources().getXml(mXmlLayoutResId);
		
		try{
			int event;
			while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT){
				if (event == XmlResourceParser.START_TAG) {
					String tag = parser.getName();
					if(TAG_KEYBOARD.equals(tag)){
						
					}
					else if(TAG_KEY.equals(tag)){
						AttributeSet as = Xml.asAttributeSet(parser);
						int key = as.getAttributeIntValue(0, 0);
						String keyLabel = as.getAttributeValue(1);
						int iconResId = as.getAttributeResourceValue(2, 0);
						mKeyList.add(new Key(key, keyLabel, iconResId, mIsCustom, keyLabel));
					}
				}
				else if(event == XmlResourceParser.END_TAG){
				}
			}
		}catch(Exception e){
		}		
	}
	
	public void loadKeyboard(Map<Integer, String> keyStringMap) {
		mKeyList = new ArrayList<Key>();
		int size = WHITE_NUM + BLACK_NUM;
		for(int i=0; i<size; i++){
			Key key = null;
			if(keyStringMap.containsKey(i)){
				String keyData = keyStringMap.get(i);
				int keyLabelSize;
				if(keyData != null){
					if(keyData.length() > mMaxLabelSize)
						keyLabelSize = mMaxLabelSize;
					else
						keyLabelSize = keyData.length();
					key = new Key(i, keyData.substring(0, keyLabelSize), 0, mIsCustom, keyData);
				}
				else{
					key = new Key(i, "", 0, mIsCustom, "");
				}
			}
			mKeyList.add(key);
		}
	}
	
	public List<Key> getKeyList(){
		// 키 데이터를 리턴한다.
		return mKeyList;
	}
	
	public void setIsCustom(boolean isCustom){
		mIsCustom = isCustom;
	}
	
	public boolean isCustom(){
		return mIsCustom;
	}
	
	/**
	 * 키 객체
	 */
	public static class Key {
		
		private int mKeyCode;
		private String mKeyLabel;
		private int mKeyIcon;
		private boolean mIsCustom;
		private String mKeyData;
		
		public Key(int keyCode, String keyLabel, int keyIcon, boolean isCustom, String keyData){
			mKeyCode = keyCode;
			mKeyLabel = keyLabel;
			mKeyIcon = keyIcon;
			mIsCustom = isCustom;
			mKeyData = keyData;
		}

		public int getKeyCode() {
			return mKeyCode;
		}

		public void setKeyCode(int keyCode) {
			mKeyCode = keyCode;
		}

		public String getKeyLabel() {
			return mKeyLabel;
		}

		public void setKeyLabel(String keyLabel) {
			mKeyLabel = keyLabel;
		}

		public int getKeyIcon() {
			return mKeyIcon;
		}

		public void setKeyIcon(int keyIcon) {
			mKeyIcon = keyIcon;
		}
		
		public boolean isCustom(){
			return mIsCustom;
		}
		
		public String getKeyData(){
			return mKeyData;
		}
		
	}

}
