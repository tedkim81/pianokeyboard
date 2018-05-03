package com.teuskim.pianokeyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;

import com.teuskim.pianokeyboard.PianoKeyboard.Key;
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener;

public class PianoPlayActivity extends BaseActivity implements OnKeyboardActionListener,OnClickListener {
	
	private PianoKeyboardView mKeyboardView;
	private PianoSoundManager mSoundManager;
	private Animation mNoteAni1;
	private Animation mNoteAni2;
	private Animation mNoteAni3;
	private Animation mNoteAni4;
	private ImageView mNote1;
	private ImageView mNote2;
	private ImageView mNote3;
	private ImageView mNote4;
	private Button mBtnPlayLog;
	private List<Integer> mNoteResList;
	private int mNoteIndex;
	private List<PlayLog> mPlayLogs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.piano_play);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		findViews();
		
		mNoteResList = new ArrayList<Integer>();
		mNoteResList.add(R.drawable.note1);
		mNoteResList.add(R.drawable.note2);
		mNoteResList.add(R.drawable.note3);
		mNoteResList.add(R.drawable.note4);
		mNoteResList.add(R.drawable.note5);
		mNoteResList.add(R.drawable.note6);
		mNoteIndex = 0;
		new SoundInitTask().execute();
		
		mPlayLogs = new ArrayList<PlayLog>();
		
		// google analytics
		mTracker.trackPageView("PianoPlayActivity");
	}
	
	protected void findViews(){
		mKeyboardView = (PianoKeyboardView) findViewById(R.id.keyboard);
		Map<Integer, String> keymap = new HashMap<Integer, String>();
		String[] strs = getResources().getStringArray(R.array.ex3_pitch_names);
		if(strs != null){
			for(int i=0; i<strs.length; i++){
				keymap.put(i, strs[i]);
			}
		}
		mKeyboardView.setTextSize(15);
		mKeyboardView.setKeyboard(new PianoKeyboard(getApplicationContext(), keymap, 3));
		mKeyboardView.adjustLayoutParams(2.1, 1, 1);
		mKeyboardView.setOnKeyboardActionListener(this);
		mNote1 = (ImageView) findViewById(R.id.note1);
		mNote2 = (ImageView) findViewById(R.id.note2);
		mNote3 = (ImageView) findViewById(R.id.note3);
		mNote4 = (ImageView) findViewById(R.id.note4);
		mBtnPlayLog = (Button) findViewById(R.id.btn_playlog);
		mBtnPlayLog.setOnClickListener(this);
		mNoteAni1 = AnimationUtils.loadAnimation(getBaseContext(), R.anim.note_jump);
		mNoteAni2 = AnimationUtils.loadAnimation(getBaseContext(), R.anim.note_jump);
		mNoteAni3 = AnimationUtils.loadAnimation(getBaseContext(), R.anim.note_jump);
		mNoteAni4 = AnimationUtils.loadAnimation(getBaseContext(), R.anim.note_jump);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_playlog:
			dialogPlayLog();
			break;
		}
	}

	@Override
	public void onTouchDown(int keyType, int index, Key key) {
		int soundIndex;
		if(keyType == KeyView.KEY_TYPE_WHITE){
			soundIndex = index;
		}
		else{
			soundIndex = index + PianoKeyboard.WHITE_NUM;
		}
		
		if (mSoundManager != null)
			mSoundManager.playSoundFromPlayer(soundIndex);
		
		if (mNote1 != null){
			if(keyType == KeyView.KEY_TYPE_WHITE){
				switch(index%(PianoKeyboard.WHITE_NUM/2)){
				case 0: case 1:
					noteAnimation(mNoteAni1, mNote1);
					break;
				case 2: case 3:
					noteAnimation(mNoteAni2, mNote2);
					break;
				case 4: case 5:
					noteAnimation(mNoteAni3, mNote3);
					break;
				default:
					noteAnimation(mNoteAni4, mNote4);
				}
			}
			else{
				switch(index%(PianoKeyboard.BLACK_NUM/2)){
				case 0: case 1:
					noteAnimation(mNoteAni1, mNote1);
					break;
				case 2: 
					noteAnimation(mNoteAni2, mNote2);
					break;
				case 3: case 4:
					noteAnimation(mNoteAni3, mNote3);
					break;
				default:
					noteAnimation(mNoteAni4, mNote4);
				}
			}
		}
		
		if(mPlayLogs.size() < 1000){
			mPlayLogs.add(new PlayLog(key.getKeyData(), System.currentTimeMillis()));
		}
	}

	@Override
	public void onTouchMove() {
	}

	@Override
	public void onTouchUp() {
	}
	
	private void noteAnimation(Animation noteAni, final ImageView note){
		note.setImageResource(mNoteResList.get(mNoteIndex));
		mNoteIndex = (mNoteIndex+1) % mNoteResList.size();
		
		note.startAnimation(noteAni);
		noteAni.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				note.setVisibility(View.INVISIBLE);
			}
		});
		note.setVisibility(View.VISIBLE);
	}
	
	private class SoundInitTask extends AsyncTask<String, Integer, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			PianoSoundManager sm = PianoSoundManager.getInstance(getApplicationContext());
			mSoundManager = sm;
			return true;
		}
		
	}
	
	private void dialogPlayLog(){
		final String message;
		if(mPlayLogs != null && mPlayLogs.size() > 0){
			long lastTime = 0;
			StringBuilder sb = new StringBuilder();
			for(PlayLog pl : mPlayLogs){
				if(pl.mTime-lastTime > 20)
					sb.append(' ');
				else 
					sb.append(',');
				sb.append(pl.mCode);
				lastTime = pl.mTime;
			}
			message = sb.toString();
		}
		else{
			message = getString(R.string.txt_no_playlog);
		}
		
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPlayLogs.clear();
			}
		})
		.setNeutralButton(R.string.btn_copy, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				cm.setText(message);
			}
		})
		.setNegativeButton(R.string.btn_close, null)
		.show();
	}
	
	private class PlayLog {
		public String mCode;
		public long mTime;
		
		public PlayLog(String code, long time){
			mCode = code;
			mTime = time;
		}
	}

}
