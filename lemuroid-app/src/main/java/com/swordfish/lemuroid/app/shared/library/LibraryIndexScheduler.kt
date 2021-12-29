package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object LibraryIndexScheduler {
    val UNIQUE_WORK_ID: String = LibraryIndexScheduler::class.java.simpleName

    fun scheduleFullSync(applicationContext: Context?) {
        if (applicationContext != null) {
            WorkManager.getInstance(applicationContext)
                .beginUniqueWork(
                    UNIQUE_WORK_ID,
                    ExistingWorkPolicy.APPEND,
                    OneTimeWorkRequestBuilder<LibraryIndexWork>().build()
                )
                .then(OneTimeWorkRequestBuilder<CoreUpdateWork>().build())
                .enqueue()
            Toast.makeText(applicationContext, "scheduleFullSync()", Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleCoreUpdate(applicationContext: Context) {
        WorkManager.getInstance(applicationContext)
            .beginUniqueWork(
                UNIQUE_WORK_ID,
                ExistingWorkPolicy.APPEND,
                OneTimeWorkRequestBuilder<CoreUpdateWork>().build()
            )
            .enqueue()
    }
}
