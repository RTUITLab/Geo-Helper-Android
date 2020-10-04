package com.rtuitlab.geohelper.utils

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

fun Context.hasPermissions(vararg permissions: String) = permissions.all {
    ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

fun Context.showDialogOK(
    message: String,
    okListener: DialogInterface.OnClickListener
) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton("OK", okListener)
        .setNegativeButton("Cancel", okListener)
        .create()
        .show()
}