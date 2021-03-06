package com.teuskim.pianokeyboard

import java.util.Date
import java.util.HashMap

import android.content.Context
import android.media.AudioManager

class PianoSoundManager private constructor(context: Context) : SoundManager() {
    private var mSoundMode = SoundMode.RECOMMENDED
    private val mReservedPlayer: ReservedPlayer
    private val mAudioManager: AudioManager
    private var mIsSoundOffIfSilent = true

    init {
        initSounds(context)
        addSound(0, R.raw.sound_c)  // 도
        addSound(1, R.raw.sound_d)  // 레
        addSound(2, R.raw.sound_e)  // 미
        addSound(3, R.raw.sound_f)  // 파
        addSound(4, R.raw.sound_g)  // 솔
        addSound(5, R.raw.sound_a)  // 라
        addSound(6, R.raw.sound_b)  // 시
        addSound(7, R.raw.sound_2c)  // 도
        addSound(8, R.raw.sound_2c)  // 도
        addSound(9, R.raw.sound_2d)  // 레
        addSound(10, R.raw.sound_2e)  // 미
        addSound(11, R.raw.sound_2f)  // 파
        addSound(12, R.raw.sound_2g)  // 솔
        addSound(13, R.raw.sound_2a)  // 라
        addSound(14, R.raw.sound_2b)  // 시
        addSound(15, R.raw.sound_3c)  // 도
        addSound(16, R.raw.sound_cc)  // 도#
        addSound(17, R.raw.sound_dd)  // 레#
        addSound(18, R.raw.sound_ff)  // 파#
        addSound(19, R.raw.sound_gg)  // 솔#
        addSound(20, R.raw.sound_aa)  // 라#
        addSound(21, R.raw.sound_2cc)  // 도#
        addSound(22, R.raw.sound_2cc)  // 도#
        addSound(23, R.raw.sound_2dd)  // 레#
        addSound(24, R.raw.sound_2ff)  // 파#
        addSound(25, R.raw.sound_2gg)  // 솔#
        addSound(26, R.raw.sound_2aa)  // 라#
        addSound(27, R.raw.sound_3cc)  // 도#

        addSound(28, R.raw.sound_space)  // 스페이스바
        addSound(29, R.raw.sound_back)  // 백스페이스
        addSound(30, R.raw.sound_enter)  // 엔터
        addSound(31, R.raw.sound_shift)  // 시프트
        addSound(32, R.raw.sound_repeat)  // 반복

        mReservedPlayer = ReservedPlayer()
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun setSoundMode(soundMode: Int) {
        mSoundMode = soundMode
    }

    fun setIsSoundOffIfSilent(isSoundOffIfSilent: Boolean) {
        mIsSoundOffIfSilent = isSoundOffIfSilent
    }

    override fun playSound(index: Int) {
        when (mSoundMode) {
            SoundMode.RECOMMENDED -> mReservedPlayer.playSound(index)
            SoundMode.ORIGINAL -> superPlaySound(index)
        }
    }

    fun superPlaySound(index: Int) {
        val ringerMode = mAudioManager.ringerMode
        if (mSoundMode != SoundMode.NONE) {
            if (mIsSoundOffIfSilent == false || mIsSoundOffIfSilent == true && ringerMode != AudioManager.RINGER_MODE_SILENT && ringerMode != AudioManager.RINGER_MODE_VIBRATE)
                super.playSound(index)
        }
    }

    fun playSoundFromPlayer(index: Int) {
        super.playSound(index)
    }

    fun updateLastPlayTime() {
        mReservedPlayer.setLastTime(Date().time)
    }

    fun resetLastPlayTime() {
        mReservedPlayer.setLastTime(0)
    }

    private inner class ReservedPlayer {

        private val mMap: MutableMap<Int, String>
        private var mLastTime: Long = 0
        private var mLastIndex = 0
        private var mCurrStr: String? = null

        init {
            mMap = HashMap()
            mMap[0] = "000004040505040303020201010004040303020201040403030202010000040405050403030202010100"  // 작은별
            mMap[1] = "010102010418010102010504010906041802070706040504"  // 생일축하
            mMap[2] = "02020100000101020100040403020201000102000202030404050504030202030404050505040202010000010102010004040302020100010200"  // 주먹쥐고
            mMap[3] = "030400072005040520050403020302010203040007200504052005040302030001"  // 타이타닉
            mMap[4] = "040405040707091009100705070904040504070709100909090910071212121009070504050709100913131313131213121012131210091212121210090705040507091007"  // 진짜사나이
            mMap[5] = "050921060905180509102412241013122410092106051805091010240921091021241213241009210910131224100921060518050910102409"  // 애국가
            mMap[6] = "06060606060606090405060707070707060606060505040509060606060606060904050607070707070606060909070504"  // 징글벨
            mMap[7] = "070812121313121111101009090812121111101009121211111010090808121213131211111010090908"  // 작은별(2)
            mMap[8] = "080812121313121111101009090812121111101009121211111010090808121213131211111010090908"  // 작은별(2)
            mMap[9] = "09060607050504050607090909090606060705050406090906060605050505050607060606060607090906060607050504060909060606"  // 나비야
            mMap[10] = "10231023100609070500020506021906071023102310060907050002050602070605060709100411100903100907020907061023102310060907050002050602070605"  // 엘리제를 위하여
            mMap[11] = "11111111232222072020221126262626252424112323112411241113241111232222072007070707220720202020"  // 로망스
            mMap[12] = "121213131212101212101009121213131212101210091007"  // 학교종
            mMap[13] = "13131313121111100909111312131313131226131211101011"  // 아빠힘내세요
            mMap[14] = "142524102425142524102425242514251427252714252410"  // 아침
            mMap[15] = "15132615132615070910111213261311121305200709072007111011"  // 캐논변주곡
            mMap[16] = "1617030316031920191617030316031920192019032019031717161703191920201920222222191903171617031603192019170318031716"  // 퐁당퐁당
            mMap[17] = "1719191917200419171922220720192019041920171919191720041917190723071903200719"  // 결혼행진곡
            mMap[18] = "180510090504181818040506051805100905041805050621090910050521060518050906091009210518051009050418050506210909"  // 에델바이스
            mMap[19] = "192019201903171621201903192020192019031716170317031617171617030303162019200721192120190316031920190318031716"  // 아빠하고나하고
            mMap[20] = "20042004200720042004170304032004200420072023072004030417"  // 개나리
            mMap[21] = "2111252111252626262524242411111123232321"  // 무엇이똑같을까
            mMap[22] = "2211252111252626262524242411111123232321"  // 무엇이똑같을까
            mMap[23] = "2322182424232218242423221824172416111123221611112322161111232216111711182424"  // 고양이춤
            mMap[24] = "240621091024092409240609060409061009212406210910240924092406090604090606210910241213122412131413121314271424092106210910241213122412131413121314151210121527"  // 백조의 호수
            mMap[25] = "252527152515262526251111252409232524112322"  // Marble Halls
            mMap[26] = "26132615121123092311070910111215112626131211"  // 위풍당당행진곡
            mMap[27] = "27131410101427132714131010142713"  // 시계탑 종소리
        }

        fun playSound(index: Int) {
            val lastTime = Date().time
            if (mLastTime == 0L || lastTime - mLastTime > GAP) {
                mCurrStr = mMap[index]
                mLastIndex = 0
            } else {
                mLastIndex++
                if (mLastIndex >= mCurrStr!!.length / 2) {
                    mCurrStr = mMap[index]
                    mLastIndex = 0
                }
            }

            mLastTime = lastTime
            val playIndex = Integer.valueOf(mCurrStr!!.substring(mLastIndex * 2, mLastIndex * 2 + 2))!!
            superPlaySound(playIndex!!)
        }

        fun setLastTime(time: Long) {
            mLastTime = time
        }
    }

    companion object {

        private val GAP = 1500

        private var sInstance: PianoSoundManager? = null

        fun getInstance(context: Context): PianoSoundManager {
            if (sInstance == null) {
                sInstance = PianoSoundManager(context)
            }
            return sInstance as PianoSoundManager
        }
    }

}
