package com.utshob.mybikes;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.MileageCalculator;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.Ui;
import com.utshob.mybikes.work.ReminderScheduler;

/** Add / edit one fuel record. Extras: "bike_id" (required), "fuel_id" (when editing). */
public class AddEditFuelActivity extends AppCompatActivity {
    private TextInputEditText etOdo, etPrice, etCost, etLitres;
    private MaterialButton btnDateTime;
    private Spinner spType;
    private SwitchMaterial swFull;
    private TextView tvPreview;

    private String bikeId, fuelId;
    private long timestamp = System.currentTimeMillis();
    private boolean skipFirstSpinnerEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_fuel);
        DataStore.get().start();
        bikeId = getIntent().getStringExtra("bike_id");
        fuelId = getIntent().getStringExtra("fuel_id");
        if (bikeId == null) { finish(); return; }

        etOdo = findViewById(R.id.etOdo);
        etPrice = findViewById(R.id.etPrice);
        etCost = findViewById(R.id.etCost);
        etLitres = findViewById(R.id.etLitres);
        btnDateTime = findViewById(R.id.btnDateTime);
        spType = findViewById(R.id.spFuelType);
        swFull = findViewById(R.id.swFull);
        tvPreview = findViewById(R.id.tvPreview);
        TextView title = findViewById(R.id.tvTitle);

        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConfig.FUEL_TYPES));
        btnDateTime.setText(Fmt.dateTime(timestamp));
        btnDateTime.setOnClickListener(v -> Ui.pickDateTime(this, timestamp, t -> {
            timestamp = t;
            btnDateTime.setText(Fmt.dateTime(t));
        }));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { updatePreview(); }
        };
        etPrice.addTextChangedListener(watcher);
        etCost.addTextChangedListener(watcher);
        etLitres.addTextChangedListener(watcher);

        skipFirstSpinnerEvent = fuelId != null;      // when editing keep the SAVED price
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (skipFirstSpinnerEvent) { skipFirstSpinnerEvent = false; return; }
                // Automatically use the saved price for this fuel type (user may still change it).
                Double saved = DataStore.get().price(AppConfig.FUEL_TYPES[pos]);
                if (saved != null) etPrice.setText(Fmt.plain(saved));
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        if (fuelId == null) {
            title.setText("Add fuel");
            DataStore.get().runWhenLoaded(() -> {
                Bike b = DataStore.get().bike(bikeId);
                if (b == null) return;
                etOdo.setHint("Odometer (km), last known " + Fmt.plain(b.getOdometer()));
                for (int i = 0; i < AppConfig.FUEL_TYPES.length; i++) {
                    if (AppConfig.FUEL_TYPES[i].equals(b.getFuelType())) spType.setSelection(i);
                }
                Double saved = DataStore.get().price(b.getFuelType());
                if (saved != null && Ui.str(etPrice).isEmpty()) etPrice.setText(Fmt.plain(saved));
            });
        } else {
            title.setText("Edit fuel record");
            DataStore.get().runWhenLoaded(() -> {
                FuelRecord found = null;
                for (FuelRecord r : DataStore.get().fuelFor(bikeId)) if (fuelId.equals(r.getId())) found = r;
                if (found == null) { finish(); return; }
                timestamp = found.getTimestamp();
                btnDateTime.setText(Fmt.dateTime(timestamp));
                etOdo.setText(Fmt.plain(found.getOdometer()));
                etPrice.setText(Fmt.plain(found.getPricePerLitre()));
                etCost.setText(Fmt.plain(found.getTotalCost()));
                etLitres.setText(Fmt.plain(found.getLitres()));
                swFull.setChecked(found.isFullTank());
                for (int i = 0; i < AppConfig.FUEL_TYPES.length; i++) {
                    if (AppConfig.FUEL_TYPES[i].equals(found.getFuelType())) spType.setSelection(i);
                }
            });
        }
    }

    private void updatePreview() {
        double price = Fmt.parse(etPrice.getText()), cost = Fmt.parse(etCost.getText()), litres = Fmt.parse(etLitres.getText());
        if (Double.isNaN(litres) && !Double.isNaN(cost) && !Double.isNaN(price) && price > 0) {
            tvPreview.setText("Fuel quantity = " + Fmt.plain(cost) + " ÷ " + Fmt.plain(price) + " = " + Fmt.litres(cost / price));
        } else if (Double.isNaN(cost) && !Double.isNaN(litres) && !Double.isNaN(price)) {
            tvPreview.setText("Total cost = " + Fmt.plain(litres) + " × " + Fmt.plain(price) + " = " + Fmt.money(litres * price));
        } else {
            tvPreview.setText("Enter the price and either the total cost or the litres. The other value is calculated.");
        }
    }

    private void save() {
        double odo = Fmt.parse(etOdo.getText());
        double price = Fmt.parse(etPrice.getText());
        double cost = Fmt.parse(etCost.getText());
        double litres = Fmt.parse(etLitres.getText());

        if (Double.isNaN(odo) || odo < 0) { Ui.err(etOdo, "Enter the odometer reading"); return; }
        if (Double.isNaN(price) || price <= 0) { Ui.err(etPrice, "Price per litre must be greater than 0"); return; }
        if (Double.isNaN(cost) && Double.isNaN(litres)) { Ui.err(etCost, "Enter the total cost or the litres"); return; }

        boolean bothGiven = !Double.isNaN(cost) && !Double.isNaN(litres);
        if (bothGiven && Math.abs(litres * price - cost) > Math.max(1.0, 0.02 * cost)) {
            Ui.err(etCost, "Cost should be about litres × price (" + Fmt.plain(Math.round(litres * price * 100) / 100.0) + ")");
            return;
        }
        if (Double.isNaN(litres)) litres = cost / price;          // Litres = Total cost ÷ Price per litre
        else if (Double.isNaN(cost)) cost = litres * price;
        litres = Math.round(litres * 1000) / 1000.0;
        cost = Math.round(cost * 100) / 100.0;
        if (litres <= 0) { Ui.err(etLitres, "Fuel quantity must be greater than 0"); return; }
        if (cost <= 0) { Ui.err(etCost, "Total cost must be greater than 0"); return; }

        String odoError = MileageCalculator.checkOdometer(DataStore.get().fuelFor(bikeId), fuelId, timestamp, odo);
        if (odoError != null) { Ui.err(etOdo, odoError); return; }

        FuelRecord r = new FuelRecord();
        r.setBikeId(bikeId);
        r.setTimestamp(timestamp);
        r.setOdometer(odo);
        r.setFuelType((String) spType.getSelectedItem());
        r.setPricePerLitre(price);          // the actual price used is stored inside the record
        r.setTotalCost(cost);
        r.setLitres(litres);
        r.setFullTank(swFull.isChecked());

        String id = fuelId != null ? fuelId : DataStore.userRoot().child("fuel").child(bikeId).push().getKey();
        DataStore.userRoot().child("fuel").child(bikeId).child(id).setValue(r)
                .addOnFailureListener(e -> Ui.toast(getApplicationContext(), "Save failed: " + e.getMessage()));

        // The odometer was typed by the user, so we can store it on the bike if it is higher.
        Bike b = DataStore.get().bike(bikeId);
        if (b != null && odo > b.getOdometer()) {
            DataStore.userRoot().child("bikes").child(bikeId).child("odometer").setValue(odo);
        }
        ReminderScheduler.checkNow(this);
        Ui.toast(this, fuelId == null ? "Fuel record added" : "Fuel record updated");
        finish();
    }
}
