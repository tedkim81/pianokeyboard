package com.teuskim.pianokeyboard

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

class SettingActivity : BaseActivity(), View.OnClickListener {

    private var mListViewKeyboard: LinearLayout? = null
    private var mListCustom: LinearLayout? = null
    private var mRadiobtnSound: RadioGroup? = null
    private var mRadioBtnSoundRecommended: RadioButton? = null
    private var mRadioBtnSoundOriginal: RadioButton? = null
    private var mRadioBtnSoundNone: RadioButton? = null
    private var mCheckboxNoSound: CheckBox? = null

    private var mInflater: LayoutInflater? = null
    private var mDb: PianoKeyboardDb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting)
        volumeControlStream = AudioManager.STREAM_MUSIC
        findViews()

        mDb = PianoKeyboardDb.getInstance(applicationContext)
        mInflater = LayoutInflater.from(applicationContext)

        refreshKeySetList()
        refreshCustomList()

        val soundMode = mDb!!.soundMode
        when (soundMode) {
            SoundMode.RECOMMENDED -> mRadioBtnSoundRecommended!!.isChecked = true
            SoundMode.ORIGINAL -> mRadioBtnSoundOriginal!!.isChecked = true
            SoundMode.NONE -> mRadioBtnSoundNone!!.isChecked = true
        }
        mCheckboxNoSound!!.isChecked = mDb!!.isSoundOffIfSilent

        // google analytics
        mTracker!!.trackPageView("SettingActivity")
    }

    private fun findViews() {
        mListViewKeyboard = findViewById<View>(R.id.list_keyboard) as LinearLayout
        mListCustom = findViewById<View>(R.id.list_custom) as LinearLayout
        mRadiobtnSound = findViewById<View>(R.id.radiobtn_sound) as RadioGroup
        mRadioBtnSoundRecommended = findViewById<View>(R.id.radiobtn_sound_recommended) as RadioButton
        mRadioBtnSoundOriginal = findViewById<View>(R.id.radiobtn_sound_original) as RadioButton
        mRadioBtnSoundNone = findViewById<View>(R.id.radiobtn_sound_none) as RadioButton
        mCheckboxNoSound = findViewById<View>(R.id.checkbox_nosound) as CheckBox

        findViewById<View>(R.id.btn_close).setOnClickListener(this)
        findViewById<View>(R.id.btn_add_custom).setOnClickListener(this)
        mCheckboxNoSound!!.setOnClickListener(this)

        val listener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radiobtn_sound_recommended -> mDb!!.updateSoundMode(SoundMode.RECOMMENDED)
                R.id.radiobtn_sound_original -> mDb!!.updateSoundMode(SoundMode.ORIGINAL)
                R.id.radiobtn_sound_none -> mDb!!.updateSoundMode(SoundMode.NONE)
            }
        }
        mRadiobtnSound!!.setOnCheckedChangeListener(listener)
    }

    override fun onClick(v: View) {
        val i: Intent
        when (v.id) {
            R.id.btn_close -> finish()
            R.id.btn_add_custom -> {
                i = Intent(applicationContext, RegisterCustomActivity::class.java)
                startActivityForResult(i, 0)
            }
            R.id.checkbox_nosound -> changeCheckboxNosound()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // custom keyset 추가한 후 리프레쉬
        refreshCustomList()
    }

    private fun refreshKeySetList() {
        mListViewKeyboard!!.removeAllViews()
        val list = mDb!!.keySetList

        for (item in list) {
            val v = mInflater!!.inflate(R.layout.setting_keyset_item, null)
            val checkbox = v.findViewById<View>(R.id.checkbox_keyset) as CheckBox
            checkbox.text = mDb!!.getKeyboardName(item.mType)
            if ("Y" == item.mShowYN)
                checkbox.isChecked = true
            else
                checkbox.isChecked = false

            checkbox.setOnClickListener { v ->
                mDb!!.updateKeySetChecked(item.mId, (v as CheckBox).isChecked)
                mDb!!.updateKeyboardPosition(0)
            }

            mListViewKeyboard!!.addView(v)
        }
    }

    private fun refreshCustomList() {
        mListCustom!!.removeAllViews()
        val list = mDb!!.customKeySetList

        for (item in list) {
            val v = mInflater!!.inflate(R.layout.setting_custom_item, null)
            val name = v.findViewById<View>(R.id.custom_name) as TextView
            name.text = item.mName
            val btnEdit = v.findViewById<View>(R.id.btn_edit_custom) as Button
            val btnDelete = v.findViewById<View>(R.id.btn_delete_custom) as Button
            val listener = View.OnClickListener { v ->
                when (v.id) {
                    R.id.btn_edit_custom -> {
                        val i = Intent(applicationContext, RegisterCustomActivity::class.java)
                        i.putExtra("customId", item.mId)
                        startActivityForResult(i, 0)
                    }
                    R.id.btn_delete_custom -> dialogDeleteCustom(item.mId.toLong())
                }
            }
            btnEdit.setOnClickListener(listener)
            btnDelete.setOnClickListener(listener)

            val checkbox = v.findViewById<View>(R.id.checkbox_custom) as CheckBox
            val showYN = mDb!!.getCustomKeySetShowYN(item.mId)

            checkbox.setText(R.string.txt_use_custom)

            if ("Y" == showYN) {
                checkbox.isChecked = true
            } else {
                checkbox.isChecked = false
            }
            checkbox.setOnClickListener {
                if (checkbox.isChecked) {
                    mDb!!.updateCustomKeySetShowYN(item.mId, "Y")
                } else {
                    mDb!!.updateCustomKeySetShowYN(item.mId, "N")
                }
            }

            mListCustom!!.addView(v)
        }
    }

    private fun dialogDeleteCustom(itemId: Long) {
        AlertDialog.Builder(this)
                .setMessage(R.string.alert_delete)
                .setPositiveButton(R.string.btn_ok) { dialog, which ->
                    mDb!!.deleteCustomKeyset(itemId)
                    refreshCustomList()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
    }

    private fun changeCheckboxNosound() {
        mDb!!.updateIsSoundOffIfSilent(mCheckboxNoSound!!.isChecked)
    }

}
