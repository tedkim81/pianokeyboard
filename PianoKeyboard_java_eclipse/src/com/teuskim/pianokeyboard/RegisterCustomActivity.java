package com.teuskim.pianokeyboard;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.teuskim.pianokeyboard.PianoKeyboard.Key;
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener;

public class RegisterCustomActivity extends BaseActivity implements OnClickListener,OnKeyboardActionListener {
	
	private EditText mNameCustom;
	private PianoKeyboardView mKeyboardView;
	private LinearLayout mKeymapListView;
	private Button mBtnRecommend;
	private Button mBtnReset;
	
	private Map<Integer, String> mKeymap;
	private int mPressedIndex;
	private PianoKeyboard mPianoKeyboard;
	private int mCustomId;
	private PianoKeyboardDb mDb;
	private LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register_custom);
		findViews();
		
		mDb = PianoKeyboardDb.getInstance(getApplicationContext());
		mKeymap = new HashMap<Integer, String>();
		mInflater = LayoutInflater.from(this);
		
		mCustomId = getIntent().getIntExtra("customId", 0);
		if(mCustomId > 0){
			String name = mDb.getCustomKeySetName(mCustomId);
			mNameCustom.setText(name);
			
			List<PianoKeyboardDb.CustomKeysetData> cksdList = mDb.getCustomKeySetDataList(mCustomId);
			for(PianoKeyboardDb.CustomKeysetData cksd : cksdList){
				mKeymap.put(cksd.mPosition, cksd.mData);
			}
			
			refreshKeymap();
		}
		
		// google analytics
		mTracker.trackPageView("RegisterCustomActivity");
	}

	protected void findViews(){
		mNameCustom = (EditText) findViewById(R.id.name_custom);
		mKeyboardView = (PianoKeyboardView) findViewById(R.id.keyboard_view);
		mKeyboardView.setIsRegisterMode(true);
		mKeyboardView.adjustLayoutParams(3,3,1);
		mKeyboardView.setOnKeyboardActionListener(this);
		mKeymapListView = (LinearLayout) findViewById(R.id.keymap_list);
		mBtnRecommend = (Button) findViewById(R.id.btn_custom_recommend);
		mBtnReset = (Button) findViewById(R.id.btn_custom_reset);
		
		mKeyboardView.setOnClickListener(this);
		findViewById(R.id.btn_save).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		mBtnRecommend.setOnClickListener(this);
		mBtnReset.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_save:
			save();
			break;
		case R.id.btn_cancel:
			finish();
			break;
		case R.id.btn_custom_recommend:
			dialogRecommendedCustom();
			break;
		case R.id.btn_custom_reset:
			reset();
			break;
		}
	}
	
	private void save(){
		String name = mNameCustom.getText().toString();
		if(name == null || name.length() == 0){
			Toast.makeText(getApplicationContext(), R.string.toast_input_title, Toast.LENGTH_SHORT).show();
			return;
		}
		
		SaveTask task = new SaveTask();
		task.execute(name);
		
		finish();
	}
	
	private class SaveTask extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			String name = params[0];
			String showYN = "N";
			if(mCustomId > 0){
				showYN = mDb.getCustomKeySetShowYN(mCustomId);
				if(mDb.deleteCustomKeyset(mCustomId) == false){
					mDb.deleteCustomKeyset(mCustomId);
				}
			}		
			return mDb.insertCustomKeyset(name, showYN, mKeymap);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result)
				Toast.makeText(getApplicationContext(), R.string.toast_save_ok, Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), R.string.toast_save_fail, Toast.LENGTH_SHORT).show();
		}
		
	}

	@Override
	public void onTouchDown(int keyType, int index, Key key) {
		if(keyType == KeyView.KEY_TYPE_WHITE){
			mPressedIndex = index;
		}
		else{
			mPressedIndex = index + PianoKeyboard.WHITE_NUM;
		}
	}

	@Override
	public void onTouchMove() {
	}

	@Override
	public void onTouchUp() {
		dialogKeyInput();
	}
	
	private void dialogKeyInput(){
		LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		View layout = inflater.inflate(R.layout.register_custom_dialog, null);
		final EditText edittext = (EditText) layout.findViewById(R.id.input_keyset);
		
		new AlertDialog.Builder(this)
			.setTitle(R.string.title_input_key)
			.setView(layout)
			.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					String data = edittext.getText().toString();
					if(data != null && data.length() > 0){
						mKeymap.put(mPressedIndex, data);
						
						refreshKeymap();
					}
				}
			})
			.create().show();
	}
	
	private void refreshKeymap(){
		mKeymapListView.removeAllViews();
		
		TreeSet<Integer> keyset = new TreeSet<Integer>(mKeymap.keySet());
		Iterator<Integer> iter = keyset.iterator();
		boolean hasKeymap = false;
		boolean isShow1 = false;
		boolean isShow2 = false;
		boolean isShow3 = false;
		boolean isShow4 = false;
		
		while(iter.hasNext()){
			final int index = iter.next();
			
			if(index < 8 && isShow1 == false){
				addCustomSubtitle(R.string.txt_piano_white_top);
				isShow1 = true;
			}
			else if(index >= 8 && index < 16 && isShow2 == false){
				mInflater.inflate(R.layout.register_custom_divider, mKeymapListView);
				addCustomSubtitle(R.string.txt_piano_white_bottom);
				isShow2 = true;
			}
			else if(index >= 16 && index < 22 && isShow3 == false){
				mInflater.inflate(R.layout.register_custom_divider, mKeymapListView);
				addCustomSubtitle(R.string.txt_piano_black_top);
				isShow3 = true;
			}
			else if(index >= 22 && isShow4 == false){
				mInflater.inflate(R.layout.register_custom_divider, mKeymapListView);
				addCustomSubtitle(R.string.txt_piano_black_bottom);
				isShow4 = true;
			}
			
			View v = mInflater.inflate(R.layout.register_custom_item, null);
			TextView customData = (TextView) v.findViewById(R.id.custom_data);
			customData.setText(mKeymap.get(index));
			
			v.findViewById(R.id.btn_delete).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mKeymap.remove(index);
					refreshKeymap();
				}
			});
			
			mKeymapListView.addView(v);
			hasKeymap = true;
		}
		
		if(hasKeymap)
			mBtnReset.setVisibility(View.VISIBLE);
		else
			mBtnReset.setVisibility(View.GONE);
		
		mPianoKeyboard = new PianoKeyboard(this, mKeymap);
		mKeyboardView.setKeyboard(mPianoKeyboard);
	}
	
	private void addCustomSubtitle(int resId){
		View titleView = mInflater.inflate(R.layout.register_custom_title_item, null);
		TextView titleTextView = (TextView) titleView.findViewById(R.id.custom_title);
		titleTextView.setText(resId);
		mKeymapListView.addView(titleView);
	}
	
	private void reset(){
		mNameCustom.setText(null);
		mKeymap.clear();
		refreshKeymap();
	}
	
	private void dialogRecommendedCustom(){
		new AlertDialog.Builder(this)
			.setTitle(R.string.btn_custom_recommend)
			.setItems(R.array.custom_names, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Resources res = getResources();
					String[] names = res.getStringArray(R.array.custom_names);
					mNameCustom.setText(names[which]);
					
					String[] strs = null;
					switch(which){
					case 0:
						strs = res.getStringArray(R.array.ex1_symbols);
						break;
					case 1:
						strs = res.getStringArray(R.array.ex2_emoticons);
						break;
					case 2:
						strs = res.getStringArray(R.array.ex3_pitch_names);
						break;
					case 3:
						strs = res.getStringArray(R.array.ex4_hangul_2nd);
						break;
					}
					if(strs != null){
						for(int i=0; i<strs.length; i++){
							mKeymap.put(i, strs[i]);
						}
					}
					refreshKeymap();
				}
			})
			.create().show();
	}
	
}
