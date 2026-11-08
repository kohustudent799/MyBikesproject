package com.utshob.mybikes.work;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    private static final String NAME = "mybikes_reminders";

    private ReminderScheduler() {}

    /** Checks all reminders every 12 hours in the background. */
    public static void schedule(Context c) {
        PeriodicWorkRequest w = new PeriodicWorkRequest.Builder(ReminderWorker.class, 12, TimeUnit.HOURS).build();
        WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, w);
    }

    /** Runs one check right now (used after saving fuel / odometer / reminders). */
    public static void checkNow(Context c) {
        WorkManager.getInstance(c).enqueue(new OneTimeWorkRequest.Builder(ReminderWorker.class).build());
    }

    public static void cancel(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(NAME);
    }
}
