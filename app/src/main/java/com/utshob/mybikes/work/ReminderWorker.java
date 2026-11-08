package com.utshob.mybikes.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.utshob.mybikes.data.ReminderLogic;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.Maintenance;
import com.utshob.mybikes.util.Prefs;

import java.util.concurrent.TimeUnit;

/** Background job: reads bikes + maintenance from Firebase and posts a notification for every due reminder (once). */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        Prefs.init(ctx);
        if (!Prefs.remindersEnabled()) return Result.success();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();
        try {
            DatabaseReference root = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            DataSnapshot bikesSnap = Tasks.await(root.child("bikes").get(), 25, TimeUnit.SECONDS);
            DataSnapshot mSnap = Tasks.await(root.child("maintenance").get(), 25, TimeUnit.SECONDS);
            for (DataSnapshot bs : bikesSnap.getChildren()) {
                Bike b = bs.getValue(Bike.class);
                if (b == null) continue;
                b.setId(bs.getKey());
                Maintenance m = mSnap.child(bs.getKey()).getValue(Maintenance.class);
                for (ReminderLogic.Reminder r : ReminderLogic.evaluate(b, m)) {
                    if (r.due && !Prefs.isNotified(r.key)) {
                        if (NotificationHelper.show(ctx, r.title, r.text, r.key.hashCode())) {
                            Prefs.markNotified(r.key);   // never notify the same reminder twice
                        }
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
