package com.mohanbuilds.focus.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.data.Recurrence
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class TaskCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!NotificationPreferences.isEnabled(applicationContext)) return Result.success()

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val allTasks = CalendarRepository.loadTasksSync(applicationContext)

        val oneOffTasksToday = allTasks.filter { task ->
            runCatching { LocalDate.parse(task.date) }.getOrNull() == today
                && task.recurrence == Recurrence.NONE
                && task.seriesId == null
        }

        val hasOneOffTasks = oneOffTasksToday.isNotEmpty()
        val allDone = hasOneOffTasks && oneOffTasksToday.all { it.completed }

        val hour = now.hour

        when {
            // 8 AM — only if no one-off tasks at all
            hour == 8 && !hasOneOffTasks -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_8AM,
                    "Focus",
                    "Start fresh—what's the plan today?",
                    "Add Tasks",
                )
            }
            // 12 PM — no one-off tasks, or all done
            hour == 12 && !hasOneOffTasks -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_12PM,
                    "Focus",
                    "Make today count! Add your tasks.",
                    "Add Tasks",
                )
            }
            hour == 12 && allDone -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_12PM,
                    "Focus",
                    "Got another goal? Add it now.",
                    "Add Tasks",
                )
            }
            // 4 PM — tasks pending, or all done
            hour == 16 && hasOneOffTasks && !allDone -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_4PM,
                    "Focus",
                    "Make today a win—mark your progress!",
                    "Clear Tasks",
                )
            }
            hour == 16 && allDone -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_4PM,
                    "Focus",
                    "Anything else to add?",
                )
            }
            // 8 PM — tasks still pending
            hour == 20 && hasOneOffTasks && !allDone -> {
                NotificationHelper.postNotification(
                    applicationContext,
                    NOTIF_ID_8PM,
                    "Focus",
                    "Finish strong! Check off those wins.",
                )
            }
        }

        // Re-schedule for the next day
        scheduleNextDay(context = applicationContext)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME_PREFIX = "task_check_"
        const val NOTIF_ID_8AM = 1001
        const val NOTIF_ID_12PM = 1002
        const val NOTIF_ID_4PM = 1003
        const val NOTIF_ID_8PM = 1004

        private val TIME_SLOTS = listOf(8, 12, 16, 20)

        fun scheduleAll(context: Context) {
            cancelAll(context)
            val now = LocalDateTime.now()

            for (hour in TIME_SLOTS) {
                val delay = initialDelayToHour(now, hour)
                val request = PeriodicWorkRequestBuilder<TaskCheckWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay)
                    .addTag(WORK_NAME_PREFIX + hour)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PREFIX + hour,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }

        fun cancelAll(context: Context) {
            for (hour in TIME_SLOTS) {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(WORK_NAME_PREFIX + hour)
            }
        }

        private fun scheduleNextDay(context: Context) {
            val now = LocalDateTime.now()
            for (hour in TIME_SLOTS) {
                val delay = initialDelayToHour(now, hour)
                val request = PeriodicWorkRequestBuilder<TaskCheckWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay)
                    .addTag(WORK_NAME_PREFIX + hour)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PREFIX + hour,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }

        private fun initialDelayToHour(now: LocalDateTime, targetHour: Int): Duration {
            val target = LocalDateTime.of(now.toLocalDate(), LocalTime.of(targetHour, 0))
            val nextTarget = if (now.isBefore(target)) target else target.plusDays(1)
            return Duration.between(now, nextTarget)
        }
    }
}
