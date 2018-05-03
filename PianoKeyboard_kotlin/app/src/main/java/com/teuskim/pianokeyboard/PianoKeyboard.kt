package com.teuskim.pianokeyboard

import java.util.ArrayList

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.Xml

/**
 * 키보드 모델 클래스
 * 1. xml 로부터 키 데이터를 만들어 저장하고,
 * 2. 필요한 키 데이터를 리턴해준다.
 *
 * @author kim5724
 */
class PianoKeyboard {

    private var mContext: Context? = null
    private var mXmlLayoutResId = 0
    private var mKeyList: MutableList<Key>? = null
    var isCustom = false
    private var mMaxLabelSize = 2

    // 키 데이터를 리턴한다.
    val keyList: List<Key>?
        get() = mKeyList

    constructor(context: Context, xmlLayoutResId: Int) {
        // 필요한 멤버 변수 초기화하고, xml 로딩한다.
        mContext = context
        mXmlLayoutResId = xmlLayoutResId
        loadKeyboard()
    }

    @JvmOverloads constructor(context: Context, keyStringMap: Map<Int, String>, maxLabelSize: Int = 2) {
        mContext = context
        mMaxLabelSize = maxLabelSize
        isCustom = true
        loadKeyboard(keyStringMap)
    }

    fun loadKeyboard() {
        if (mXmlLayoutResId == 0)
            return

        // xml 파싱하여 키 데이터를 만들어서 저장한다.
        mKeyList = ArrayList()
        val parser = mContext!!.resources.getXml(mXmlLayoutResId)

        try {
            var event: Int = parser.next()
            while (event != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (TAG_KEYBOARD == tag) {

                    } else if (TAG_KEY == tag) {
                        val `as` = Xml.asAttributeSet(parser)
                        val key = `as`.getAttributeIntValue(0, 0)
                        val keyLabel = `as`.getAttributeValue(1)
                        val iconResId = `as`.getAttributeResourceValue(2, 0)
                        mKeyList!!.add(Key(key, keyLabel, iconResId, isCustom, keyLabel))
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                }
                event = parser.next()
            }
        } catch (e: Exception) {
        }

    }

    fun loadKeyboard(keyStringMap: Map<Int, String>) {
        mKeyList = ArrayList()
        val size = WHITE_NUM + BLACK_NUM
        for (i in 0 until size) {
            var key: Key? = null
            if (keyStringMap.containsKey(i)) {
                val keyData = keyStringMap[i]
                val keyLabelSize: Int
                if (keyData != null) {
                    if (keyData.length > mMaxLabelSize)
                        keyLabelSize = mMaxLabelSize
                    else
                        keyLabelSize = keyData.length
                    key = Key(i, keyData.substring(0, keyLabelSize), 0, isCustom, keyData)
                } else {
                    key = Key(i, "", 0, isCustom, "")
                }
            }
            key?.let { mKeyList!!.add(it) }
        }
    }

    /**
     * 키 객체
     */
    class Key(var keyCode: Int, var keyLabel: String?, var keyIcon: Int, val isCustom: Boolean, val keyData: String)

    companion object {

        // Keyboard XML Tags
        private val TAG_KEYBOARD = "Keyboard"
        private val TAG_KEY = "Key"

        val WHITE_NUM = 16
        val BLACK_NUM = 12
    }

}
