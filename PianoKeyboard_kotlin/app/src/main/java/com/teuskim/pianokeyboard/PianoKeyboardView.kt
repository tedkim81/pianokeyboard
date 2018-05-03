package com.teuskim.pianokeyboard

import java.util.ArrayList
import java.util.HashMap

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout

class PianoKeyboardView : LinearLayout {

    private var mContext: Context? = null
    private var mWhiteKeyViewList: MutableList<KeyView>? = null
    private var mBlackKeyViewList: MutableList<KeyView>? = null
    private var mPressedViewMap: MutableMap<Int, KeyView>? = null

    private var mWhiteWidth: Int = 0
    private var mBlackWidth: Int = 0
    private var mSmallGap: Int = 0
    private var mBigGap: Int = 0

    private var mKeyboardActionListener: OnKeyboardActionListener? = null
    var keyboard: PianoKeyboard? = null
        set(keyboard) {
            field = keyboard
            val keyList = keyboard!!.keyList
            var i = 0
            for (kv in mWhiteKeyViewList!!) {
                val key = keyList!![i++]
                if (key != null)
                    kv.setText(key.keyLabel!!)
                else
                    kv.setText("")
                if (mTextSize > 0)
                    kv.setTextSize(mTextSize)
                mKeyMap!![kv] = key
            }
            for (kv in mBlackKeyViewList!!) {
                val key = keyList!![i++]
                if (key != null)
                    kv.setText(key.keyLabel!!)
                else
                    kv.setText("")
                if (mTextSize > 0)
                    kv.setTextSize(mTextSize)
                mKeyMap!![kv] = key

                kv.alignCenter()
            }
        }
    private var mKeyMap: MutableMap<KeyView, PianoKeyboard.Key>? = null
    private var mIsRegisterMode = false
    private var mTextSize = 0

    interface OnKeyboardActionListener {
        /**
         * MotionEvent.ACTION_DOWN 발생시 호출
         */
        fun onTouchDown(keyType: Int, index: Int, key: PianoKeyboard.Key)

        /**
         * MotionEvent.ACTION_MOVE 발생시 호출
         */
        fun onTouchMove()

        /**
         * MotionEvent.ACTION_UP 발생시 호출
         */
        fun onTouchUp()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        // xml layout 으로 뷰를 생성하고, 멤버 변수들을 초기화 한다.
        mContext = context
        LayoutInflater.from(context).inflate(R.layout.keyboard, this)

        mWhiteKeyViewList = ArrayList()
        mBlackKeyViewList = ArrayList()
        mPressedViewMap = HashMap()
        mKeyMap = HashMap()

        fillKeyViewList(mWhiteKeyViewList as ArrayList<KeyView>, findViewById<KeyView>(R.id.key_white_top) as LinearLayout)
        fillKeyViewList(mBlackKeyViewList as ArrayList<KeyView>, findViewById<KeyView>(R.id.key_black_top) as LinearLayout)
        fillKeyViewList(mWhiteKeyViewList as ArrayList<KeyView>, findViewById<KeyView>(R.id.key_white_bottom) as LinearLayout)
        fillKeyViewList(mBlackKeyViewList as ArrayList<KeyView>, findViewById<KeyView>(R.id.key_black_bottom) as LinearLayout)
    }

    private fun fillKeyViewList(keyViewList: MutableList<KeyView>, layout: LinearLayout) {
        var j = 0
        for (i in 0 until layout.childCount) {
            if (layout.getChildAt(j) is KeyView)
                keyViewList.add(layout.getChildAt(j) as KeyView)
            j++
        }
    }

    private fun setKeyBlackLayoutParams(keyLayout: KeyView, keyWidth: Int, keyHeight: Int, leftMargin: Int) {
        val m1 = ViewGroup.MarginLayoutParams(keyWidth, keyHeight)
        m1.setMargins(leftMargin, 0, 0, 0)
        keyLayout.layoutParams = LinearLayout.LayoutParams(m1)
    }

    fun setIsRegisterMode(isRegisterMode: Boolean) {
        mIsRegisterMode = isRegisterMode
    }

