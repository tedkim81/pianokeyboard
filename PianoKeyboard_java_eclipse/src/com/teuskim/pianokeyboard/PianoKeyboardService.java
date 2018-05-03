package com.teuskim.pianokeyboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener;


/**
 * 키보드 서비스 ( 컨트롤러 )
 * 1. 키보드 모델 및 키보드 뷰를 생성한다.
 * 2. 키보드뷰에 리스너를 달고, 콜백 받을 메소드들을 구현한다.
 * 3. 선택받은 글자를 출력하거나, 키보드를 변경하는 메소드를 구현한다.
 * 
 * 참고 ( 생명주기 )
 * 1. 키보드가 생성될 때, onCreate, onStartInput 호출
 * 2. 키보드가 노출될 때, onCreateInputView, onCreateCandidatesView, onStartInputView 호출
 * 3. 가끔 onStartInput 부터가 다시 호출되는데 왜그런지 모르겠다.
 * 4. 백키로 키보드 없애면 onFinishInputView 호출
 * 5. 입력영역을 눌러서 키보드를 다시 노출하면 onStartInputView 호출
 * 6. 다른 키보드를 선택하면 onDestroy 호출
 * 7. 다시 키보드 선택하면 1번부터.
 * 8. 현재화면에서 벗어나면 onFinishInputView, onStartInput 호출
 * 9. 현재화면 들어오면 onStartInput 호출
 */
public class PianoKeyboardService extends InputMethodService implements OnKeyboardActionListener,OnClickListener {
	
	private static final String TAG = "PianoKeyboardService";
	
	private static final int TYPE_CHAR = 1;
	private static final int TYPE_SPACE = 2;
	private static final int TYPE_ETC = 3;
	private static final double AFFINITY_WEIGHT = 50;
	
