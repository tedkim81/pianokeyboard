package com.teuskim.pianokeyboard

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : BaseActivity(), OnClickListener {

    private var mBtnPlayPiano: Button? = null
    private var mTxtAboutButton: TextView? = null
    private var mBtnChangeSettings: Button? = null
    private var mInputTest: EditText? = null
    private var mImm: InputMethodManager? = null
    private var mCurrState: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        findViews()
        volumeControlStream = AudioManager.STREAM_MUSIC

        mImm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // google analytics
        mTracker!!.trackPageView("MainActivity")

        // TODO: for test
        //		PianoKeyboardDb db = PianoKeyboardDb.getInstance(getApplicationContext());
        //		List<PianoKeyboardDb.Word> wordList = db.getWordList();
        //		for(PianoKeyboardDb.Word word : wordList){
        //			Log.e("AAAA", "word : "+word.mWordId+", "+word.mWord+", "+word.mComposition+", "+word.mUseCntTotal
        //					+", ("+word.mUseCnt0+" "+word.mUseCnt4+" "+word.mUseCnt8+" "+word.mUseCnt12+" "+word.mUseCnt16+" "+word.mUseCnt20+")"
        //					+", ("+word.mUseCntNormal+" "+word.mUseCntEmailAddress+" "+word.mUseCntEmailSubject+" "+word.mUseCntUri+" "+word.mUseCntPersonName+" "+word.mUseCntPostalAddress+" "+word.mUseCntNumber+")"
        //					+", "+word.mUpdDt+", "+word.mCrtDt);
        //		}
        //		List<PianoKeyboardDb.NextWordGroup> nwgList = db.getNextWordGroupList();
        //		for(PianoKeyboardDb.NextWordGroup nwg : nwgList){
        //			Log.e("AAAA", "next word group : "+nwg.mWord+", "+nwg.mNextWord+", "+nwg.mUseCnt);
        //		}
    }

    protected fun findViews() {
        mBtnPlayPiano = findViewById<View>(R.id.btn_play_piano) as Button
        mTxtAboutButton = findViewById<View>(R.id.txt_about_button) as TextView
        mBtnChangeSettings = findViewById<View>(R.id.btn_change_settings) as Button
        mInputTest = findViewById<View>(R.id.input_test) as EditText

        mBtnPlayPiano!!.setOnClickListener(this)
        mBtnChangeSettings!!.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()

        makeSettingButton()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            makeSettingButton()
        }
    }

    private fun makeSettingButton() {
        val packageName = "com.teuskim.pianokeyboard"
        val infos = mImm!!.enabledInputMethodList
        var enabled = false
        for (info in infos) {
            if (packageName == info.packageName) {
                enabled = true
                break
            }
        }
        if (enabled) {
            val strIM = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            if (strIM != null && strIM.contains(packageName)) {
                mTxtAboutButton!!.text = ""
                mTxtAboutButton!!.visibility = View.GONE
                mBtnChangeSettings!!.setText(R.string.title_go_settings)
                mCurrState = CURR_STATE_USE
                mInputTest!!.visibility = View.VISIBLE
            } else {
                mTxtAboutButton!!.setText(R.string.txt_change_keyboard)
                mTxtAboutButton!!.visibility = View.VISIBLE
                mBtnChangeSettings!!.setText(R.string.title_change_keyboard)
                mCurrState = CURR_STATE_UNSELECTED
                mInputTest!!.visibility = View.GONE
            }
        } else {
            mTxtAboutButton!!.setText(R.string.txt_go_android_settings)
            mTxtAboutButton!!.visibility = View.VISIBLE
            mBtnChangeSettings!!.setText(R.string.title_go_android_settings)
            mCurrState = CURR_STATE_UNCHECKED
            mInputTest!!.visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        val i: Intent

        when (v.id) {
            R.id.btn_play_piano -> {
                i = Intent(applicationContext, PianoPlayActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(i)
            }

            R.id.btn_change_settings -> actionButton()
        }
    }

    private fun actionButton() {
        val i: Intent
        when (mCurrState) {
            CURR_STATE_UNCHECKED -> {
                i = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                startActivity(i)
            }
            CURR_STATE_UNSELECTED -> if (mImm != null) {
                mImm!!.showInputMethodPicker()
            }
            CURR_STATE_USE -> {
                i = Intent(applicationContext, SettingActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(i)
            }
        }
    }

    companion object {

        private val CURR_STATE_UNCHECKED = 1
        private val CURR_STATE_UNSELECTED = 2
        private val CURR_STATE_USE = 3
    }

}
