package com.utshob.mybikes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.ReminderLogic;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.Maintenance;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;
import com.utshob.mybikes.work.ReminderScheduler;

import java.util.HashMap;
import java.util.Map;

/** Engine oil + air filter reminders for one bike (extra "bike_id"). */
public class MaintenanceActivity extends AppCompatActivity implements DataStore.Listener {
    private String bikeId;
    private TextView tvBikeName, tvOilStatus, tvAirStatus;
    private MaterialButton btnOilDate, btnAirInstallDate, btnAirNextDate;
    private TextInputEditText etOilKm, etOilInterval, etAirInstallKm, etAirNextKm;
    private long oilDate = System.currentTimeMillis();
    private long airInstallDate = System.currentTimeMillis();
    private long airNextDate = 0;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);
        DataStore.get().start();
        bikeId = getIntent().getStringExtra("bike_id");
        if (bikeId == null) { finish(); return; }

        tvBikeName = findViewById(R.id.tvBikeName);
        tvOilStatus = findViewById(R.id.tvOilStatus);
        tvAirStatus = findViewById(R.id.tvAirStatus);
        btnOilDate = findViewById(R.id.btnOilDate);
        btnAirInstallDate = findViewById(R.id.btnAirInstallDate);
        btnAirNextDate = findViewById(R.id.btnAirNextDate);
        etOilKm = findViewById(R.id.etOilKm);
        etOilInterval = findViewById(R.id.etOilInterval);
        etAirInstallKm = findViewById(R.id.etAirInstallKm);
        etAirNextKm = findViewById(R.id.etAirNextKm);

        btnOilDate.setText(Fmt.date(oilDate));
        btnAirInstallDate.setText(Fmt.date(airInstallDate));
        showNextDate();

        btnOilDate.setOnClickListener(v -> Ui.pickDate(this, oilDate, d -> { oilDate = d; btnOilDate.setText(Fmt.date(d)); }));
        btnAirInstallDate.setOnClickListener(v -> Ui.pickDate(this, airInstallDate, d -> { airInstallDate = d; btnAirInstallDate.setText(Fmt.date(d)); }));
        btnAirNextDate.setOnClickListener(v -> Ui.pickDate(this, airNextDate > 0 ? airNextDate : System.currentTimeMillis(), d -> { airNextDate = d; showNextDate(); }));
        findViewById(R.id.btnAirClearDate).setOnClickListener(v -> { airNextDate = 0; showNextDate(); });
        findViewById(R.id.btnOilSave).setOnClickListener(v -> saveOil());
        findViewById(R.id.btnOilDone).setOnClickListener(v -> oilDoneDialog());
        findViewById(R.id.btnAirSave).setOnClickListener(v -> saveAir());
        findViewById(R.id.btnAirDone).setOnClickListener(v -> airDoneDialog());

        DataStore.get().runWhenLoaded(this::fillForm);
    }

    @Override protected void onStart() {
        super.onStart();
        DataStore.get().addListener(this);
        updateStatus();
    }

    @Override protected void onStop() {
        super.onStop();
        DataStore.get().removeListener(this);
    }

    @Override public void onDataChanged() { updateStatus(); }

    private void showNextDate() {
        btnAirNextDate.setText(airNextDate > 0 ? Fmt.date(airNextDate) : "Not set (tap to choose)");
    }

    private void fillForm() {
        Maintenance m = DataStore.get().maintenance.get(bikeId);
        if (m == null) return;
        if (m.getOilIntervalKm() > 0) {
            oilDate = m.getOilLastDate() > 0 ? m.getOilLastDate() : oilDate;
            btnOilDate.setText(Fmt.date(oilDate));
            etOilKm.setText(Fmt.plain(m.getOilLastKm()));
            etOilInterval.setText(Fmt.plain(m.getOilIntervalKm()));
        }
        if (m.getAirInstallDate() > 0) { airInstallDate = m.getAirInstallDate(); btnAirInstallDate.setText(Fmt.date(airInstallDate)); }
        if (m.getAirInstallKm() >= 0) etAirInstallKm.setText(Fmt.plain(m.getAirInstallKm()));
        airNextDate = m.getAirNextDate();
        showNextDate();
        if (m.getAirNextKm() > 0) etAirNextKm.setText(Fmt.plain(m.getAirNextKm()));
    }

    private void updateStatus() {
        Bike b = DataStore.get().bike(bikeId);
        Maintenance m = DataStore.get().maintenance.get(bikeId);
        if (b == null) { if (DataStore.get().isLoaded()) finish(); return; }
        tvBikeName.setText(b.getName() + "  ·  odometer " + Fmt.km(b.getOdometer()));

        // Engine oil
        if (m == null || m.getOilIntervalKm() <= 0) {
            setStatus(tvOilStatus, "No engine oil reminder set yet.", false, false);
        } else {
            double next = ReminderLogic.oilNextKm(m);
            boolean due = b.getOdometer() >= next;
            String text = "Next oil change: " + Fmt.km(next) + "  (" + Fmt.km(m.getOilLastKm()) + " + " + Fmt.km(m.getOilIntervalKm()) + ")\n";
            if (due) text += "Status: DUE NOW";
            else text += "Status: " + (m.isOilDone() ? "Completed" : "Scheduled") + ", " + Fmt.km(next - b.getOdometer()) + " left";
            setStatus(tvOilStatus, text, due, !due);
        }
        // Air filter
        if (m == null || (m.getAirNextDate() <= 0 && m.getAirNextKm() <= 0)) {
            setStatus(tvAirStatus, "No air filter reminder set yet.", false, false);
        } else {
            boolean dueDate = m.getAirNextDate() > 0 && System.currentTimeMillis() >= m.getAirNextDate();
            boolean dueKm = m.getAirNextKm() > 0 && b.getOdometer() >= m.getAirNextKm();
            StringBuilder sb = new StringBuilder();
            if (m.getAirNextDate() > 0) sb.append("Next date: ").append(Fmt.date(m.getAirNextDate())).append('\n');
            if (m.getAirNextKm() > 0) sb.append("Next odometer: ").append(Fmt.km(m.getAirNextKm())).append('\n');
            boolean due = dueDate || dueKm;
            sb.append("Status: ").append(due ? "DUE NOW" : (m.isAirDone() ? "Completed, next one scheduled" : "Scheduled"));
            setStatus(tvAirStatus, sb.toString(), due, !due);
        }
    }

    private void setStatus(TextView tv, String text, boolean red, boolean green) {
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(this, red ? R.color.red : green ? R.color.green : R.color.text_secondary));
    }

    private void write(Map<String, Object> values) {
        DataStore.userRoot().child("maintenance").child(bikeId).updateChildren(values)
                .addOnFailureListener(e -> Ui.toast(getApplicationContext(), "Save failed: " + e.getMessage()));
        askNotificationPermissionIfNeeded();
        ReminderScheduler.checkNow(this);
    }

    private void askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && Prefs.remindersEnabled()
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ---------------- engine oil ----------------
    private void saveOil() {
        double km = Fmt.parse(etOilKm.getText());
        double interval = Fmt.parse(etOilInterval.getText());
        if (Double.isNaN(km) || km < 0) { Ui.err(etOilKm, "Enter the odometer at the last oil change"); return; }
        if (Double.isNaN(interval) || interval <= 0) { Ui.err(etOilInterval, "Enter a service interval greater than 0"); return; }
        Map<String, Object> m = new HashMap<>();
        m.put("oilLastDate", oilDate);
        m.put("oilLastKm", km);
        m.put("oilIntervalKm", interval);
        m.put("oilDone", false);
        write(m);
        Ui.toast(this, "Oil reminder saved. Next change at " + Fmt.km(km + interval));
    }

    private void oilDoneDialog() {
        Bike b = DataStore.get().bike(bikeId);
        Maintenance old = DataStore.get().maintenance.get(bikeId);
        TextInputLayout tilOdo = Ui.field(this, "Odometer at this oil change (km)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputLayout tilInt = Ui.field(this, "Next service interval (km)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputEditText etOdo = Ui.edit(tilOdo), etInt = Ui.edit(tilInt);
        if (b != null) etOdo.setText(Fmt.plain(b.getOdometer()));
        etInt.setText(old != null && old.getOilIntervalKm() > 0 ? Fmt.plain(old.getOilIntervalKm()) : "2000");
        AlertDialog d = buildDialog("Mark oil change as completed", tilOdo, tilInt);
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double odo = Fmt.parse(etOdo.getText()), interval = Fmt.parse(etInt.getText());
            if (Double.isNaN(odo) || odo < 0) { Ui.err(etOdo, "Enter a valid odometer"); return; }
            if (Double.isNaN(interval) || interval <= 0) { Ui.err(etInt, "Interval must be greater than 0"); return; }
            Map<String, Object> m = new HashMap<>();
            m.put("oilLastDate", System.currentTimeMillis());
            m.put("oilLastKm", odo);
            m.put("oilIntervalKm", interval);
            m.put("oilDone", true);
            write(m);
            if (b != null && odo > b.getOdometer()) DataStore.userRoot().child("bikes").child(bikeId).child("odometer").setValue(odo);
            oilDate = System.currentTimeMillis();
            btnOilDate.setText(Fmt.date(oilDate));
            etOilKm.setText(Fmt.plain(odo));
            etOilInterval.setText(Fmt.plain(interval));
            d.dismiss();
            Ui.toast(this, "Completed. Next oil change at " + Fmt.km(odo + interval));
        }));
        d.show();
    }

    // ---------------- air filter ----------------
    private void saveAir() {
        double installKm = Fmt.parse(etAirInstallKm.getText());
        double nextKm = Fmt.parse(etAirNextKm.getText());
        if (!Double.isNaN(installKm) && installKm < 0) { Ui.err(etAirInstallKm, "Odometer cannot be negative"); return; }
        if (!Double.isNaN(nextKm) && nextKm <= 0) { Ui.err(etAirNextKm, "Reminder odometer must be greater than 0"); return; }
        if (!Double.isNaN(installKm) && !Double.isNaN(nextKm) && nextKm <= installKm) {
            Ui.err(etAirNextKm, "Must be higher than the installation odometer"); return;
        }
        if (airNextDate <= 0 && Double.isNaN(nextKm)) {
            Ui.err(etAirNextKm, "Set a reminder date or a reminder odometer"); return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("airInstallDate", airInstallDate);
        m.put("airInstallKm", Double.isNaN(installKm) ? -1.0 : installKm);
        m.put("airNextDate", airNextDate);
        m.put("airNextKm", Double.isNaN(nextKm) ? -1.0 : nextKm);
        m.put("airDone", false);
        write(m);
        Ui.toast(this, "Air filter reminder saved");
    }

    private void airDoneDialog() {
        Bike b = DataStore.get().bike(bikeId);
        TextInputLayout tilOdo = Ui.field(this, "Odometer now (km, optional)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputLayout tilMonths = Ui.field(this, "Next service after (months)", InputType.TYPE_CLASS_NUMBER);
        TextInputLayout tilKm = Ui.field(this, "Next service after (km, optional)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputEditText etOdo = Ui.edit(tilOdo), etMonths = Ui.edit(tilMonths), etKm = Ui.edit(tilKm);
        if (b != null) etOdo.setText(Fmt.plain(b.getOdometer()));
        etMonths.setText("6");
        AlertDialog d = buildDialog("Mark air filter as serviced", tilOdo, tilMonths, tilKm);
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double odo = Fmt.parse(etOdo.getText()), months = Fmt.parse(etMonths.getText()), kmInterval = Fmt.parse(etKm.getText());
            if (Double.isNaN(months) || months < 1) { Ui.err(etMonths, "Enter 1 or more months"); return; }
            if (!Double.isNaN(kmInterval) && Double.isNaN(odo)) { Ui.err(etOdo, "Enter the odometer to use a km interval"); return; }
            long now = System.currentTimeMillis();
            long nextDate = Fmt.addMonths(now, (int) months);
            double nextKm = (!Double.isNaN(odo) && !Double.isNaN(kmInterval) && kmInterval > 0) ? odo + kmInterval : -1;
            Map<String, Object> m = new HashMap<>();
            m.put("airInstallDate", now);
            m.put("airInstallKm", Double.isNaN(odo) ? -1.0 : odo);
            m.put("airNextDate", nextDate);
            m.put("airNextKm", nextKm);
            m.put("airDone", true);
            write(m);
            airInstallDate = now;
            btnAirInstallDate.setText(Fmt.date(now));
            etAirInstallKm.setText(Double.isNaN(odo) ? "" : Fmt.plain(odo));
            airNextDate = nextDate;
            showNextDate();
            etAirNextKm.setText(nextKm > 0 ? Fmt.plain(nextKm) : "");
            d.dismiss();
            Ui.toast(this, "Completed. Next reminder on " + Fmt.date(nextDate));
        }));
        d.show();
    }

    private AlertDialog buildDialog(String title, TextInputLayout... fields) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) Ui.dp(this, 20);
        box.setPadding(pad, pad / 2, pad, 0);
        for (TextInputLayout f : fields) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = (int) Ui.dp(this, 8);
            box.addView(f, lp);
        }
        return new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Complete", null)
                .setNegativeButton("Cancel", null)
                .create();
    }
}