	private PianoSoundManager mSoundManager;
	private PianoKeyboardView mKeyboardView;
	private PianoKeyboard mEnglishKeyboard;
	private PianoKeyboard mEnglishKeyboardShift;
	private PianoKeyboard mHangulKeyboard;
	private PianoKeyboard mHangulKeyboardShift;
	private PianoKeyboard mSymbolKeyboard;
	private PianoKeyboard mSymbolKeyboardShift;
	private StringBuilder mComposing = new StringBuilder();
	private Button mBtnChangeKeyboard;
	private Button mBtnBackspace;
	private Button mBtnSpace;
	private Button mBtnEnter;
	private Button mBtnShift;
	private boolean mIsPressedBtnShift;
	private Button mBtnRepeat;
	private Button mBtnSettings;
	private HangulHandler mHangulHandler;
	private List<PianoKeyboard> mKeyboardList = new ArrayList<PianoKeyboard>();
	private Map<PianoKeyboard, String> mNameMap = new HashMap<PianoKeyboard, String>();
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			// msg.what 이 keycode 이다.
			mHangulHandler.handle(mHangulHandler.getKeyIntFromKeyCode(msg.what));
		}
		
	};
	private String mRepeatedString;
	private PianoKeyboardDb mDb;
	private LinearLayout mWordListLayout;
	private WordListTask mWordListTask;
	private String mInputString;
	private int mCurrTypeTextVariation;
	private int mKeyCodeEnter = KeyEvent.KEYCODE_ENTER;
	private boolean mIsInserting = false;
	
	private GoogleAnalyticsTracker mTracker;
	
	@Override
	public void onCreate() {
		// 객체 생성시 호출. 변경되지 않는 멤버 변수 초기화
		Log.d(TAG, "onCreate!!");
		super.onCreate();		
		
		mDb = PianoKeyboardDb.getInstance(getApplicationContext());		
		mSoundManager = PianoSoundManager.getInstance(getApplicationContext());
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.startNewSession("UA-33008558-1", 20*60, getApplication());
	}

	@Override
	public void onInitializeInterface() {
		// 뭔가 설정이 변경될 때마다 호출된다. 여기에 UI관련 멤버변수를 모두 셋팅한다.
		Log.d(TAG, "onInitializeInterface!!");
		super.onInitializeInterface();
		
		mEnglishKeyboard = new PianoKeyboard(this, R.xml.keyboard_english);
		mEnglishKeyboardShift = new PianoKeyboard(this, R.xml.keyboard_english_shift);
		mNameMap.put(mEnglishKeyboard, shortNameIfNeeded(getString(R.string.txt_english)));
		
		mHangulKeyboard = new PianoKeyboard(this, R.xml.keyboard_hangul);
		mHangulKeyboardShift = new PianoKeyboard(this, R.xml.keyboard_hangul_shift);
		mNameMap.put(mHangulKeyboard, shortNameIfNeeded(getString(R.string.txt_hangul)));
		mHangulHandler = new HangulHandler(this);
		
		mSymbolKeyboard = new PianoKeyboard(this, R.xml.keyboard_symbols);
		mSymbolKeyboardShift = new PianoKeyboard(this, R.xml.keyboard_symbols_shift);
		mNameMap.put(mSymbolKeyboard, shortNameIfNeeded(getString(R.string.txt_symbols)));
	}
	
	private String shortNameIfNeeded(String name){
		if(name != null && name.length() > 3)
			name = name.substring(0, 3);
		return name;
	}

	@Override
	public View onCreateInputView() {
		// 키보드뷰를 최초 출력할 때와 설정이 변경될 때마다 호출된다. 키보드뷰를 생성하여 리턴한다.
		Log.d(TAG, "onCreateInputView!!");
		
		View inputView = getLayoutInflater().inflate(R.layout.input, null);
		mKeyboardView = (PianoKeyboardView) inputView.findViewById(R.id.keyboard);
		mKeyboardView.adjustLayoutParams(2.6, 1.6, 0.65);
		mKeyboardView.setKeyboard(mEnglishKeyboard);
		mKeyboardView.setOnKeyboardActionListener(this);
		mBtnChangeKeyboard = (Button) inputView.findViewById(R.id.btn_changekeyboard);
		mBtnChangeKeyboard.setText(shortNameIfNeeded(getString(R.string.txt_english)));
		mBtnBackspace = (Button) inputView.findViewById(R.id.btn_backspace);
		mBtnSpace = (Button) inputView.findViewById(R.id.btn_space);
		mBtnEnter = (Button) inputView.findViewById(R.id.btn_enter);
		mBtnShift = (Button) inputView.findViewById(R.id.btn_shift);
		mIsPressedBtnShift = false;
		mBtnRepeat = (Button) inputView.findViewById(R.id.btn_repeat);
		mBtnSettings = (Button) inputView.findViewById(R.id.btn_settings);
		mWordListLayout = (LinearLayout) inputView.findViewById(R.id.history_list_layout);
		mBtnChangeKeyboard.setOnClickListener(this);
		mBtnShift.setOnClickListener(this);
		mBtnSettings.setOnClickListener(this);
		
		RepeatListener listener = new RepeatListener(this);
		mBtnBackspace.setOnTouchListener(listener);
		mBtnSpace.setOnTouchListener(listener);
		mBtnEnter.setOnTouchListener(listener);
		mBtnRepeat.setOnTouchListener(listener);
		
		return inputView;
	}
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		// 키보드뷰 셋팅이 완료된 후 호출된다. 키보드 객체를 셋팅한다.
		Log.d(TAG, "onStartInputView!!");
		super.onStartInputView(info, restarting);
		
		mKeyboardList.clear();
		String english = getString(R.string.txt_english);
		String hangul = getString(R.string.txt_hangul);
		String symbols = getString(R.string.txt_symbols);
		List<PianoKeyboardDb.KeySet> list = mDb.getKeySetList();
		for(PianoKeyboardDb.KeySet ks : list){
			if("Y".equals(ks.mShowYN)){
				String keyboardName = mDb.getKeyboardName(ks.mType);
				if(english.equals(keyboardName))
					mKeyboardList.add(mEnglishKeyboard);
				else if(hangul.equals(keyboardName))
					mKeyboardList.add(mHangulKeyboard);
				else if(symbols.equals(keyboardName))
					mKeyboardList.add(mSymbolKeyboard);
			}	
		}
		List<PianoKeyboardDb.CustomKeyset> cksList = mDb.getCustomKeySetList();
		for(PianoKeyboardDb.CustomKeyset cks : cksList){
			if("Y".equals(cks.mShowYN)){
				List<PianoKeyboardDb.CustomKeysetData> cksdList = mDb.getCustomKeySetDataList(cks.mId);
				Map<Integer, String> map = new HashMap<Integer, String>();
				for(PianoKeyboardDb.CustomKeysetData cksd : cksdList){
					map.put(cksd.mPosition, cksd.mData);
				}
				PianoKeyboard customKeyboard = new PianoKeyboard(getApplicationContext(), map);
				mKeyboardList.add(customKeyboard);
				mNameMap.put(customKeyboard, shortNameIfNeeded(cks.mName));
			}
		}
		
		switch (info.inputType&EditorInfo.TYPE_MASK_CLASS){
		case EditorInfo.TYPE_CLASS_NUMBER:
		case EditorInfo.TYPE_CLASS_DATETIME:
		case EditorInfo.TYPE_CLASS_PHONE:
			mKeyboardView.setKeyboard(mSymbolKeyboard);
			mBtnChangeKeyboard.setText(mNameMap.get(mSymbolKeyboard));
			break;
		default:
			try{
				PianoKeyboard keyboard = mKeyboardList.get(mDb.getKeyboardPosition());
				mKeyboardView.setKeyboard(keyboard);
				mBtnChangeKeyboard.setText(mNameMap.get(keyboard));
			}catch(Exception e){
				mDb.updateKeyboardPosition(0);
			}
			break;
		}
		
		mSoundManager.setSoundMode(mDb.getSoundMode());
		mSoundManager.setIsSoundOffIfSilent(mDb.isSoundOffIfSilent());
		showWordList();
		
		// google analytics
		mTracker.trackPageView("PianoKeyboardService");
		
		mCurrTypeTextVariation = info.inputType&EditorInfo.TYPE_MASK_VARIATION;
	}
	
	@Override
	public void onFinishInputView(boolean finishingInput) {
		// 키보드뷰가 사라지면 호출된다.
		Log.d(TAG, "onFinishInputView!!");
		
		// 입력된 내용을 저장한다.
		wordInsert(mInputString);
		
		super.onFinishInputView(finishingInput);
		
		finishInput();
		mRepeatedString = null;
	}
	
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		// 키 입력전 호출되며 가장 중요한 부분이다. 각종 멤버변수를 셋팅하는 등의 입력 준비 단계를 마무리 짓는다.
		Log.d(TAG, "onStartInput!!");
		super.onStartInput(attribute, restarting);
		
		switch (attribute.imeOptions & (EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
		case EditorInfo.IME_ACTION_NEXT:
			mKeyCodeEnter = KeyEvent.KEYCODE_TAB;
			break;
		default:
			mKeyCodeEnter = KeyEvent.KEYCODE_ENTER;
		}
	}
	
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		
		Log.d(TAG, "oldStart:"+oldSelStart+", oldEnd:"+oldSelEnd+", newStart:"+newSelStart+", newEnd:"+newSelEnd+", candStart:"+candidatesStart+", candEnd:"+candidatesEnd);
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
		
		if ((mComposing.length() > 0 || mHangulHandler.getComposingLength() > 0)
				&& (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)){
			
			finishInput();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
	}

	@Override
	public void onFinishInput() {
		// 키 입력이 완료되면 호출된다. 각종 상태를 리셋한다.
		Log.d(TAG, "onFinishInput!!");
		super.onFinishInput();
		
		finishInput();
	}
	
	private void finishInput(){
		mComposing.setLength(0);
		mHangulHandler.initHangulData();
	}

	@Override
	public void onDestroy() {
		// 객체가 종료될 때 호출된다. 메모리릭 방지를 위한 종료처리 등을 한다.
		Log.d(TAG, "onDestroy!!");
		mTracker.stopSession();
		super.onDestroy();
	}

	public void keyDownUp(int keyEventCode) {
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}

	@Override
	public void onClick(View v) {
		boolean isTextChanged = false;
		
		switch(v.getId()){
		case R.id.btn_changekeyboard:
			changeKeyboard();
			mSoundManager.resetLastPlayTime();
			break;
		case R.id.btn_backspace:
			mSoundManager.superPlaySound(29);
			handleBackspace();
			mSoundManager.resetLastPlayTime();
			isTextChanged = true;
			break;
		case R.id.btn_space:
			mSoundManager.superPlaySound(28);
			handleSpace();
			mSoundManager.updateLastPlayTime();
			isTextChanged = true;
			break;
		case R.id.btn_enter:
			mSoundManager.superPlaySound(30);
			handleEnter();
			mSoundManager.updateLastPlayTime();
			isTextChanged = true;
			break;
		case R.id.btn_shift:
			mSoundManager.superPlaySound(31);
			handleShift();
			mSoundManager.resetLastPlayTime();
			break;
		case R.id.btn_repeat:
			mSoundManager.superPlaySound(32);
			handleRepeat();
			mSoundManager.updateLastPlayTime();
			isTextChanged = true;
			break;
		case R.id.btn_settings:
			goSettings();
			break;
		}
		changeSpacebarText();
		
		if(isTextChanged){
			// 입력내용 저장
			showWordList();
		}
	}

	@Override
	public void onTouchDown(int keyType, int index, PianoKeyboard.Key key) {
		int soundIndex;
		if(keyType == KeyView.KEY_TYPE_WHITE){
			soundIndex = index;
		}
		else{
			soundIndex = index + PianoKeyboard.WHITE_NUM;
		}
		
		mSoundManager.playSound(soundIndex);
		
		handleInput(key);
		changeSpacebarText();
	}

	@Override
	public void onTouchMove() {}

	@Override
	public void onTouchUp() {}
	
	private void handleInput(PianoKeyboard.Key key){
		// 키 입력
		if(key == null || key.isCustom()){
			handleCustom(key);
		}
		else{
			if(isHangulInput(key)){
				handleHangul(key);
			}
			else{
				handleCommon(key);
			}
		}
		
		// 입력내용 저장
		showWordList();
	}
	
	private void changeSpacebarText(){
		if(mHangulHandler.isInit() == false){
			mBtnSpace.setText(R.string.btn_commit);
		}
		else{
			mBtnSpace.setText(R.string.btn_space);
		}
	}
	
	private void handleCommon(PianoKeyboard.Key key){
		mHangulHandler.commit();
		
		String selectedStr = String.valueOf((char)key.getKeyCode());
		mComposing.append(selectedStr);
		if(Character.isLetterOrDigit(key.getKeyCode())){
			setComposingText(mComposing, 1);
		}
		else{
			commitText(mComposing, mComposing.length());
			mComposing.setLength(0);
		}
	}
	
	private void handleHangul(PianoKeyboard.Key key){
		if(mComposing.length() > 0){
			commitText(mComposing, 1);
			mComposing.setLength(0);
			mHandler.sendEmptyMessageDelayed(key.getKeyCode(), 50);
		}
		else{
			mHangulHandler.handle(mHangulHandler.getKeyIntFromKeyCode(key.getKeyCode()));
		}
	}
	
	private void handleCustom(PianoKeyboard.Key key){
		if(key != null){
			int keyInt = mHangulHandler.getKeyIntFromJaso(key.getKeyData());
			if(keyInt >= 0){
				if(mComposing.length() > 0){
					commitText(mComposing, 1);
					mComposing.setLength(0);
				}
				mHangulHandler.handle(keyInt);
			}
			else{
				mHangulHandler.commit();
				
				String selectedStr = key.getKeyData();
				mComposing.append(selectedStr);
				if(selectedStr.length() == 1 && Character.isLetterOrDigit(selectedStr.charAt(0))){
					setComposingText(mComposing, 1);
				}
				else{
					commitText(mComposing, mComposing.length());
					mComposing.setLength(0);
				}
			}
		}
	}
	
	protected void setComposingText(CharSequence text, int pos){
		getCurrentInputConnection().setComposingText(text, pos);
	}
	
	private void commitText(CharSequence text, int pos){
		getCurrentInputConnection().commitText(text, pos);
	}
	
	protected void commitHangul(CharSequence text, int pos){
		getCurrentInputConnection().commitText(text, pos);
	}
	
	private void changeKeyboard(){
		if(mIsPressedBtnShift){
			handleShift();
		}
		int currPos = mKeyboardList.indexOf(mKeyboardView.getKeyboard());
		int nextPos = (currPos+1) % mKeyboardList.size();
		PianoKeyboard nextKeyboard = mKeyboardList.get(nextPos);
		mKeyboardView.setKeyboard(nextKeyboard);
		mBtnChangeKeyboard.setText(mNameMap.get(nextKeyboard));
	}
	
	private boolean isHangulInput(){
		return mHangulHandler.getComposingLength() > 0;
	}
	
	private boolean isHangulInput(PianoKeyboard.Key key){
		if(mHangulHandler.getKeyIntFromKeyCode(key.getKeyCode()) >= 0)
			return true;
		return false;
	}
	
	private void handleBackspace(){
		if(isHangulInput()){
			mHangulHandler.handleBackspace();
		}
		else{
			final int length = mComposing.length();
			if(length > 1){
				mComposing.delete(length - 1, length);
				getCurrentInputConnection().setComposingText(mComposing, 1);
			}
			else if(length > 0){
				mComposing.setLength(0);
				getCurrentInputConnection().commitText("", 0);
			}
			else{
				keyDownUp(KeyEvent.KEYCODE_DEL);
			}
		}
	}
	
	private void handleSpace(){
		if(isHangulInput()){
			mHangulHandler.handleSpace();
		}
		else{
			mHangulHandler.commit();
			mComposing.append(' ');
			commitText(mComposing, mComposing.length());
			mComposing.setLength(0);
		}
	}
	
	private void handleEnter(){
		if(isHangulInput()){
			mHangulHandler.commit();
		}
		keyDownUp(mKeyCodeEnter);
	}
	
	private void handleShift(){
		if(mKeyboardView.getKeyboard() == mEnglishKeyboard){
			mKeyboardView.setKeyboard(mEnglishKeyboardShift);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift_on);
			mIsPressedBtnShift = true;
		}
		else if(mKeyboardView.getKeyboard() == mEnglishKeyboardShift){
			mKeyboardView.setKeyboard(mEnglishKeyboard);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift);
			mIsPressedBtnShift = false;
		}
		else if(mKeyboardView.getKeyboard() == mHangulKeyboard){
			mKeyboardView.setKeyboard(mHangulKeyboardShift);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift_on);
			mIsPressedBtnShift = true;
		}
		else if(mKeyboardView.getKeyboard() == mHangulKeyboardShift){
			mKeyboardView.setKeyboard(mHangulKeyboard);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift);
			mIsPressedBtnShift = false;
		}
		else if(mKeyboardView.getKeyboard() == mSymbolKeyboard){
			mKeyboardView.setKeyboard(mSymbolKeyboardShift);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift_on);
			mIsPressedBtnShift = true;
		}
		else if(mKeyboardView.getKeyboard() == mSymbolKeyboardShift){
			mKeyboardView.setKeyboard(mSymbolKeyboard);
			mBtnShift.setBackgroundResource(R.drawable.btn_shift);
			mIsPressedBtnShift = false;
		}
	}
	
	private void handleRepeat(){
		if(isHangulInput()){
			if(mHangulHandler.getComposingLength() > 0){
				mRepeatedString = mHangulHandler.getComposing().toString();
				mHangulHandler.commit();
			}			
		}
		else{
			if(mComposing.length() > 0){
				mRepeatedString = mComposing.toString();
				commitText(mComposing, 1);
			}
		}
		if(mRepeatedString != null && mRepeatedString.length() > 0){
			commitText(mRepeatedString, 1);
		}
	}
	
	private void goSettings(){
		Intent i = new Intent(this, SettingActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}
	
	private class RepeatListener implements View.OnTouchListener{

		private boolean mIsDown = false;
		private View mView;
		private OnClickListener mListener;
		
		public RepeatListener(OnClickListener listener){
			mListener = listener;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			mView = v;
			
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mIsDown = true;
				mHandler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						delayedRepeat();
					}
				}, 300);
				break;
			case MotionEvent.ACTION_UP:
				mIsDown = false;
				mHandler.removeCallbacksAndMessages(null);
				mHandler.post(mRepeatRunnable);
				break;
			}
			
			return false;
		}
		
		private void delayedRepeat(){
			if(mIsDown == true){
				mHandler.postDelayed(mRepeatRunnable, 40);
			}
		}
		
		private Runnable mRepeatRunnable = new Runnable() {
			
			@Override
			public void run() {
				mListener.onClick(mView);
				delayedRepeat();
			}
		};
		
	}
	
	private String getAllText(){
		InputConnection ic = getCurrentInputConnection();
		return (String)ic.getTextBeforeCursor(100, 0) + (String)ic.getTextAfterCursor(100, 0);
	}
	
	private String getCurrentWord(){
		if(isHangulInput()){
			return mHangulHandler.getComposing().toString();
		}
		else{
			return mComposing.toString();
		}
	}
	
	private char getLastChar(){
		if(mInputString != null && mInputString.length() > 0){
			return mInputString.charAt(mInputString.length()-1);
		}
		return ' ';
	}
	
	private int getInputType(){
		EditorInfo info = getCurrentInputEditorInfo();
		return info.inputType&EditorInfo.TYPE_MASK_VARIATION;
	}
	
	private void recordInputString(){
		if(getInputType() != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD){
			if(mInputString == null || mInputString.length() == 0)
				mDb.updateKeyboardPosition(mKeyboardList.indexOf(mKeyboardView.getKeyboard()));
			
			String inputString = getAllText();
			
			// 입력영역이 초기화가 되었다면 단어저장
			if(mInputString != null && mInputString.length() > 1
					&& (inputString == null || inputString.length() == 0)
					&& mIsInserting == false){
				wordInsert(mInputString);
			}
			
			mInputString = inputString;
			Log.d(TAG, "record input string : "+mInputString);
		}
	}
	
	private class WordListTask extends AsyncTask<String, Integer, List<RecommendWord>>{
		
		@Override
		protected List<RecommendWord> doInBackground(String... params) {
			String text = getCurrentWord();
			if(text != null && getCharType(getLastChar()) != TYPE_SPACE)
				text = text.trim();
			else
				text = "";
			return recommendWords(text, mHangulHandler.separateJaso(text));
		}

		@Override
		protected void onPostExecute(List<RecommendWord> result) {
//			for(RecommendWord rw : result){
//				Log.e("AAAA", rw.mWord+" , "+rw.mPoint);
//			}
			
			LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
			mWordListLayout.removeAllViews();
			int cnt;
			if(result != null && (cnt = result.size()) > 0){
				int min = Math.min(cnt, 10);
				for(int i=0; i<min; i++){
					RecommendWord rw = result.get(i);
					final String word = rw.mWord;
					
					View v = inflater.inflate(R.layout.history_list_item, null);
					((TextView) v.findViewById(R.id.history_word)).setText(word);
					v.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							mComposing.setLength(0);
							commitHangul("", 1);
							mHangulHandler.initHangulData();
							
							if(getCharType(word.charAt(word.length()-1)) == TYPE_ETC)
								commitText(word, 1);
							else
								commitText(word+" ", 1);
							showWordList();
							
							changeSpacebarText();
						}
					});
					mWordListLayout.addView(v);
				}
			}
			else{
				// TODO: 예측 단어가 없을 경우 어떻게 할까? 기존에는 설정으로 보내는 알림 텍스트를 넣었으나, 다른게 좋을듯.
			}
		}
		
		private List<RecommendWord> recommendWords(String word, String composition){

			// 앞단어 이용해서 친밀도 기반 목록 구한다.
			List<RecommendWord> affinityWordList = getAffinityWordList(word, composition);
			
			// 입력영역속성 기반 목록을 구한다.
			List<RecommendWord> attrWordList = getAttrWordList(composition);
			
			// 입력횟수기반 목록을 구한다.
			List<RecommendWord> useCntWordList = getUseCntWordList(composition);
			
			// 입력시각기반 목록을 구한다.
			List<RecommendWord> useTimeWordList = getUseTimeWordList(composition);
			
			// 모든 목록을 병합하여 내림차순으로 10개의 항목을 갖는 목록을 만들어 리턴한다.
			return mergeWordList(affinityWordList, attrWordList, useCntWordList, useTimeWordList);
		}
		
		private List<RecommendWord> getAffinityWordList(String word, String composition){
			InputConnection ic = getCurrentInputConnection();
			String prevText = (String)ic.getTextBeforeCursor(50, 0);
			List<RecommendWord> resultList = new ArrayList<RecommendWord>();
			
			if(prevText.length() > 0){
				List<String> texts = splitText(prevText);
				if(texts.size() > 0)
					resultList = mDb.getNextWordList(texts.get(texts.size()-1), composition);
			}
			
			if(word != null && word.length() > 0 && getCharType(word.charAt(word.length()-1)) == TYPE_ETC){
				resultList.addAll(mDb.getNextWordList(word, ""));
			}
			
			return resultList;
		}
		
		private List<RecommendWord> getAttrWordList(String composition){
			EditorInfo info = getCurrentInputEditorInfo();
			int typeTextVariation = (info.inputType&EditorInfo.TYPE_MASK_VARIATION);
			return mDb.getWordListByAttr(composition, typeTextVariation);
		}
		
		private List<RecommendWord> getUseCntWordList(String composition){
			return mDb.getWordListByUseCnt(composition);
		}
		
		private List<RecommendWord> getUseTimeWordList(String composition){
			return mDb.getWordListByUseTime(composition);
		}
		
		private List<RecommendWord> mergeWordList(List<RecommendWord> affinityWordList, List<RecommendWord> attrWordList
				, List<RecommendWord> useCntWordList, List<RecommendWord> useTimeWordList){
			
//			for(RecommendWord rw : affinityWordList){
//				Log.e("AAAA", "affinityWordList : "+rw.mWordId+" , "+rw.mWord+" , "+rw.mUseCntNext+" , "+rw.mUseCntNextSum);
//			}
//			for(RecommendWord rw : attrWordList){
//				Log.e("AAAA", "attrWordList : "+rw.mWord+" , "+rw.mUseCntXxx+" , "+rw.mUseCntXxxSum);
//			}
//			for(RecommendWord rw : useCntWordList){
//				Log.e("AAAA", "useCntWordList : "+rw.mWord+" , "+rw.mUseCntTotal+" , "+rw.mUseCntTotalSum);
//			}
//			for(RecommendWord rw : useTimeWordList){
//				Log.e("AAAA", "useTimeWordList : "+rw.mWord+" , "+rw.mUseCntN+" , "+rw.mUseCntNSum);
//			}
			
			Map<Integer, RecommendWord> map = new HashMap<Integer, RecommendWord>();
			for(RecommendWord rw : affinityWordList){
				map.put(rw.mWordId, rw);
			}
			for(RecommendWord rw : attrWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntXxx = rw.mUseCntXxx;
					rwTmp.mUseCntXxxSum = rw.mUseCntXxxSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			for(RecommendWord rw : useCntWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntTotal = rw.mUseCntTotal;
					rwTmp.mUseCntTotalSum = rw.mUseCntTotalSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			for(RecommendWord rw : useTimeWordList){
				if(map.containsKey(rw.mWordId)){
					RecommendWord rwTmp = map.get(rw.mWordId);
					rwTmp.mUseCntN = rw.mUseCntN;
					rwTmp.mUseCntNSum = rw.mUseCntNSum;
				}
				else{
					map.put(rw.mWordId, rw);
				}
			}
			
			Iterator<RecommendWord> iter = map.values().iterator();
			PianoKeyboardDb.MyInfo myInfo = mDb.getMyInfo();
			long availableTime = myInfo.mAvailablePeriod * 86400000L;
			List<RecommendWord> resultList = new ArrayList<RecommendWord>();
			while(iter.hasNext()){
				RecommendWord rw = iter.next();
				rw.generatePoint(AFFINITY_WEIGHT, myInfo.mUseCntXxxWeight, myInfo.mUseCntTotalWeight, myInfo.mUseCntNWeight, availableTime);
				resultList.add(rw);
			}
			Collections.sort(resultList);
			return resultList;
		}
	}
	
	private void wordInsert(String inputString){
		// 입력된 내용을 저장한다.
		mIsInserting = true;
		WordInsertTask task = new WordInsertTask();
		task.execute(inputString);
	}
	
	private int getCharType(char ch){
		if((ch >= 97 && ch <= 122) || (ch >= 65 && ch <= 90) || (ch >= 0x3131 && ch <= 0xd7a3))
			return TYPE_CHAR;
		else if (ch == 9 || ch == 10 || ch == 13 || ch == 32)
			return TYPE_SPACE;
		else
			return TYPE_ETC;
	}
	
	private List<String> splitText(String inputText){
		List<String> wordList = new ArrayList<String>();
		if(inputText == null)
			return wordList;
		
		StringBuilder sb = new StringBuilder();
		int currType = 0;
		for(int i=0; i<inputText.length(); i++){
			char ch = inputText.charAt(i);
			int type = getCharType(ch);
			
			if(currType != TYPE_CHAR && type == TYPE_CHAR && sb.length() > 0){
				wordList.add(sb.toString());
				sb.setLength(0);
			}
			if(type != TYPE_SPACE){
				sb.append(ch);
			}
			
			currType = type;
		}
		if(sb.length() > 0)
			wordList.add(sb.toString());
		
		return wordList;
	}
	
	private class WordInsertTask extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			String inputString = params[0];
			Log.d(TAG, "input string : "+inputString);
			if(inputString == null || inputString.length() == 0)
				return false;
			
			String inputText = inputString.trim();
			if(inputText.length() > 0){
				// 단어분리 {{
				List<String> wordList = splitText(inputText);
				// }} 단어분리
				
				// 저장하기 {{
				String preWord = null;
				Calendar cal = Calendar.getInstance();
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				String columnUseCntXXX = mDb.getColumnUseCntXXX(mCurrTypeTextVariation);
				String columnUseCntN = mDb.getColumnUseCntN(hour);
				for(String word : wordList){
					String composition = mHangulHandler.separateJaso(word);
					mDb.insertOrUpdateWord(word, composition
										, mCurrTypeTextVariation, columnUseCntXXX
										, hour, columnUseCntN);
					
					if(preWord != null)
						mDb.insertOrUpdateNextWordGroup(preWord, word);
					preWord = word;
					
					Log.d(TAG, "insert word: "+word+" , composition: "+composition+" , typeTextVariation: "+mCurrTypeTextVariation);
				}
				// }} 저장하기
				
				// 분석 / 학습 {{
				// 가중치 갱신하기
				double weightTotal = mDb.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_TOTAL, PianoKeyboardDb.Word.USE_CNT_TOTAL);
				double weightN = mDb.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_N, columnUseCntN);
				double weightXXX = mDb.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_XXX, columnUseCntXXX);
				if(weightTotal > 0 && weightN > 0 && weightXXX > 0){
					double ratio = (PianoKeyboardDb.WEIGHT_SUM - PianoKeyboardDb.WEIGHT_INIT_NEXT) / (weightTotal + weightN + weightXXX);
					weightTotal = weightTotal * ratio;
					weightN = weightN * ratio;
					weightXXX = weightXXX * ratio;
					mDb.updateWeight((int)weightTotal, (int)weightN, (int)weightXXX);
				}
				
				// 유효기간 갱신하기
				// TODO: 성능상의 문제가 있다. 나중에 하자.
				// }} 분석 / 학습
				
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if(result){
				mInputString = null;
			}
			mIsInserting = false;
		}
		
	}
	
	public void showWordList(){
		recordInputString();
		
		mHandler.removeCallbacks(mWordListRunnable);
		mHandler.postDelayed(mWordListRunnable, 50);
	}
	
	private WordListRunnable mWordListRunnable = new WordListRunnable();
	private class WordListRunnable implements Runnable {

		@Override
		public void run() {
			if(mWordListTask != null){
				mWordListTask.cancel(true);
			}
			mWordListTask = new WordListTask();
			mWordListTask.execute();
		}
		
	}
	
}
