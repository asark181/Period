package com.nuanyue.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nuanyue.app.MainActivity
import com.nuanyue.app.NuanYueApp
import com.nuanyue.app.R
import com.nuanyue.app.data.AppSettings
import com.nuanyue.app.domain.CycleEngine
import com.nuanyue.app.domain.Copy
import com.nuanyue.app.domain.Period
import com.nuanyue.app.domain.Phase
import com.nuanyue.app.widget.PeriodWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL_ID = "cycle_reminders"
    private const val REQUEST_DAILY = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "经期提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "经期将至、排卵日、记录提醒"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Schedules the next daily check at the configured time. Safe to call repeatedly. */
    fun schedule(context: Context, s: AppSettings) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        am.cancel(pi)
        if (!s.remindEnabled) return
        var at = LocalDate.now().atTime(s.remindHour, s.remindMinute)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_DAILY,
        Intent(context, ReminderReceiver::class.java).setAction("com.nuanyue.app.DAILY_CHECK"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Decides which messages are due today. Pure, so it is easy to reason about. */
    fun due(periods: List<Period>, s: AppSettings, today: LocalDate = LocalDate.now()): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val snap = CycleEngine.snapshot(periods, s.defaultCycle, s.defaultPeriod, today)
        val partner = s.isPartner
        if (snap.hasData) {
            val t = snap.today
            val inPeriod = t.phase == Phase.MENSTRUAL
            val until = t.daysUntilNext
            if (s.remindBefore && !inPeriod && t.lateDays == 0 && until != null) {
                if (until == s.remindBeforeDays && until > 0) out += Copy.beforePeriod(until, today.plusDays(until.toLong()), partner)
                else if (until == 0) out += Copy.periodToday(partner)
            }
            if (s.remindLate && t.lateDays in setOf(1, 3, 7, 14)) out += Copy.late(t.lateDays, partner)
            if (s.remindEnd && !partner) {
                val last = snap.periods.last()
                val day = CycleEngine.days(last.start, today) + 1
                if (last.end == null && day == snap.avgPeriod + 1) out += Copy.endCheck(day)
            }
            if (s.remindFertile && !inPeriod) {
                val ov = t.nextOvulation
                if (ov != null) {
                    if (ov == today) out += Copy.ovulation(partner)
                    else if (ov.minusDays(5) == today) out += Copy.fertileStart(partner)
                }
            }
        }
        return out
    }

    fun notify(context: Context, id: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFF2708A.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as NuanYueApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = app.settings.current()
                if (s.isPartner && s.syncEnabled) app.sync.syncNow()
                val periods = app.repository.periodsNow()
                Reminders.due(periods, s).forEachIndexed { i, (title, body) ->
                    Reminders.notify(context, 2000 + i, title, body)
                }
                PeriodWidget.updateAll(context)
                Reminders.schedule(context, s)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are wiped on reboot, app update and time changes; put the daily check back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as NuanYueApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.schedule(context, app.settings.current())
                PeriodWidget.updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
