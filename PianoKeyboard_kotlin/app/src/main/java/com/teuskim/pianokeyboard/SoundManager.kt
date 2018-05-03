package com.teuskim.pianokeyboard

import java.util.HashMap

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool


open class SoundManager {

    private var mSoundPool: SoundPool? = null
    private var mSoundPoolMap: HashMap<Int, Int>? = null
    private var mAudioManager: AudioManager? = null
    private var mContext: Context? = null

    fun initSounds(theContext: Context) {
        mContext = theContext
        mSoundPool = SoundPool(4, AudioManager.STREAM_MUSIC, 0)
        mSoundPoolMap = HashMap()
        mAudioManager = mContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun addSound(Index: Int, SoundID: Int) {
        mSoundPoolMap!![Index] = mSoundPool!!.load(mContext, SoundID, 1)
    }

    open fun playSound(index: Int) {

        val streamVolume = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        mSoundPool!!.play(mSoundPoolMap!![index]!!, streamVolume.toFloat(), streamVolume.toFloat(), 1, 0, 1f)
    }

    fun playLoopedSound(index: Int) {

        val streamVolume = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        mSoundPool!!.play(mSoundPoolMap!![index]!!, streamVolume.toFloat(), streamVolume.toFloat(), 1, -1, 1f)
    }

}
