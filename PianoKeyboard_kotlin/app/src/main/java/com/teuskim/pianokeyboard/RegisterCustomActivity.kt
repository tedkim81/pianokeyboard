package com.teuskim.pianokeyboard

import java.util.HashMap
import java.util.TreeSet

import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.teuskim.pianokeyboard.PianoKeyboard.Key
import com.teuskim.pianokeyboard.PianoKeyboardView.OnKeyboardActionListener

class RegisterCustomActivity : BaseActivity(), OnClickListener, OnKeyboardActionListener {

    private var mNameCustom: EditText? = null
    private var mKeyboardView: PianoKeyboardView? = null
    private var mKeymapListView: LinearLayout? = null
    private var mBtnRecommend: Button? = null
    private var mBtnReset: Button? = null

    private var mKeymap: MutableMap<Int, String>? = null
    private var mPressedIndex: Int = 0
    private var mPianoKeyboard: PianoKeyboard? = null
    private var mCustomId: Int = 0
    private var mDb: PianoKeyboardDb? = null
    private var mInflater: LayoutInflater? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_custom)
        findViews()

        mDb = PianoKeyboardDb.getInstance(applicationContext)
        mKeymap = HashMap()
        mInflater = LayoutInflater.from(this)

        mCustomId = intent.getIntExtra("customId", 0)
        if (mCustomId > 0) {
            val name = mDb!!.getCustomKeySetName(mCustomId)
            mNameCustom!!.setText(name)

            val cksdList = mDb!!.getCustomKeySetDataList(mCustomId)
            for (cksd in cksdList) {
                mKeymap!!.put(cksd.mPosition, cksd.mData!!)
            }

            refreshKeymap()
        }

        // google analytics
        mTracker!!.trackPageView("RegisterCustomActivity")
    }

    protected fun findViews() {
        mNameCustom = findViewById<View>(R.id.name_custom) as EditText
        mKeyboardView = findViewById<View>(R.id.keyboard_view) as PianoKeyboardView
        mKeyboardView!!.setIsRegisterMode(true)
        mKeyboardView!!.adjustLayoutParams(3.0, 3.0, 1.0)
        mKeyboardView!!.setOnKeyboardActionListener(this)
        mKeymapListView = findViewById<View>(R.id.keymap_list) as LinearLayout
        mBtnRecommend = findViewById<View>(R.id.btn_custom_recommend) as Button
        mBtnReset = findViewById<View>(R.id.btn_custom_reset) as Button

        mKeyboardView!!.setOnClickListener(this)
        findViewById<View>(R.id.btn_save).setOnClickListener(this)
        findViewById<View>(R.id.btn_cancel).setOnClickListener(this)
        mBtnRecommend!!.setOnClickListener(this)
        mBtnReset!!.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_save -> save()
            R.id.btn_cancel -> finish()
            R.id.btn_custom_recommend -> dialogRecommendedCustom()
            R.id.btn_custom_reset -> reset()
        }
    }

    private fun save() {
        val name = mNameCustom!!.text.toString()
        if (name == null || name.length == 0) {
            Toast.makeText(applicationContext, R.string.toast_input_title, Toast.LENGTH_SHORT).show()
            return
        }

        val task = SaveTask()
        task.execute(name)

        finish()
    }

    private inner class SaveTask : AsyncTask<String, Int, Boolean>() {

        override fun doInBackground(vararg params: String): Boolean? {
            val name = params[0]
            var showYN = "N"
            if (mCustomId > 0) {
                showYN = mDb!!.getCustomKeySetShowYN(mCustomId)
                if (mDb!!.deleteCustomKeyset(mCustomId.toLong()) == false) {
                    mDb!!.deleteCustomKeyset(mCustomId.toLong())
                }
            }
            return mDb!!.insertCustomKeyset(name, showYN, mKeymap)
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!)
                Toast.makeText(applicationContext, R.string.toast_save_ok, Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(applicationContext, R.string.toast_save_fail, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onTouchDown(keyType: Int, index: Int, key: Key) {
        if (keyType == KeyView.KEY_TYPE_WHITE) {
            mPressedIndex = index
        } else {
            mPressedIndex = index + PianoKeyboard.WHITE_NUM
        }
    }

    override fun onTouchMove() {}

    override fun onTouchUp() {
        dialogKeyInput()
    }

    private fun dialogKeyInput() {
        val inflater = LayoutInflater.from(applicationContext)
        val layout = inflater.inflate(R.layout.register_custom_dialog, null)
        val edittext = layout.findViewById<View>(R.id.input_keyset) as EditText

        AlertDialog.Builder(this)
                .setTitle(R.string.title_input_key)
                .setView(layout)
                .setPositiveButton(R.string.btn_ok) { dialog, which ->
                    val data = edittext.text.toString()
                    if (data != null && data.length > 0) {
                        mKeymap!![mPressedIndex] = data

                        refreshKeymap()
                    }
                }
                .create().show()
    }

    private fun refreshKeymap() {
        mKeymapListView!!.removeAllViews()

        val keyset = TreeSet(mKeymap!!.keys)
        val iter = keyset.iterator()
        var hasKeymap = false
        var isShow1 = false
        var isShow2 = false
        var isShow3 = false
        var isShow4 = false

        while (iter.hasNext()) {
            val index = iter.next()

            if (index < 8 && isShow1 == false) {
                addCustomSubtitle(R.string.txt_piano_white_top)
                isShow1 = true
            } else if (index >= 8 && index < 16 && isShow2 == false) {
                mInflater!!.inflate(R.layout.register_custom_divider, mKeymapListView)
                addCustomSubtitle(R.string.txt_piano_white_bottom)
                isShow2 = true
            } else if (index >= 16 && index < 22 && isShow3 == false) {
                mInflater!!.inflate(R.layout.register_custom_divider, mKeymapListView)
                addCustomSubtitle(R.string.txt_piano_black_top)
                isShow3 = true
            } else if (index >= 22 && isShow4 == false) {
                mInflater!!.inflate(R.layout.register_custom_divider, mKeymapListView)
                addCustomSubtitle(R.string.txt_piano_black_bottom)
                isShow4 = true
            }

            val v = mInflater!!.inflate(R.layout.register_custom_item, null)
            val customData = v.findViewById<View>(R.id.custom_data) as TextView
            customData.text = mKeymap!![index]

            v.findViewById<View>(R.id.btn_delete).setOnClickListener {
                mKeymap!!.remove(index)
                refreshKeymap()
            }

            mKeymapListView!!.addView(v)
            hasKeymap = true
        }

        if (hasKeymap)
            mBtnReset!!.visibility = View.VISIBLE
        else
            mBtnReset!!.visibility = View.GONE

        mPianoKeyboard = PianoKeyboard(this, mKeymap!!)
        mKeyboardView!!.keyboard = mPianoKeyboard
    }

    private fun addCustomSubtitle(resId: Int) {
        val titleView = mInflater!!.inflate(R.layout.register_custom_title_item, null)
        val titleTextView = titleView.findViewById<View>(R.id.custom_title) as TextView
        titleTextView.setText(resId)
        mKeymapListView!!.addView(titleView)
    }

    private fun reset() {
        mNameCustom!!.text = null
        mKeymap!!.clear()
        refreshKeymap()
    }

    private fun dialogRecommendedCustom() {
        AlertDialog.Builder(this)
                .setTitle(R.string.btn_custom_recommend)
                .setItems(R.array.custom_names) { dialog, which ->
                    val res = resources
                    val names = res.getStringArray(R.array.custom_names)
                    mNameCustom!!.setText(names[which])

                    var strs: Array<String>? = null
                    when (which) {
                        0 -> strs = res.getStringArray(R.array.ex1_symbols)
                        1 -> strs = res.getStringArray(R.array.ex2_emoticons)
                        2 -> strs = res.getStringArray(R.array.ex3_pitch_names)
                        3 -> strs = res.getStringArray(R.array.ex4_hangul_2nd)
                    }
                    if (strs != null) {
                        for (i in strs.indices) {
                            mKeymap!![i] = strs[i]
                        }
                    }
                    refreshKeymap()
                }
                .create().show()
    }

}
