package com.utshob.mybikes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.ui.ExpensesFragment;
import com.utshob.mybikes.ui.GarageFragment;
import com.utshob.mybikes.ui.HistoryFragment;
import com.utshob.mybikes.ui.SettingsFragment;
import com.utshob.mybikes.util.Ui;
import com.utshob.mybikes.work.ReminderScheduler;

/** Hosts the 4 bottom-navigation tabs. Also uses the ACCELEROMETER: shake the phone to add a fuel record. */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShake = 0;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        DataStore.get().start();

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_expenses) f = new ExpensesFragment();
            else if (id == R.id.nav_history) f = new HistoryFragment();
            else if (id == R.id.nav_settings) f = new SettingsFragment();
            else f = new GarageFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_garage);

        // Android 13+ needs the notification permission at runtime.
        if (Build.VERSION.SDK_INT >= 33 && com.utshob.mybikes.util.Prefs.remindersEnabled()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        ReminderScheduler.schedule(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float gx = e.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = e.values[1] / SensorManager.GRAVITY_EARTH;
        float gz = e.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gx * gx + gy * gy + gz * gz);   // about 1.0 when the phone is still
        if (gForce > 2.7) {
            long now = System.currentTimeMillis();
            if (now - lastShake < 1500) return;
            lastShake = now;
            Bike b = DataStore.get().selectedBike();
            if (b == null) {
                Ui.toast(this, "Add a bike first, then shake to add fuel");
            } else {
                startActivity(new Intent(this, AddEditFuelActivity.class).putExtra("bike_id", b.getId()));
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
