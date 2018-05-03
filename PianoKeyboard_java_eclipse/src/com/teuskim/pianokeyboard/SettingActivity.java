package com.teuskim.pianokeyboard;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingActivity extends BaseActivity implements View.OnClickListener {
	
	private LinearLayout mListViewKeyboard;
	private LinearLayout mListCustom;
	private RadioGroup mRadiobtnSound;
	private RadioButton mRadioBtnSoundRecommended;
	private RadioButton mRadioBtnSoundOriginal;
	private RadioButton mRadioBtnSoundNone;
	private CheckBox mCheckboxNoSound;
	
	private LayoutInflater mInflater;
	private PianoKeyboardDb mDb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		findViews();
		
		mDb = PianoKeyboardDb.getInstance(getApplicationContext());
		mInflater = LayoutInflater.from(getApplicationContext());
		
		refreshKeySetList();
		refreshCustomList();
		
		int soundMode = mDb.getSoundMode();
		switch(soundMode){
		case SoundMode.RECOMMENDED:
			mRadioBtnSoundRecommended.setChecked(true);
			break;
		case SoundMode.ORIGINAL:
			mRadioBtnSoundOriginal.setChecked(true);
			break;
		case SoundMode.NONE:
			mRadioBtnSoundNone.setChecked(true);
			break;
		}
		mCheckboxNoSound.setChecked(mDb.isSoundOffIfSilent());
		
		// google analytics
		mTracker.trackPageView("SettingActivity");
	}
	
	private void findViews(){
		mListViewKeyboard = (LinearLayout) findViewById(R.id.list_keyboard);
		mListCustom = (LinearLayout) findViewById(R.id.list_custom);
		mRadiobtnSound = (RadioGroup) findViewById(R.id.radiobtn_sound);
		mRadioBtnSoundRecommended = (RadioButton) findViewById(R.id.radiobtn_sound_recommended);
		mRadioBtnSoundOriginal = (RadioButton) findViewById(R.id.radiobtn_sound_original);
		mRadioBtnSoundNone = (RadioButton) findViewById(R.id.radiobtn_sound_none);
		mCheckboxNoSound = (CheckBox) findViewById(R.id.checkbox_nosound);
		
		findViewById(R.id.btn_close).setOnClickListener(this);
		findViewById(R.id.btn_add_custom).setOnClickListener(this);
		mCheckboxNoSound.setOnClickListener(this);
		
		RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch(checkedId){
				case R.id.radiobtn_sound_recommended:
					mDb.updateSoundMode(SoundMode.RECOMMENDED);
					break;
				case R.id.radiobtn_sound_original:
					mDb.updateSoundMode(SoundMode.ORIGINAL);
					break;
				case R.id.radiobtn_sound_none:
					mDb.updateSoundMode(SoundMode.NONE);
					break;
				}
			}
		};
		mRadiobtnSound.setOnCheckedChangeListener(listener);
	}
	
	public void onClick(View v) {
		Intent i;
		switch(v.getId()){
		case R.id.btn_close:
			finish();
			break;
		case R.id.btn_add_custom:
			i = new Intent(getApplicationContext(), RegisterCustomActivity.class);
			startActivityForResult(i, 0);
			break;
		case R.id.checkbox_nosound:
			changeCheckboxNosound();
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// custom keyset 추가한 후 리프레쉬
		refreshCustomList();
	}

	private void refreshKeySetList(){
		mListViewKeyboard.removeAllViews();
		List<PianoKeyboardDb.KeySet> list = mDb.getKeySetList();
		
		for(final PianoKeyboardDb.KeySet item : list){
			View v = mInflater.inflate(R.layout.setting_keyset_item, null);
			CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_keyset);
			checkbox.setText(mDb.getKeyboardName(item.mType));
			if("Y".equals(item.mShowYN))
				checkbox.setChecked(true);
			else
				checkbox.setChecked(false);
			
			checkbox.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					mDb.updateKeySetChecked(item.mId, ((CheckBox)v).isChecked());
					mDb.updateKeyboardPosition(0);
				}
			});
			
			mListViewKeyboard.addView(v);
		}		
	}
	
	private void refreshCustomList(){
		mListCustom.removeAllViews();
		List<PianoKeyboardDb.CustomKeyset> list = mDb.getCustomKeySetList();
		
		for(final PianoKeyboardDb.CustomKeyset item : list){
			View v = mInflater.inflate(R.layout.setting_custom_item, null);
			TextView name = (TextView) v.findViewById(R.id.custom_name);
			name.setText(item.mName);
			Button btnEdit = (Button) v.findViewById(R.id.btn_edit_custom);
			Button btnDelete = (Button) v.findViewById(R.id.btn_delete_custom);
			View.OnClickListener listener = new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					switch(v.getId()){
					case R.id.btn_edit_custom:
						Intent i = new Intent(getApplicationContext(), RegisterCustomActivity.class);
						i.putExtra("customId", item.mId);
						startActivityForResult(i, 0);
						break;
					case R.id.btn_delete_custom:
						dialogDeleteCustom(item.mId);
						break;
					}
				}
			};
			btnEdit.setOnClickListener(listener);
			btnDelete.setOnClickListener(listener);
			
			final CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_custom);
			String showYN = mDb.getCustomKeySetShowYN(item.mId);
			
			checkbox.setText(R.string.txt_use_custom);
			
			if("Y".equals(showYN)){
				checkbox.setChecked(true);
			}
			else{
				checkbox.setChecked(false);
			}
			checkbox.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(checkbox.isChecked()){
						mDb.updateCustomKeySetShowYN(item.mId, "Y");
					}
					else{
						mDb.updateCustomKeySetShowYN(item.mId, "N");
					}
				}
			});
			
			mListCustom.addView(v);
		}
	}
	
	private void dialogDeleteCustom(final long itemId){
		new AlertDialog.Builder(this)
		.setMessage(R.string.alert_delete)
		.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDb.deleteCustomKeyset(itemId);
				refreshCustomList();
			}
		})
		.setNegativeButton(R.string.btn_cancel, null)
		.show();
	}
	
	private void changeCheckboxNosound(){
		mDb.updateIsSoundOffIfSilent(mCheckboxNoSound.isChecked());
	}
	
}
