package com.github.danishjamal104.notes.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.danishjamal104.notes.backgroundtask.RestoreWorker
import com.github.danishjamal104.notes.databinding.ActivityMainBinding
import com.github.danishjamal104.notes.util.AppConstant
import com.github.danishjamal104.notes.util.copyToClipboard
import com.github.danishjamal104.notes.util.longToast
import com.github.danishjamal104.notes.util.performActionThroughSecuredChannel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding

    @Inject
    lateinit var workManager: WorkManager

    private lateinit var launcher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(AppConstant.IntentExtra.ENCRYPTION_KEY)?.let {
            this.copyToClipboard(it)
            this.longToast("Encryption Key copied to clipboard")
        }
        launcher = requestFile(this) {
            it?.let { it1 -> restoreBackup(it1) }
        }
    }

    private fun restoreBackup(uri: Uri) {
        val key = "test-key"

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

    fun openRestoreFile() {
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