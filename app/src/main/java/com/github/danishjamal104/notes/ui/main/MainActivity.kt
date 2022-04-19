package com.github.danishjamal104.notes.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.danishjamal104.notes.R
import com.github.danishjamal104.notes.backgroundtask.BackupWorker
import com.github.danishjamal104.notes.backgroundtask.RestoreWorker
import com.github.danishjamal104.notes.databinding.ActivityMainBinding
import com.github.danishjamal104.notes.util.*
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding

    private lateinit var navController: NavController

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var systemManager: SystemManager

    private lateinit var launcher: ActivityResultLauncher<Intent>

    private var taskAfterPermission: ((permissionGranted: Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(AppConstant.IntentExtra.ENCRYPTION_KEY)?.let {
            this.copyToClipboard(it)
            this.longToast("Encryption Key copied to clipboard")
        }

        val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)
        if (hostFragment is NavHostFragment)
            navController = hostFragment.navController

        // setup restore file picker
        launcher = requestFile(this) {
            it?.let { uri ->
                takeEncryptionKeyInput { key ->
                    if (key.isEmpty()) {
                        shortToast("Empty encryption key")
                        return@takeEncryptionKeyInput
                    }
                    this.showDefaultMaterialAlert(
                        "Confirmation",
                        "You are about to restore the notes from backup. This will remove all the existing notes."
                    ) {
                        restoreBackup(uri, key)
                        shortToast("Restore scheduled")
                    }
                }
            }
        }

        // disable motion layout in auth fragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.authenticationFragment -> {
                    hideMotionLayout()
                }
                R.id.homeFragment -> {
                    showMotionLayout()
                }
                else -> {
                    hideMotionLayout()
                }
            }
        }

        binding.root.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                when (currentId) {
                    R.id.start -> {
                        // layout reached start position
                        binding.backupButton.disable()
                        binding.restoreButton.disable()
                    }
                    R.id.end -> {
                        binding.backupButton.enable()
                        binding.restoreButton.enable()
                    }
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })
        binding.backupButton.disable()
        binding.restoreButton.disable()

        val permissionLauncher =
            systemManager.registerPermissionLauncher(this) { permissionGranted ->
                taskAfterPermission?.invoke(permissionGranted)
            }

        // setting up click listener
        binding.backupButton.setOnClickListener {
            taskAfterPermission = { permissionGranted ->
                if (!permissionGranted) {
                    shortToast("Need storage access to perform backup")
                } else {
                    performActionThroughSecuredChannel {
                        val key = EncryptionHelper.generateEncryptionKey()
                        Log.i("SECUREDINFO", key)
                        val data = Data.Builder().putString(AppConstant.Worker.KEY, key).build()
                        val request = OneTimeWorkRequestBuilder<BackupWorker>()
                            .addTag("HOME FRAGMENT")
                            .setInputData(data).build()
                        workManager.enqueue(request)
                        shortToast("Backup scheduled")
                    }
                }
            }
            if (systemManager.checkPermission()) {
                taskAfterPermission!!.invoke(true)
            } else {
                systemManager.launchPermissionIntent(permissionLauncher)
            }
        }

        binding.restoreButton.setOnClickListener {
            openRestoreFile()
        }
    }

    fun resetMotionLayout() {
        binding.root.transitionToStart()
    }

    private fun showMotionLayout() {
        binding.root.getTransition(R.id.start_to_end).isEnabled = true
        binding.backupRestoreImage.visibility = View.VISIBLE
    }

    private fun hideMotionLayout() {
        binding.root.getTransition(R.id.start_to_end).isEnabled = false
        binding.backupRestoreImage.visibility = View.GONE
    }

    private fun takeEncryptionKeyInput(result: (key: String) -> Unit) {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Enter encryption key")

        val input = TextInputEditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.isSingleLine = true
        input.ellipsize = TextUtils.TruncateAt.END

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val side = resources.getDimensionPixelSize(R.dimen.dialog_edit_text_margin_horizontal)
            val topBottom =
                resources.getDimensionPixelSize(R.dimen.dialog_edit_text_margin_vertical)
            setMargins(side, topBottom, side, topBottom)
        }
        input.layoutParams = params

        container.addView(input)

        builder.setView(container)
        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            val key = input.text.toString()
            result.invoke(key)
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            shortToast("Canceled by user")
        }

        builder.show()
    }

    private fun restoreBackup(uri: Uri, key: String) {

        performActionThroughSecuredChannel {
            val data = Data.Builder()
                .putString(AppConstant.Worker.KEY, key)
                .putString(AppConstant.Worker.FILE_URI, uri.toString()).build()
            val request = OneTimeWorkRequestBuilder<RestoreWorker>()
                .addTag("HOME ACTIVITY")
                .setInputData(data).build()
            workManager.enqueue(request)
        }
    }

    private fun openRestoreFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        launcher.launch(intent)
    }

    private fun requestFile(
        activity: ComponentActivity,
        result: (file: Uri?) -> Unit
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val st = it.data!!.data
                if (st == null) {
                    result(null)
                    return@registerForActivityResult
                }
                result(st)
            }
        }
    }
}