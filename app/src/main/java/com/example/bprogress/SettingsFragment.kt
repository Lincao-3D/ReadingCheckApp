// --- app/src/main/java/com/example/bprogress/SettingsFragment.kt ---
package com.example.bprogress

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Hilt's ViewModel delegate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Important: Annotate Fragment for Hilt injection
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSwitch(view)
        setupLanguageSpinner(view)
        setupDonationButtons(view)
        setupFiftyStreakObserver(view)
    }

    private fun setupThemeSwitch(view: View) {
        val themeSwitch: androidx.appcompat.widget.SwitchCompat = view.findViewById(R.id.themeSwitch)
        val sharedPreferences = requireActivity().getSharedPreferences("BProgressPrefs", Context.MODE_PRIVATE)

        // Define o estado inicial do switch baseado no modo atual
        val isDarkMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        themeSwitch.isChecked = isDarkMode

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit { putBoolean("isDarkMode", isChecked) }
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            // Recriar a atividade para aplicar o tema imediatamente é uma opção,
            // mas o ideal é que o tema se aplique na próxima vez que a app for aberta ou
            // que a activity seja recriada por outra razão, para uma transição mais suave.
            // Se a aplicação imediata for um requisito, a linha abaixo pode ser descomentada.
            requireActivity().recreate()
        }
    }

    private fun setupLanguageSpinner(view: View) {
        val languageSpinner: Spinner = view.findViewById(R.id.languageSpinner)
        val sharedPreferences = requireActivity().getSharedPreferences("BProgressPrefs", Context.MODE_PRIVATE)
        val languages = resources.getStringArray(R.array.language_entries)
        val localeCodes = resources.getStringArray(R.array.language_values)

        languageSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        val currentLocale = sharedPreferences.getString("appLocale", "en") ?: "en"
        languageSpinner.setSelection(localeCodes.indexOf(currentLocale).coerceAtLeast(0))

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLocale = localeCodes[position]
                if (selectedLocale != sharedPreferences.getString("appLocale", "en")) {
                    // CORRECTED: Call the static method from App's companion object
                    App.updateAppLocale(requireContext(), selectedLocale) // Use updateAppLocale and pass context
                    requireActivity().recreate() // Recreate AFTER locale is fully updated
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupDonationButtons(view: View) {
        val copyPixKeyButton: Button = view.findViewById(R.id.copyPixKeyButton)
        val copyPaypalEmailButton: Button = view.findViewById(R.id.copyPaypalEmailButton)

        copyPixKeyButton.setOnClickListener {
            copyToClipboard(getString(R.string.donation_pix_info))
        }
        copyPaypalEmailButton.setOnClickListener {
            copyToClipboard(getString(R.string.donation_paypal_info))
        }
    }

    private fun setupFiftyStreakObserver(view: View) {
        val viewFiftyStreakFeelingButton: Button = view.findViewById(R.id.viewFiftyStreakFeelingButton)
        viewModel.userProgress.observe(viewLifecycleOwner) { userProgress ->
            if (userProgress?.fiftyStreakFeeling != null) {
                viewFiftyStreakFeelingButton.visibility = View.VISIBLE
                viewFiftyStreakFeelingButton.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.fifty_streak_feeling_title)
                        .setMessage(userProgress.fiftyStreakFeeling)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            } else {
                viewFiftyStreakFeelingButton.visibility = View.GONE
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BProgress Donation", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard_toast), Toast.LENGTH_SHORT).show()
    }
}
