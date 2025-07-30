package com.example.bprogress

import android.app.AlertDialog
import android.content.Context
// Ensure your Event.kt file is in this package (com.example.bprogress)
// If it is, you don't need an explicit import for com.example.bprogress.Event here.
import com.example.bprogress.Event
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer // Standard androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bprogress.databinding.FragmentActivitiesBinding
// R should be imported from your app's package
import com.example.bprogress.R // (already implicitly available if in same module)
import dagger.hilt.android.AndroidEntryPoint
import com.example.bprogress.ActivitiesViewModel
import com.example.bprogress.ActivityAdapter

@AndroidEntryPoint
class ActivitiesFragment : Fragment() {
    private val viewModel: ActivitiesViewModel by viewModels()

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ActivityAdapter
    private var mediaPlayer: MediaPlayer? = null

    // REMOVE the local Event class and EventObserver class definitions from here.
    // Ensure Event.kt is in the com.example.bprogress package.
    // If you need a reusable EventObserver, define it in its own file or a common utils file.
    // For this example, we'll use the direct lambda for observing.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupTutorial()

        Log.d("ActivitiesFragment", "Triggering initial data load from ViewModel")
        viewModel.initialDataLoad()
    }

    private fun setupRecyclerView() {
        adapter = ActivityAdapter(
            onDoubleClick = { activityItem ->
                Log.d("ActivitiesFragment", "Double-click on item ID: ${activityItem.id}")
                viewModel.toggleCheckedStatus(activityItem)
            },
            onLongClick = { activityItem ->
                Log.d("ActivitiesFragment", "Long-click on item ID: ${activityItem.id}")
                viewModel.toggleImportantStatus(activityItem)
            },
            onSingleClickWarn = { activityItem ->
                Log.d("ActivitiesFragment", "Single-click (warn) on item ID: ${activityItem.id}")
                showWarningToast(getString(R.string.single_click_warning))
            }
        )

        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewActivities.adapter = adapter
        Log.d("ActivitiesFragment", "RecyclerView and Adapter set up")
    }

    private fun setupObservers() {
        // Loading state observer
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> // Using direct lambda
            Log.d("ActivitiesFragment", "Loading state changed: $isLoading")
            binding.progressBarActivities.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recyclerViewActivities.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        // Activities list observer
        viewModel.allActivities.observe(viewLifecycleOwner) { activities -> // Using direct lambda
            Log.d("ActivitiesFragment", "Received activities list with size: ${activities?.size ?: 0}")
            adapter.submitList(activities)
        }

        // Milestone dialog event observer using a direct lambda
        viewModel.showStreakDialogEvent.observe(viewLifecycleOwner) { event -> // event is Event<Int>
            event.getContentIfNotHandled()?.let { totalChecksAtMilestone -> // totalChecksAtMilestone is Int
                Log.d("ActivitiesFragment", "Milestone dialog event received for milestone: $totalChecksAtMilestone")
                showFiftyStreakDialog(totalChecksAtMilestone)
            }
        }
    }

    private fun setupTutorial() {
        val sharedPreferences = requireActivity().getSharedPreferences("BProgressPrefs", Context.MODE_PRIVATE)
        val tutorialKey = "isFirstLaunchActivitiesFragment"
        val isFirstLaunch = sharedPreferences.getBoolean(tutorialKey, true)

        if (isFirstLaunch) {
            Log.d("ActivitiesFragment", "Showing tutorial overlay for first launch")
            binding.tutorialOverlay.visibility = View.VISIBLE
            binding.tutorialBalloonLayout.visibility = View.VISIBLE
        } else {
            binding.tutorialOverlay.visibility = View.GONE
            binding.tutorialBalloonLayout.visibility = View.GONE
        }

        binding.tutorialGotItButton.setOnClickListener {
            Log.d("ActivitiesFragment", "Tutorial 'Got It' clicked, hiding tutorial")
            binding.tutorialOverlay.visibility = View.GONE
            binding.tutorialBalloonLayout.visibility = View.GONE
            sharedPreferences.edit { putBoolean(tutorialKey, false) }
        }
    }

    private fun showWarningToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showFiftyStreakDialog(totalChecksAtMilestone: Int) {
        Log.d("ActivitiesFragment", "Showing fifty streak dialog for milestone: $totalChecksAtMilestone")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fifty_streak_dialog, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog)
            .setView(dialogView)
            .setCancelable(false) // Optional: Prevent dismissing by tapping outside or back press
            .create()

        val feelingEditText: EditText = dialogView.findViewById(R.id.feelingEditText)
        val sendButton: Button = dialogView.findViewById(R.id.sendFeelingButton)
        // val mainDialogBackground: ImageView? = dialogView.findViewById(R.id.dialogBackgroundImage) // Assuming you use this

        // If you have image loading logic for mainDialogBackground, it would go here.
        // For example, with Glide or Coil:
        // mainDialogBackground?.let {
        //     Glide.with(this).load(R.drawable.your_dialog_background_image).into(it)
        // }

        playCongratsSound()

        sendButton.setOnClickListener {
            val feeling = feelingEditText.text.toString().trim()
            if (feeling.isNotBlank()) {
                viewModel.saveUserFeelingForMilestone(feeling, totalChecksAtMilestone)
                Toast.makeText(requireContext(), getString(R.string.sync_successful), Toast.LENGTH_SHORT).show()

                // This SharedPreferences write is mostly a local failsafe.
                // The DB (UserProgress.lastStreakDialogShownAtCount) is the primary source of truth.
                val dialogKey = "fiftyStreakDialogShown_$totalChecksAtMilestone"
                requireActivity().getSharedPreferences("BProgressPrefs", Context.MODE_PRIVATE).edit {
                    putBoolean(dialogKey, true)
                    // apply() is fine here
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.enter_feeling_hint), Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun playCongratsSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.congrats)?.apply {
            setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer == mp) { // Ensure we are releasing the correct instance
                    mediaPlayer = null
                }
            }
            setOnErrorListener { _, _, _ ->
                Log.e("ActivitiesFragment", "Error playing congratulations sound.")
                mediaPlayer?.release() // Attempt to release on error too
                mediaPlayer = null
                true // Indicate error was handled
            }
            start()
        }
        Log.d("ActivitiesFragment", "Attempting to play congratulations sound.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null // Important to prevent memory leaks
        Log.d("ActivitiesFragment", "View destroyed, binding nulled, and media player released.")
    }
}