    fun adjustLayoutParams(portraitRatio: Double, horizontalRatio: Double, horizontalWidthRatio: Double) {
        val display = (mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        var keyboardHeight: Int
        val keyboardWidth: Int
        val isPortrait = display.width < display.height
        if (isPortrait) {  // portrait
            keyboardHeight = (display.height / portraitRatio).toInt()
            keyboardWidth = display.width
        } else {  // horizontal
            keyboardHeight = (display.height / horizontalRatio).toInt()
            keyboardWidth = (display.width * horizontalWidthRatio).toInt()
        }
        val lp: LinearLayout.LayoutParams
        if (!isPortrait && horizontalRatio == 1.0) {
            lp = LinearLayout.LayoutParams(keyboardWidth, LinearLayout.LayoutParams.FILL_PARENT)
            keyboardHeight = (keyboardHeight * 0.8).toInt()
        } else {
            lp = LinearLayout.LayoutParams(keyboardWidth, keyboardHeight)
        }
        layoutParams = lp
        mWhiteWidth = keyboardWidth / 8
        mBlackWidth = ((keyboardWidth / 8).toDouble() * 0.9).toInt()
        mSmallGap = mWhiteWidth - mBlackWidth
        mBigGap = mWhiteWidth * 2 - mBlackWidth

        val blackHeight = keyboardHeight / 4
        setKeyBlackLayoutParams(mBlackKeyViewList!![0], mBlackWidth, blackHeight, mWhiteWidth - mBlackWidth / 2)
        setKeyBlackLayoutParams(mBlackKeyViewList!![1], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![2], mBlackWidth, blackHeight, mBigGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![3], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![4], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![5], mBlackWidth, blackHeight, 0)
        setKeyBlackLayoutParams(mBlackKeyViewList!![6], mBlackWidth, blackHeight, mWhiteWidth / 2)
        setKeyBlackLayoutParams(mBlackKeyViewList!![7], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![8], mBlackWidth, blackHeight, mBigGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![9], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![10], mBlackWidth, blackHeight, mSmallGap)
        setKeyBlackLayoutParams(mBlackKeyViewList!![11], mBlackWidth, blackHeight, 0)
    }

    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener) {
        mKeyboardActionListener = listener
    }

    fun setTextSize(textSize: Int) {
        mTextSize = textSize
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> actionDown(action, event)

            MotionEvent.ACTION_MOVE -> if (mIsRegisterMode == false)
                actionMove(event)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> actionUp(action, event)
        }
        return true
    }

    private fun actionDown(action: Int, event: MotionEvent) {
        val pointerIndex = getPointerIndex(action)
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val keyIndex = getKeyIndexByXY(x, y)
        val pressedView = getKeyViewByXY(y, keyIndex)
        pressedView.isPressed = true
        mPressedViewMap!![pointerId] = pressedView

        if (mKeyboardActionListener != null) {
            mKeyboardActionListener!!.onTouchDown(pressedView.keyType, keyIndex, mKeyMap!![pressedView]!!)
        }
    }

    private fun actionMove(event: MotionEvent) {
        val pointerCnt = event.pointerCount
        var pressChanged = false
        var pressedView: KeyView? = null
        for (i in 0 until pointerCnt) {
            val x = event.getX(i)
            val y = event.getY(i)
            val keyIndex = getKeyIndexByXY(x, y)
            pressedView = getKeyViewByXY(y, keyIndex)
            if (mPressedViewMap!!.containsValue(pressedView) == false) {
                pressChanged = true
                if (mKeyboardActionListener != null) {
                    mKeyboardActionListener!!.onTouchDown(pressedView.keyType, keyIndex, mKeyMap!![pressedView]!!)
                }
                break
            }
        }
        if (pressChanged) {
            val it = mPressedViewMap!!.keys.iterator()
            while (it.hasNext()) {
                mPressedViewMap!![it.next()]!!.setPressed(false)
            }
            for (i in 0 until pointerCnt) {
                val x = event.getX(i)
                val y = event.getY(i)
                val keyIndex = getKeyIndexByXY(x, y)
                pressedView = getKeyViewByXY(y, keyIndex)
                pressedView.isPressed = true
                val pointerId = event.getPointerId(i)
                mPressedViewMap!![pointerId] = pressedView
            }
        }
    }

    private fun actionUp(action: Int, event: MotionEvent) {
        mPressedViewMap!!.remove(event.getPointerId(getPointerIndex(action)))!!.setPressed(false)

        if (mKeyboardActionListener != null) {
            mKeyboardActionListener!!.onTouchUp()
        }
    }

    private fun getPointerIndex(action: Int): Int {
        return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
    }

    private fun getKeyIndexByXY(x: Float, y: Float): Int {
        val hCnt = y.toInt() / (height / 4)
        var index = 0
        if (hCnt == 0 || hCnt == 2) {  // black
            if (x <= mWhiteWidth * 1.5) {
                index = 0
            } else if (x <= mWhiteWidth * 3) {
                index = 1
            } else if (x <= mWhiteWidth * 4.5) {
                index = 2
            } else if (x <= mWhiteWidth * 5.5) {
                index = 3
            } else if (x <= mWhiteWidth * 7) {
                index = 4
            } else {
                index = 5
            }

            if (hCnt == 2)
                index += 6
        } else {  // white
            index = x.toInt() / mWhiteWidth

            if (hCnt == 3)
                index += 8
        }
        return index
    }

    private fun getKeyViewByXY(y: Float, keyIndex: Int): KeyView {
        val hCnt = y.toInt() / (height / 4)
        return if (hCnt == 0 || hCnt == 2) {
            mBlackKeyViewList!![keyIndex]
        } else {
            mWhiteKeyViewList!![keyIndex]
        }
    }

}
