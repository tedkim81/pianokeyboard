package com.teuskim.pianokeyboard

import java.util.ArrayList
import java.util.Calendar
import java.util.Collections
import java.util.HashMap

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import com.google.android.apps.analytics.GoogleAnalyticsTracker
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener


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
class PianoKeyboardService : InputMethodService(), OnKeyboardActionListener, OnClickListener {

    private var mSoundManager: PianoSoundManager? = null
    private var mKeyboardView: PianoKeyboardView? = null
    private var mEnglishKeyboard: PianoKeyboard? = null
    private var mEnglishKeyboardShift: PianoKeyboard? = null
    private var mHangulKeyboard: PianoKeyboard? = null
    private var mHangulKeyboardShift: PianoKeyboard? = null
    private var mSymbolKeyboard: PianoKeyboard? = null
    private var mSymbolKeyboardShift: PianoKeyboard? = null
    private val mComposing = StringBuilder()
    private var mBtnChangeKeyboard: Button? = null
    private var mBtnBackspace: Button? = null
    private var mBtnSpace: Button? = null
    private var mBtnEnter: Button? = null
    private var mBtnShift: Button? = null
    private var mIsPressedBtnShift: Boolean = false
    private var mBtnRepeat: Button? = null
    private var mBtnSettings: Button? = null
    private var mHangulHandler: HangulHandler? = null
    private val mKeyboardList = ArrayList<PianoKeyboard>()
    private val mNameMap = HashMap<PianoKeyboard, String>()
    private val mHandler = object : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            // msg.what 이 keycode 이다.
            mHangulHandler!!.handle(mHangulHandler!!.getKeyIntFromKeyCode(msg.what))
        }

    }
    private var mRepeatedString: String? = null
    private var mDb: PianoKeyboardDb? = null
    private var mWordListLayout: LinearLayout? = null
    private var mWordListTask: WordListTask? = null
    private var mInputString: String? = null
    private var mCurrTypeTextVariation: Int = 0
    private var mKeyCodeEnter = KeyEvent.KEYCODE_ENTER
    private var mIsInserting = false

    private var mTracker: GoogleAnalyticsTracker? = null

    private val isHangulInput: Boolean
        get() = mHangulHandler!!.composingLength > 0

    private val allText: String
        get() {
            val ic = currentInputConnection
            return ic.getTextBeforeCursor(100, 0) as String + ic.getTextAfterCursor(100, 0) as String
        }

    private val currentWord: String
        get() = if (isHangulInput) {
            mHangulHandler!!.composing.toString()
        } else {
            mComposing.toString()
        }

    private val lastChar: Char
        get() = if (mInputString != null && mInputString!!.length > 0) {
            mInputString!![mInputString!!.length - 1]
        } else ' '

    private val inputType: Int
        get() {
            val info = currentInputEditorInfo
            return info.inputType and EditorInfo.TYPE_MASK_VARIATION
        }

    private val mWordListRunnable = WordListRunnable()

    override fun onCreate() {
        // 객체 생성시 호출. 변경되지 않는 멤버 변수 초기화
        Log.d(TAG, "onCreate!!")
        super.onCreate()

        mDb = PianoKeyboardDb.getInstance(applicationContext)
        mSoundManager = PianoSoundManager.getInstance(applicationContext)
        mTracker = GoogleAnalyticsTracker.getInstance()
        mTracker!!.startNewSession("UA-33008558-1", 20 * 60, application)
    }

    override fun onInitializeInterface() {
        // 뭔가 설정이 변경될 때마다 호출된다. 여기에 UI관련 멤버변수를 모두 셋팅한다.
        Log.d(TAG, "onInitializeInterface!!")
        super.onInitializeInterface()

        mEnglishKeyboard = PianoKeyboard(this, R.xml.keyboard_english)
        mEnglishKeyboardShift = PianoKeyboard(this, R.xml.keyboard_english_shift)
        mNameMap.put(mEnglishKeyboard!!, shortNameIfNeeded(getString(R.string.txt_english))!!)

        mHangulKeyboard = PianoKeyboard(this, R.xml.keyboard_hangul)
        mHangulKeyboardShift = PianoKeyboard(this, R.xml.keyboard_hangul_shift)
        mNameMap.put(mHangulKeyboard!!, shortNameIfNeeded(getString(R.string.txt_hangul))!!)
        mHangulHandler = HangulHandler(this)

        mSymbolKeyboard = PianoKeyboard(this, R.xml.keyboard_symbols)
        mSymbolKeyboardShift = PianoKeyboard(this, R.xml.keyboard_symbols_shift)
        mNameMap.put(mSymbolKeyboard!!, shortNameIfNeeded(getString(R.string.txt_symbols))!!)
    }

    private fun shortNameIfNeeded(name: String?): String? {
        var name = name
        if (name != null && name.length > 3)
            name = name.substring(0, 3)
        return name
    }

    override fun onCreateInputView(): View {
        // 키보드뷰를 최초 출력할 때와 설정이 변경될 때마다 호출된다. 키보드뷰를 생성하여 리턴한다.
        Log.d(TAG, "onCreateInputView!!")

        val inputView = layoutInflater.inflate(R.layout.input, null)
        mKeyboardView = inputView.findViewById<View>(R.id.keyboard) as PianoKeyboardView
        mKeyboardView!!.adjustLayoutParams(2.6, 1.6, 0.65)
        mKeyboardView!!.keyboard = mEnglishKeyboard
        mKeyboardView!!.setOnKeyboardActionListener(this)
        mBtnChangeKeyboard = inputView.findViewById<View>(R.id.btn_changekeyboard) as Button
        mBtnChangeKeyboard!!.text = shortNameIfNeeded(getString(R.string.txt_english))
        mBtnBackspace = inputView.findViewById<View>(R.id.btn_backspace) as Button
        mBtnSpace = inputView.findViewById<View>(R.id.btn_space) as Button
        mBtnEnter = inputView.findViewById<View>(R.id.btn_enter) as Button
        mBtnShift = inputView.findViewById<View>(R.id.btn_shift) as Button
        mIsPressedBtnShift = false
        mBtnRepeat = inputView.findViewById<View>(R.id.btn_repeat) as Button
        mBtnSettings = inputView.findViewById<View>(R.id.btn_settings) as Button
        mWordListLayout = inputView.findViewById<View>(R.id.history_list_layout) as LinearLayout
        mBtnChangeKeyboard!!.setOnClickListener(this)
        mBtnShift!!.setOnClickListener(this)
        mBtnSettings!!.setOnClickListener(this)

        val listener = RepeatListener(this)
        mBtnBackspace!!.setOnTouchListener(listener)
        mBtnSpace!!.setOnTouchListener(listener)
        mBtnEnter!!.setOnTouchListener(listener)
        mBtnRepeat!!.setOnTouchListener(listener)

        return inputView
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        // 키보드뷰 셋팅이 완료된 후 호출된다. 키보드 객체를 셋팅한다.
        Log.d(TAG, "onStartInputView!!")
        super.onStartInputView(info, restarting)

        mKeyboardList.clear()
        val english = getString(R.string.txt_english)
        val hangul = getString(R.string.txt_hangul)
        val symbols = getString(R.string.txt_symbols)
        val list = mDb!!.keySetList
        for (ks in list) {
            if ("Y" == ks.mShowYN) {
                val keyboardName = mDb!!.getKeyboardName(ks.mType)
                if (english == keyboardName)
                    mKeyboardList.add(mEnglishKeyboard!!)
                else if (hangul == keyboardName)
                    mKeyboardList.add(mHangulKeyboard!!)
                else if (symbols == keyboardName)
                    mKeyboardList.add(mSymbolKeyboard!!)
            }
        }
        val cksList = mDb!!.customKeySetList
        for (cks in cksList) {
            if ("Y" == cks.mShowYN) {
                val cksdList = mDb!!.getCustomKeySetDataList(cks.mId)
                val map = HashMap<Int, String>()
                for (cksd in cksdList) {
                    map.put(cksd.mPosition, cksd.mData!!)
                }
                val customKeyboard = PianoKeyboard(applicationContext, map)
                mKeyboardList.add(customKeyboard)
                mNameMap.put(customKeyboard, shortNameIfNeeded(cks.mName)!!)
            }
        }

        when (info.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_DATETIME, EditorInfo.TYPE_CLASS_PHONE -> {
                mKeyboardView!!.keyboard = mSymbolKeyboard
                mBtnChangeKeyboard!!.text = mNameMap.get(mSymbolKeyboard)
            }
            else -> try {
                val keyboard = mKeyboardList[mDb!!.keyboardPosition]
                mKeyboardView!!.keyboard = keyboard
                mBtnChangeKeyboard!!.text = mNameMap[keyboard]
            } catch (e: Exception) {
                mDb!!.updateKeyboardPosition(0)
            }

        }

        mSoundManager!!.setSoundMode(mDb!!.soundMode)
        mSoundManager!!.setIsSoundOffIfSilent(mDb!!.isSoundOffIfSilent)
        showWordList()

        // google analytics
        mTracker!!.trackPageView("PianoKeyboardService")

        mCurrTypeTextVariation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        // 키보드뷰가 사라지면 호출된다.
        Log.d(TAG, "onFinishInputView!!")

        // 입력된 내용을 저장한다.
        wordInsert(mInputString)

        super.onFinishInputView(finishingInput)

        finishInput()
        mRepeatedString = null
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        // 키 입력전 호출되며 가장 중요한 부분이다. 각종 멤버변수를 셋팅하는 등의 입력 준비 단계를 마무리 짓는다.
        Log.d(TAG, "onStartInput!!")
        super.onStartInput(attribute, restarting)

        when (attribute.imeOptions and (EditorInfo.IME_MASK_ACTION or EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            EditorInfo.IME_ACTION_NEXT -> mKeyCodeEnter = KeyEvent.KEYCODE_TAB
            else -> mKeyCodeEnter = KeyEvent.KEYCODE_ENTER
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int,
                                   newSelStart: Int, newSelEnd: Int, candidatesStart: Int,
                                   candidatesEnd: Int) {

        Log.d(TAG, "oldStart:$oldSelStart, oldEnd:$oldSelEnd, newStart:$newSelStart, newEnd:$newSelEnd, candStart:$candidatesStart, candEnd:$candidatesEnd")
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        if ((mComposing.length > 0 || mHangulHandler!!.composingLength > 0) && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {

            finishInput()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    override fun onFinishInput() {
        // 키 입력이 완료되면 호출된다. 각종 상태를 리셋한다.
        Log.d(TAG, "onFinishInput!!")
        super.onFinishInput()

        finishInput()
    }

    private fun finishInput() {
        mComposing.setLength(0)
        mHangulHandler!!.initHangulData()
    }

    override fun onDestroy() {
        // 객체가 종료될 때 호출된다. 메모리릭 방지를 위한 종료처리 등을 한다.
        Log.d(TAG, "onDestroy!!")
        mTracker!!.stopSession()
        super.onDestroy()
    }

    fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    override fun onClick(v: View) {
        var isTextChanged = false

        when (v.id) {
            R.id.btn_changekeyboard -> {
                changeKeyboard()
                mSoundManager!!.resetLastPlayTime()
            }
            R.id.btn_backspace -> {
                mSoundManager!!.superPlaySound(29)
                handleBackspace()
                mSoundManager!!.resetLastPlayTime()
                isTextChanged = true
            }
            R.id.btn_space -> {
                mSoundManager!!.superPlaySound(28)
                handleSpace()
                mSoundManager!!.updateLastPlayTime()
                isTextChanged = true
            }
            R.id.btn_enter -> {
                mSoundManager!!.superPlaySound(30)
                handleEnter()
                mSoundManager!!.updateLastPlayTime()
                isTextChanged = true
            }
            R.id.btn_shift -> {
                mSoundManager!!.superPlaySound(31)
                handleShift()
                mSoundManager!!.resetLastPlayTime()
            }
            R.id.btn_repeat -> {
                mSoundManager!!.superPlaySound(32)
                handleRepeat()
                mSoundManager!!.updateLastPlayTime()
                isTextChanged = true
            }
            R.id.btn_settings -> goSettings()
        }
        changeSpacebarText()

        if (isTextChanged) {
            // 입력내용 저장
            showWordList()
        }
    }

    override fun onTouchDown(keyType: Int, index: Int, key: PianoKeyboard.Key) {
        val soundIndex: Int
        if (keyType == KeyView.KEY_TYPE_WHITE) {
            soundIndex = index
        } else {
            soundIndex = index + PianoKeyboard.WHITE_NUM
        }

        mSoundManager!!.playSound(soundIndex)

        handleInput(key)
        changeSpacebarText()
    }

    override fun onTouchMove() {}

    override fun onTouchUp() {}

    private fun handleInput(key: PianoKeyboard.Key?) {
        // 키 입력
        if (key == null || key.isCustom) {
            handleCustom(key)
        } else {
            if (isHangulInput(key)) {
                handleHangul(key)
            } else {
                handleCommon(key)
            }
        }

        // 입력내용 저장
        showWordList()
    }

    private fun changeSpacebarText() {
        if (mHangulHandler!!.isInit == false) {
            mBtnSpace!!.setText(R.string.btn_commit)
        } else {
            mBtnSpace!!.setText(R.string.btn_space)
        }
    }

    private fun handleCommon(key: PianoKeyboard.Key) {
        mHangulHandler!!.commit()

        val selectedStr = key.keyCode.toChar().toString()
        mComposing.append(selectedStr)
        if (Character.isLetterOrDigit(key.keyCode)) {
            setComposingText(mComposing, 1)
        } else {
            commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
        }
    }

    private fun handleHangul(key: PianoKeyboard.Key) {
        if (mComposing.length > 0) {
            commitText(mComposing, 1)
            mComposing.setLength(0)
            mHandler.sendEmptyMessageDelayed(key.keyCode, 50)
        } else {
            mHangulHandler!!.handle(mHangulHandler!!.getKeyIntFromKeyCode(key.keyCode))
        }
    }

    private fun handleCustom(key: PianoKeyboard.Key?) {
        if (key != null) {
            val keyInt = mHangulHandler!!.getKeyIntFromJaso(key.keyData)
            if (keyInt >= 0) {
                if (mComposing.length > 0) {
                    commitText(mComposing, 1)
                    mComposing.setLength(0)
                }
                mHangulHandler!!.handle(keyInt)
            } else {
                mHangulHandler!!.commit()

                val selectedStr = key.keyData
                mComposing.append(selectedStr)
                if (selectedStr.length == 1 && Character.isLetterOrDigit(selectedStr[0])) {
                    setComposingText(mComposing, 1)
                } else {
                    commitText(mComposing, mComposing.length)
                    mComposing.setLength(0)
                }
            }
        }
    }

    fun setComposingText(text: CharSequence, pos: Int) {
        currentInputConnection.setComposingText(text, pos)
    }

    private fun commitText(text: CharSequence?, pos: Int) {
        currentInputConnection.commitText(text, pos)
    }

    fun commitHangul(text: CharSequence, pos: Int) {
        currentInputConnection.commitText(text, pos)
    }

    private fun changeKeyboard() {
        if (mIsPressedBtnShift) {
            handleShift()
        }
        val currPos = mKeyboardList.indexOf(mKeyboardView!!.keyboard)
        val nextPos = (currPos + 1) % mKeyboardList.size
        val nextKeyboard = mKeyboardList[nextPos]
        mKeyboardView!!.keyboard = nextKeyboard
        mBtnChangeKeyboard!!.text = mNameMap[nextKeyboard]
    }

    private fun isHangulInput(key: PianoKeyboard.Key): Boolean {
        return if (mHangulHandler!!.getKeyIntFromKeyCode(key.keyCode) >= 0) true else false
    }

    private fun handleBackspace() {
        if (isHangulInput) {
            mHangulHandler!!.handleBackspace()
        } else {
            val length = mComposing.length
            if (length > 1) {
                mComposing.delete(length - 1, length)
                currentInputConnection.setComposingText(mComposing, 1)
            } else if (length > 0) {
                mComposing.setLength(0)
                currentInputConnection.commitText("", 0)
            } else {
                keyDownUp(KeyEvent.KEYCODE_DEL)
            }
        }
    }

    private fun handleSpace() {
        if (isHangulInput) {
            mHangulHandler!!.handleSpace()
        } else {
            mHangulHandler!!.commit()
            mComposing.append(' ')
            commitText(mComposing, mComposing.length)
            mComposing.setLength(0)
        }
    }

    private fun handleEnter() {
        if (isHangulInput) {
            mHangulHandler!!.commit()
        }
        keyDownUp(mKeyCodeEnter)
    }

    private fun handleShift() {
        if (mKeyboardView!!.keyboard === mEnglishKeyboard) {
            mKeyboardView!!.keyboard = mEnglishKeyboardShift
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift_on)
            mIsPressedBtnShift = true
        } else if (mKeyboardView!!.keyboard === mEnglishKeyboardShift) {
            mKeyboardView!!.keyboard = mEnglishKeyboard
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift)
            mIsPressedBtnShift = false
        } else if (mKeyboardView!!.keyboard === mHangulKeyboard) {
            mKeyboardView!!.keyboard = mHangulKeyboardShift
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift_on)
            mIsPressedBtnShift = true
        } else if (mKeyboardView!!.keyboard === mHangulKeyboardShift) {
            mKeyboardView!!.keyboard = mHangulKeyboard
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift)
            mIsPressedBtnShift = false
        } else if (mKeyboardView!!.keyboard === mSymbolKeyboard) {
            mKeyboardView!!.keyboard = mSymbolKeyboardShift
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift_on)
            mIsPressedBtnShift = true
        } else if (mKeyboardView!!.keyboard === mSymbolKeyboardShift) {
            mKeyboardView!!.keyboard = mSymbolKeyboard
            mBtnShift!!.setBackgroundResource(R.drawable.btn_shift)
            mIsPressedBtnShift = false
        }
    }

    private fun handleRepeat() {
        if (isHangulInput) {
            if (mHangulHandler!!.composingLength > 0) {
                mRepeatedString = mHangulHandler!!.composing.toString()
                mHangulHandler!!.commit()
            }
        } else {
            if (mComposing.length > 0) {
                mRepeatedString = mComposing.toString()
                commitText(mComposing, 1)
            }
        }
        if (mRepeatedString != null && mRepeatedString!!.length > 0) {
            commitText(mRepeatedString, 1)
        }
    }

    private fun goSettings() {
        val i = Intent(this, SettingActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private inner class RepeatListener(private val mListener: OnClickListener) : View.OnTouchListener {

        private var mIsDown = false
        private var mView: View? = null

        private val mRepeatRunnable = Runnable {
            mListener.onClick(mView)
            delayedRepeat()
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            mView = v

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mIsDown = true
                    mHandler.postDelayed({ delayedRepeat() }, 300)
                }
                MotionEvent.ACTION_UP -> {
                    mIsDown = false
                    mHandler.removeCallbacksAndMessages(null)
                    mHandler.post(mRepeatRunnable)
                }
            }

            return false
        }

        private fun delayedRepeat() {
            if (mIsDown == true) {
                mHandler.postDelayed(mRepeatRunnable, 40)
            }
        }

    }

    private fun recordInputString() {
        if (inputType != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD) {
            if (mInputString == null || mInputString!!.length == 0)
                mDb!!.updateKeyboardPosition(mKeyboardList.indexOf(mKeyboardView!!.keyboard))

            val inputString = allText

            // 입력영역이 초기화가 되었다면 단어저장
            if (mInputString != null && mInputString!!.length > 1
                    && (inputString == null || inputString.length == 0)
                    && mIsInserting == false) {
                wordInsert(mInputString)
            }

            mInputString = inputString
            Log.d(TAG, "record input string : " + mInputString!!)
        }
    }

    private inner class WordListTask : AsyncTask<String, Int, List<RecommendWord>>() {

        override fun doInBackground(vararg params: String): List<RecommendWord> {
            var text: String? = currentWord
            if (text != null && getCharType(lastChar) != TYPE_SPACE)
                text = text.trim { it <= ' ' }
            else
                text = ""
            return recommendWords(text, mHangulHandler!!.separateJaso(text))
        }

        override fun onPostExecute(result: List<RecommendWord>?) {
            val inflater = LayoutInflater.from(applicationContext)
            mWordListLayout!!.removeAllViews()
            if (result != null && result.size > 0) {
                val min = Math.min(result.size, 10)
                for (i in 0 until min) {
                    val rw = result[i]
                    val word = rw.mWord

                    val v = inflater.inflate(R.layout.history_list_item, null)
                    (v.findViewById<View>(R.id.history_word) as TextView).text = word
                    v.setOnClickListener {
                        mComposing.setLength(0)
                        commitHangul("", 1)
                        mHangulHandler!!.initHangulData()

                        if (getCharType(word!![word.length - 1]) == TYPE_ETC)
                            commitText(word, 1)
                        else
                            commitText(word + " ", 1)
                        showWordList()

                        changeSpacebarText()
                    }
                    mWordListLayout!!.addView(v)
                }
            } else {
                // TODO: 예측 단어가 없을 경우 어떻게 할까? 기존에는 설정으로 보내는 알림 텍스트를 넣었으나, 다른게 좋을듯.
            }
        }

        private fun recommendWords(word: String, composition: String): List<RecommendWord> {

            // 앞단어 이용해서 친밀도 기반 목록 구한다.
            val affinityWordList = getAffinityWordList(word, composition)

            // 입력영역속성 기반 목록을 구한다.
            val attrWordList = getAttrWordList(composition)

            // 입력횟수기반 목록을 구한다.
            val useCntWordList = getUseCntWordList(composition)

            // 입력시각기반 목록을 구한다.
            val useTimeWordList = getUseTimeWordList(composition)

            // 모든 목록을 병합하여 내림차순으로 10개의 항목을 갖는 목록을 만들어 리턴한다.
            return mergeWordList(affinityWordList, attrWordList, useCntWordList, useTimeWordList)
        }

        private fun getAffinityWordList(word: String?, composition: String): List<RecommendWord> {
            val ic = currentInputConnection
            val prevText = ic.getTextBeforeCursor(50, 0) as String
            var resultList: MutableList<RecommendWord> = ArrayList()

            if (prevText.length > 0) {
                val texts = splitText(prevText)
                if (texts.size > 0)
                    resultList = mDb!!.getNextWordList(texts[texts.size - 1], composition).toMutableList()
            }

            if (word != null && word.length > 0 && getCharType(word[word.length - 1]) == TYPE_ETC) {
                resultList.addAll(mDb!!.getNextWordList(word, ""))
            }

            return resultList
        }

        private fun getAttrWordList(composition: String): List<RecommendWord> {
            val info = currentInputEditorInfo
            val typeTextVariation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
            return mDb!!.getWordListByAttr(composition, typeTextVariation)
        }

        private fun getUseCntWordList(composition: String): List<RecommendWord> {
            return mDb!!.getWordListByUseCnt(composition)
        }

        private fun getUseTimeWordList(composition: String): List<RecommendWord> {
            return mDb!!.getWordListByUseTime(composition)
        }

        private fun mergeWordList(affinityWordList: List<RecommendWord>, attrWordList: List<RecommendWord>, useCntWordList: List<RecommendWord>, useTimeWordList: List<RecommendWord>): List<RecommendWord> {

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

            val map = HashMap<Int, RecommendWord>()
            for (rw in affinityWordList) {
                map[rw.mWordId] = rw
            }
            for (rw in attrWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]!!
                    rwTmp.mUseCntXxx = rw.mUseCntXxx
                    rwTmp.mUseCntXxxSum = rw.mUseCntXxxSum
                } else {
                    map[rw.mWordId] = rw
                }
            }
            for (rw in useCntWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]!!
                    rwTmp.mUseCntTotal = rw.mUseCntTotal
                    rwTmp.mUseCntTotalSum = rw.mUseCntTotalSum
                } else {
                    map[rw.mWordId] = rw
                }
            }
            for (rw in useTimeWordList) {
                if (map.containsKey(rw.mWordId)) {
                    val rwTmp = map[rw.mWordId]!!
                    rwTmp.mUseCntN = rw.mUseCntN
                    rwTmp.mUseCntNSum = rw.mUseCntNSum
                } else {
                    map[rw.mWordId] = rw
                }
            }

            val iter = map.values.iterator()
            val myInfo = mDb!!.myInfo
            val availableTime = myInfo.mAvailablePeriod * 86400000L
            val resultList = ArrayList<RecommendWord>()
            while (iter.hasNext()) {
                val rw = iter.next()
                rw.generatePoint(AFFINITY_WEIGHT, myInfo.mUseCntXxxWeight.toDouble(), myInfo.mUseCntTotalWeight.toDouble(), myInfo.mUseCntNWeight.toDouble(), availableTime)
                resultList.add(rw)
            }
            Collections.sort(resultList)
            return resultList
        }
    }

    private fun wordInsert(inputString: String?) {
        // 입력된 내용을 저장한다.
        mIsInserting = true
        val task = WordInsertTask()
        task.execute(inputString)
    }

    private fun getCharType(ch: Char): Int {
        return if (ch.toInt() >= 97 && ch.toInt() <= 122 || ch.toInt() >= 65 && ch.toInt() <= 90 || ch.toInt() >= 0x3131 && ch.toInt() <= 0xd7a3)
            TYPE_CHAR
        else if (ch.toInt() == 9 || ch.toInt() == 10 || ch.toInt() == 13 || ch.toInt() == 32)
            TYPE_SPACE
        else
            TYPE_ETC
    }

    private fun splitText(inputText: String?): List<String> {
        val wordList = ArrayList<String>()
        if (inputText == null)
            return wordList

        val sb = StringBuilder()
        var currType = 0
        for (i in 0 until inputText.length) {
            val ch = inputText[i]
            val type = getCharType(ch)

            if (currType != TYPE_CHAR && type == TYPE_CHAR && sb.length > 0) {
                wordList.add(sb.toString())
                sb.setLength(0)
            }
            if (type != TYPE_SPACE) {
                sb.append(ch)
            }

            currType = type
        }
        if (sb.length > 0)
            wordList.add(sb.toString())

        return wordList
    }

    private inner class WordInsertTask : AsyncTask<String, Int, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val inputString = params[0]
            Log.d(TAG, "input string : " + inputString)
            if (inputString == null || inputString.length == 0)
                return false

            val inputText = inputString.trim { it <= ' ' }
            if (inputText.length > 0) {
                // 단어분리 {{
                val wordList = splitText(inputText)
                // }} 단어분리

                // 저장하기 {{
                var preWord: String? = null
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val columnUseCntXXX = mDb!!.getColumnUseCntXXX(mCurrTypeTextVariation)
                val columnUseCntN = mDb!!.getColumnUseCntN(hour)
                for (word in wordList) {
                    val composition = mHangulHandler!!.separateJaso(word)
                    mDb!!.insertOrUpdateWord(word, composition, mCurrTypeTextVariation, columnUseCntXXX, hour, columnUseCntN)

                    if (preWord != null)
                        mDb!!.insertOrUpdateNextWordGroup(preWord, word)
                    preWord = word

                    Log.d(TAG, "insert word: $word , composition: $composition , typeTextVariation: $mCurrTypeTextVariation")
                }
                // }} 저장하기

                // 분석 / 학습 {{
                // 가중치 갱신하기
                var weightTotal = mDb!!.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_TOTAL.toDouble(), PianoKeyboardDb.Word.USE_CNT_TOTAL)
                var weightN = mDb!!.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_N.toDouble(), columnUseCntN)
                var weightXXX = mDb!!.getUpdatedWeight(PianoKeyboardDb.WEIGHT_INIT_XXX.toDouble(), columnUseCntXXX)
                if (weightTotal > 0 && weightN > 0 && weightXXX > 0) {
                    val ratio = (PianoKeyboardDb.WEIGHT_SUM - PianoKeyboardDb.WEIGHT_INIT_NEXT) / (weightTotal + weightN + weightXXX)
                    weightTotal = weightTotal * ratio
                    weightN = weightN * ratio
                    weightXXX = weightXXX * ratio
                    mDb!!.updateWeight(weightTotal.toInt(), weightN.toInt(), weightXXX.toInt())
                }

                // 유효기간 갱신하기
                // TODO: 성능상의 문제가 있다. 나중에 하자.
                // }} 분석 / 학습

                return true
            }
            return false
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)

            if (result!!) {
                mInputString = null
            }
            mIsInserting = false
        }

    }

    fun showWordList() {
        recordInputString()

        mHandler.removeCallbacks(mWordListRunnable)
        mHandler.postDelayed(mWordListRunnable, 50)
    }

    private inner class WordListRunnable : Runnable {

        override fun run() {
            if (mWordListTask != null) {
                mWordListTask!!.cancel(true)
            }
            mWordListTask = WordListTask()
            mWordListTask!!.execute()
        }

    }

    companion object {

        private val TAG = "PianoKeyboardService"

        private val TYPE_CHAR = 1
        private val TYPE_SPACE = 2
        private val TYPE_ETC = 3
        private val AFFINITY_WEIGHT = 50.0
    }

}
