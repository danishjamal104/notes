package com.github.danishjamal104.notes.util

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import javax.inject.Named

class SystemManager
constructor(private val ctx: Context, @Named(AppConstant.PERMISSION_ARRAY)val permissions: List<String>){

    fun checkPermission(): Boolean {
        return checkAllPermissions()
    }

    fun launchPermissionIntent(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(permissions.toTypedArray())
    }

    fun registerPermissionLauncher(activity: ComponentActivity,
                                           permissionResult: (success: Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            var all = true
            it.forEach { perm ->
                if (!perm.value) {
                    all = false
                }
            }
            if (all) {
                permissionResult(true)
            } else {
                permissionResult(false)
            }
        }
    }

    /** Permission Checking  */
    private fun checkAllPermissions(): Boolean {
        var hasPermissions = true
        for (permission in permissions) {
            hasPermissions = hasPermissions and isPermissionGranted(permission)
        }
        return hasPermissions
    }

    private fun isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

}