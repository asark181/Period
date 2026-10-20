package com.nuanyue.app.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nuanyue.app.NuanYueApp

class UpdateCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NuanYueApp
        val result = app.updates.check(notify = true, present = false)
        return if (result == CheckResult.Failed) Result.retry() else Result.success()
    }
}
