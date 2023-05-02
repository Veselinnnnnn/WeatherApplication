package com.example.weather

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import java.util.*

class LanguageSelectionDialogFragment: DialogFragment() {

    private lateinit var languageList: ListView
    private lateinit var cancelButton: Button
    private lateinit var okButton: Button

    private val languageListItems = listOf(
        LanguageListItem("English", "en", false),
        LanguageListItem("Български", "bg", false),
        LanguageListItem("Français", "fr", false),
        LanguageListItem("Español", "es", false),
        LanguageListItem("Русский", "ru", false),
        LanguageListItem("Deutsch", "de", false),
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.language_selection_dialog, null)

        languageList = view.findViewById(R.id.language_list)
        cancelButton = view.findViewById(R.id.dialog_cancel_button)
        okButton = view.findViewById(R.id.dialog_ok_button)

        val languageAdapter = object : ArrayAdapter<LanguageListItem>(
            requireContext(),
            R.layout.list_item_layout,
            R.id.languageId,
            languageListItems
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val changeLanguageDialog = super.getView(position, convertView, parent)
                val textView = changeLanguageDialog.findViewById<TextView>(R.id.languageId)

                textView.apply {
                    text = getItem(position)?.languageName?.uppercase(Locale.getDefault())
                    gravity = Gravity.CENTER
                }

                if (getItem(position)?.selected == true) {
                    changeLanguageDialog.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.darker_white
                        )
                    )
                } else {
                    changeLanguageDialog.setBackgroundColor(Color.TRANSPARENT)
                }

                return changeLanguageDialog
            }
        }

        languageList.adapter = languageAdapter
        languageList.divider = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray))
        languageList.dividerHeight = 1.dpToPx(requireContext())
        languageList.setOnItemClickListener { _, _, position, _ ->
            languageListItems[position].selected = !languageListItems[position].selected
            languageAdapter.notifyDataSetChanged()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        okButton.setOnClickListener {
            for (item in languageListItems) {
                if (item.selected) {
                    saveLanguagePreference(item.languageCode)
                    setLocale(item.languageCode)

                    break
                }
            }

            requireActivity().recreate()
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun saveLanguagePreference(languageCode: String) {
        val preferences = requireActivity().getSharedPreferences("languagePreference", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("language_code", languageCode)
        editor.apply()
    }

    private fun Int.dpToPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}