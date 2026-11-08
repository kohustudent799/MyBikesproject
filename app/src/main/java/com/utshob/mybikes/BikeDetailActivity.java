package com.utshob.mybikes;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.MileageCalculator;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.ImageUtil;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;
import com.utshob.mybikes.work.ReminderScheduler;

import java.util.List;

public class BikeDetailActivity extends AppCompatActivity implements DataStore.Listener {
    private String bikeId;
    private ImageView ivPhoto;
    private TextView tvName, tvInfo, tvOdo, tvMileage, tvFuelTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_detail);
        DataStore.get().start();
        bikeId = getIntent().getStringExtra("bike_id");
        ivPhoto = findViewById(R.id.ivPhoto);
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        tvOdo = findViewById(R.id.tvOdo);
        tvMileage = findViewById(R.id.tvMileage);
        tvFuelTotal = findViewById(R.id.tvFuelTotal);

        findViewById(R.id.btnEdit).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditBikeActivity.class).putExtra("bike_id", bikeId)));
        findViewById(R.id.btnMaintenance).setOnClickListener(v ->
                startActivity(new Intent(this, MaintenanceActivity.class).putExtra("bike_id", bikeId)));
        findViewById(R.id.btnAddFuel).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditFuelActivity.class).putExtra("bike_id", bikeId)));
        findViewById(R.id.btnUpdateOdo).setOnClickListener(v -> updateOdometerDialog());
        findViewById(R.id.btnSetActive).setOnClickListener(v -> {
            Prefs.setSelectedBike(bikeId);
            Ui.toast(this, "This is now your active bike");
        });
        findViewById(R.id.btnDelete).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this bike?")
                .setMessage("Its fuel history, expenses, reminders and photo will be deleted permanently.")
                .setPositiveButton("Delete", (d, w) -> deleteBike())
                .setNegativeButton("Cancel", null)
                .show());
    }

    @Override protected void onStart() {
        super.onStart();
        DataStore.get().addListener(this);
        bind();
    }

    @Override protected void onStop() {
        super.onStop();
        DataStore.get().removeListener(this);
    }

    @Override public void onDataChanged() { bind(); }

    private void bind() {
        DataStore ds = DataStore.get();
        Bike b = ds.bike(bikeId);
        if (b == null) {
            if (ds.isLoaded()) finish();     // deleted
            return;
        }
        Ui.loadPhoto(ivPhoto, b.getPhotoUrl());
        tvName.setText(b.getName());
        tvInfo.setText("Brand / model: " + b.getBrand() + " " + b.getModel()
                + "\nRegistration: " + (b.getRegNo().isEmpty() ? "-" : b.getRegNo())
                + "\nEngine: " + b.getEngineCc() + " cc"
                + "\nFuel type: " + b.getFuelType()
                + "\nPurchased: " + Fmt.date(b.getPurchaseDate()));
        tvOdo.setText(Fmt.km(b.getOdometer()));
        List<FuelRecord> fuel = ds.fuelFor(bikeId);
        Double latest = MileageCalculator.latestMileage(fuel);
        tvMileage.setText(latest == null ? "Not yet (needs 2 full tanks)" : Fmt.mileage(latest));
        double total = 0, litres = 0;
        for (FuelRecord r : fuel) { total += r.getTotalCost(); litres += r.getLitres(); }
        tvFuelTotal.setText(Fmt.money(total) + "  ·  " + Fmt.litres(litres));
    }

    private void updateOdometerDialog() {
        Bike b = DataStore.get().bike(bikeId);
        if (b == null) return;
        TextInputLayout til = Ui.field(this, "Current odometer (km)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        TextInputEditText et = Ui.edit(til);
        et.setText(Fmt.plain(b.getOdometer()));
        LinearLayout box = new LinearLayout(this);
        int pad = (int) Ui.dp(this, 20);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(til, new LinearLayout.LayoutParams(-1, -2));
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Update odometer")
                .setView(box)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            double odo = Fmt.parse(Ui.str(et));
            double maxFuel = MileageCalculator.maxOdometer(DataStore.get().fuelFor(bikeId));
            if (Double.isNaN(odo) || odo < 0) { Ui.err(et, "Enter a valid number"); return; }
            if (odo < maxFuel) { Ui.err(et, "Lower than a saved fuel record (" + Fmt.km(maxFuel) + ")"); return; }
            DataStore.userRoot().child("bikes").child(bikeId).child("odometer").setValue(odo);
            ReminderScheduler.checkNow(this);
            dialog.dismiss();
            Ui.toast(this, "Odometer updated");
        }));
        dialog.show();
    }

    private void deleteBike() {
        Bike b = DataStore.get().bike(bikeId);
        String photo = b == null ? "" : b.getPhotoUrl();
        DatabaseReference root = DataStore.userRoot();
        root.child("fuel").child(bikeId).removeValue();
        root.child("expenses").child(bikeId).removeValue();
        root.child("maintenance").child(bikeId).removeValue();
        root.child("bikes").child(bikeId).removeValue();
        ImageUtil.delete(this, photo);
        if (bikeId.equals(Prefs.selectedBike())) Prefs.setSelectedBike(null);
        Ui.toast(this, "Bike deleted");
        finish();
    }
}
