package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.components.InstallmentItem
import java.util.Calendar

class LoanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("loan_title") ?: "یادآوری قسط وام"
        val bank = intent.getStringExtra("loan_bank") ?: ""
        val amount = intent.getLongExtra("loan_amount", 0L)
        val formattedAmount = if (amount > 0) " به مبلغ ${PersianDateHelper.formatCurrency(amount)}" else ""
        val bankText = if (bank.isNotBlank()) " ($bank)" else ""

        val content = "سررسید قسط «$title»$bankText$formattedAmount فرارسیده است. جهت ثبت یا پیگیری وارد برنامه شوید."
        LoanNotificationHelper.showLoanNotification(context, "سررسید قسط: $title", content)
    }
}

object LoanNotificationHelper {

    const val CHANNEL_ID = "loan_installments_reminders"
    const val CHANNEL_NAME = "یادآوری اقساط و وام‌ها"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌ها و یادآوری سررسید پرداخت اقساط و وام‌های بانکی"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showLoanNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    fun showTestNotification(context: Context, loanTitle: String, amount: Long) {
        val amountText = if (amount > 0) PersianDateHelper.formatCurrency(amount) else "مبلغ تعیین‌شده"
        showLoanNotification(
            context,
            "تست اعلان: سررسید قسط $loanTitle",
            "اعلان یادآوری فعال است. قسط بعدی به مبلغ $amountText در موعد مقرر به شما یادآوری خواهد شد."
        )
    }

    fun scheduleLoanReminder(context: Context, item: InstallmentItem) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, LoanReminderReceiver::class.java).apply {
            putExtra("loan_title", item.title)
            putExtra("loan_bank", item.bankOrLender)
            putExtra("loan_amount", item.monthlyAmount)
            putExtra("loan_id", item.id)
        }

        val requestCode = item.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next trigger time: Due day at 09:00 AM minus offsetHours
        val offsetHours = if (item.reminderOffsetHours > 0 || item.reminderDaysBefore == 0) {
            item.reminderOffsetHours
        } else {
            item.reminderDaysBefore * 24
        }

        val calendar = Calendar.getInstance().apply {
            val targetDay = item.dueDayOfMonth.coerceIn(1, 31)

            set(Calendar.DAY_OF_MONTH, targetDay)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Subtract offset hours (e.g. 1 hour, 3 hours, 24 hours / 1 day)
            add(Calendar.HOUR_OF_DAY, -offsetHours)

            // If time already passed this month, schedule for next month
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    fun cancelLoanReminder(context: Context, itemId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, LoanReminderReceiver::class.java)
        val requestCode = itemId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
