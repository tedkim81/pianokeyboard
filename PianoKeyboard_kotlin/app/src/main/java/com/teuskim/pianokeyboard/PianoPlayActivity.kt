package com.teuskim.pianokeyboard

import java.util.ArrayList
import java.util.HashMap

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.ClipboardManager
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Animation.AnimationListener
import android.widget.Button
import android.widget.ImageView

import com.teuskim.pianokeyboard.PianoKeyboard.Key
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener

class PianoPlayActivity : BaseActivity(), OnKeyboardActionListener, OnClickListener {

    private var mKeyboardView: PianoKeyboardView? = null
    private var mSoundManager: PianoSoundManager? = null
    private var mNoteAni1: Animation? = null
    private var mNoteAni2: Animation? = null
    private var mNoteAni3: Animation? = null
    private var mNoteAni4: Animation? = null
    private var mNote1: ImageView? = null
    private var mNote2: ImageView? = null
    private var mNote3: ImageView? = null
    private var mNote4: ImageView? = null
    private var mBtnPlayLog: Button? = null
    private var mNoteResList: MutableList<Int>? = null
    private var mNoteIndex: Int = 0
    private var mPlayLogs: MutableList<PlayLog>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.piano_play)
        volumeControlStream = AudioManager.STREAM_MUSIC
        findViews()

        mNoteResList = ArrayList()
        mNoteResList!!.add(R.drawable.note1)
        mNoteResList!!.add(R.drawable.note2)
        mNoteResList!!.add(R.drawable.note3)
        mNoteResList!!.add(R.drawable.note4)
        mNoteResList!!.add(R.drawable.note5)
        mNoteResList!!.add(R.drawable.note6)
        mNoteIndex = 0
        SoundInitTask().execute()

        mPlayLogs = ArrayList()

        // google analytics
        mTracker!!.trackPageView("PianoPlayActivity")
    }

    protected fun findViews() {
        mKeyboardView = findViewById<View>(R.id.keyboard) as PianoKeyboardView
        val keymap = HashMap<Int, String>()
        val strs = resources.getStringArray(R.array.ex3_pitch_names)
        if (strs != null) {
            for (i in strs.indices) {
                keymap[i] = strs[i]
            }
        }
        mKeyboardView!!.setTextSize(15)
        mKeyboardView!!.keyboard = PianoKeyboard(applicationContext, keymap, 3)
        mKeyboardView!!.adjustLayoutParams(2.1, 1.0, 1.0)
        mKeyboardView!!.setOnKeyboardActionListener(this)
        mNote1 = findViewById<View>(R.id.note1) as ImageView
        mNote2 = findViewById<View>(R.id.note2) as ImageView
        mNote3 = findViewById<View>(R.id.note3) as ImageView
        mNote4 = findViewById<View>(R.id.note4) as ImageView
        mBtnPlayLog = findViewById<View>(R.id.btn_playlog) as Button
        mBtnPlayLog!!.setOnClickListener(this)
        mNoteAni1 = AnimationUtils.loadAnimation(baseContext, R.anim.note_jump)
        mNoteAni2 = AnimationUtils.loadAnimation(baseContext, R.anim.note_jump)
        mNoteAni3 = AnimationUtils.loadAnimation(baseContext, R.anim.note_jump)
        mNoteAni4 = AnimationUtils.loadAnimation(baseContext, R.anim.note_jump)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_playlog -> dialogPlayLog()
        }
    }

    override fun onTouchDown(keyType: Int, index: Int, key: Key) {
        val soundIndex: Int
        if (keyType == KeyView.KEY_TYPE_WHITE) {
            soundIndex = index
        } else {
            soundIndex = index + PianoKeyboard.WHITE_NUM
        }

        if (mSoundManager != null)
            mSoundManager!!.playSoundFromPlayer(soundIndex)

        if (mNote1 != null) {
            if (keyType == KeyView.KEY_TYPE_WHITE) {
                when (index % (PianoKeyboard.WHITE_NUM / 2)) {
                    0, 1 -> noteAnimation(mNoteAni1, mNote1)
                    2, 3 -> noteAnimation(mNoteAni2, mNote2)
                    4, 5 -> noteAnimation(mNoteAni3, mNote3)
                    else -> noteAnimation(mNoteAni4, mNote4)
                }
            } else {
                when (index % (PianoKeyboard.BLACK_NUM / 2)) {
                    0, 1 -> noteAnimation(mNoteAni1, mNote1)
                    2 -> noteAnimation(mNoteAni2, mNote2)
                    3, 4 -> noteAnimation(mNoteAni3, mNote3)
                    else -> noteAnimation(mNoteAni4, mNote4)
                }
            }
        }

        if (mPlayLogs!!.size < 1000) {
            mPlayLogs!!.add(PlayLog(key.keyData, System.currentTimeMillis()))
        }
    }

    override fun onTouchMove() {}

    override fun onTouchUp() {}

    private fun noteAnimation(noteAni: Animation?, note: ImageView?) {
        note!!.setImageResource(mNoteResList!![mNoteIndex])
        mNoteIndex = (mNoteIndex + 1) % mNoteResList!!.size

        note.startAnimation(noteAni)
        noteAni!!.setAnimationListener(object : AnimationListener {

            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                note.visibility = View.INVISIBLE
            }
        })
        note.visibility = View.VISIBLE
    }

    private inner class SoundInitTask : AsyncTask<String, Int, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val sm = PianoSoundManager.getInstance(applicationContext)
            mSoundManager = sm
            return true
        }

    }

    private fun dialogPlayLog() {
        val message: String
        if (mPlayLogs != null && mPlayLogs!!.size > 0) {
            var lastTime: Long = 0
            val sb = StringBuilder()
            for (pl in mPlayLogs!!) {
                if (pl.mTime - lastTime > 20)
                    sb.append(' ')
                else
                    sb.append(',')
                sb.append(pl.mCode)
                lastTime = pl.mTime
            }
            message = sb.toString()
        } else {
            message = getString(R.string.txt_no_playlog)
        }

        AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.btn_delete) { dialog, which -> mPlayLogs!!.clear() }
                .setNeutralButton(R.string.btn_copy) { dialog, which ->
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.text = message
                }
                .setNegativeButton(R.string.btn_close, null)
                .show()
    }

    private inner class PlayLog(var mCode: String, var mTime: Long)

}
