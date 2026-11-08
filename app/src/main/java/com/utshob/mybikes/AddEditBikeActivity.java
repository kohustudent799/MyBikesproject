package com.utshob.mybikes;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.MileageCalculator;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.ImageUtil;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;

/** Add a new bike or edit an existing one (extra "bike_id"). Handles the profile photo (gallery / photo picker). */
public class AddEditBikeActivity extends AppCompatActivity {
    private TextInputEditText etName, etBrand, etModel, etReg, etCc, etOdo;
    private MaterialButton btnDate, btnSave, btnPick, btnRemove;
    private Spinner spFuel;
    private ImageView ivPhoto;
    private ProgressBar progress;
    private View root;

    private String bikeId;
    private Bike editing;
    private long purchaseDate = System.currentTimeMillis();
    private Uri newPhotoUri;
    private boolean removePhoto = false;

    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    newPhotoUri = uri;
                    removePhoto = false;
                    ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(uri).into(ivPhoto);      // preview
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_bike);
        DataStore.get().start();
        root = findViewById(R.id.bikeFormRoot);
        etName = findViewById(R.id.etName);
        etBrand = findViewById(R.id.etBrand);
        etModel = findViewById(R.id.etModel);
        etReg = findViewById(R.id.etReg);
        etCc = findViewById(R.id.etCc);
        etOdo = findViewById(R.id.etOdo);
        btnDate = findViewById(R.id.btnPurchaseDate);
        btnSave = findViewById(R.id.btnSave);
        btnPick = findViewById(R.id.btnPickPhoto);
        btnRemove = findViewById(R.id.btnRemovePhoto);
        spFuel = findViewById(R.id.spFuelType);
        ivPhoto = findViewById(R.id.ivPhoto);
        progress = findViewById(R.id.progress);
        TextView title = findViewById(R.id.tvTitle);

        spFuel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConfig.FUEL_TYPES));
        btnDate.setText(Fmt.date(purchaseDate));
        btnDate.setOnClickListener(v -> Ui.pickDate(this, purchaseDate, d -> {
            purchaseDate = d;
            btnDate.setText(Fmt.date(d));
        }));
        btnPick.setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        btnRemove.setOnClickListener(v -> {
            newPhotoUri = null;
            removePhoto = true;
            Ui.loadPhoto(ivPhoto, "");
        });
        btnSave.setOnClickListener(v -> save());
        Ui.loadPhoto(ivPhoto, "");

        bikeId = getIntent().getStringExtra("bike_id");
        if (bikeId != null) {
            title.setText("Edit bike");
            DataStore.get().runWhenLoaded(() -> {
                editing = DataStore.get().bike(bikeId);
                if (editing == null) { finish(); return; }
                etName.setText(editing.getName());
                etBrand.setText(editing.getBrand());
                etModel.setText(editing.getModel());
                etReg.setText(editing.getRegNo());
                etCc.setText(String.valueOf(editing.getEngineCc()));
                etOdo.setText(Fmt.plain(editing.getOdometer()));
                purchaseDate = editing.getPurchaseDate();
                btnDate.setText(Fmt.date(purchaseDate));
                for (int i = 0; i < AppConfig.FUEL_TYPES.length; i++) {
                    if (AppConfig.FUEL_TYPES[i].equals(editing.getFuelType())) spFuel.setSelection(i);
                }
                Ui.loadPhoto(ivPhoto, editing.getPhotoUrl());
            });
        } else {
            title.setText("Add a bike");
        }
    }

    private void save() {
        String name = Ui.str(etName);
        if (name.isEmpty()) { Ui.err(etName, "Enter a bike name"); return; }
        String brand = Ui.str(etBrand);
        if (brand.isEmpty()) { Ui.err(etBrand, "Enter the brand"); return; }
        String model = Ui.str(etModel);
        if (model.isEmpty()) { Ui.err(etModel, "Enter the model"); return; }
        double cc = Fmt.parse(Ui.str(etCc));
        if (Double.isNaN(cc) || cc <= 0) { Ui.err(etCc, "Enter engine CC (e.g. 150)"); return; }
        double odo = Fmt.parse(Ui.str(etOdo));
        if (Double.isNaN(odo) || odo < 0) { Ui.err(etOdo, "Enter the current odometer reading"); return; }
        if (bikeId != null) {
            double maxFuelOdo = MileageCalculator.maxOdometer(DataStore.get().fuelFor(bikeId));
            if (odo < maxFuelOdo) { Ui.err(etOdo, "Lower than a saved fuel record (" + Fmt.km(maxFuelOdo) + ")"); return; }
        }

        DatabaseReference bikes = DataStore.userRoot().child("bikes");
        final String id = bikeId != null ? bikeId : bikes.push().getKey();
        final Bike b = new Bike();
        b.setName(name);
        b.setBrand(brand);
        b.setModel(model);
        b.setRegNo(Ui.str(etReg));
        b.setEngineCc((int) cc);
        b.setPurchaseDate(purchaseDate);
        b.setFuelType((String) spFuel.getSelectedItem());
        b.setOdometer(odo);
        final String oldUrl = editing == null ? "" : editing.getPhotoUrl();
        b.setPhotoUrl(oldUrl);

        busy(true);
        if (newPhotoUri != null) {
            try {
                String fileName = ImageUtil.saveToDevice(this, newPhotoUri);
                ImageUtil.delete(this, oldUrl);      // replaced photo
                b.setPhotoUrl(fileName);             // only the file name goes to the database
            } catch (Exception e) {
                busy(false);
                Ui.snack(root, "Could not save the photo: " + e.getMessage());
                return;
            }
        } else if (removePhoto) {
            ImageUtil.delete(this, oldUrl);
            b.setPhotoUrl("");
        }
        write(id, b);
    }

    private void write(String id, Bike b) {
        DataStore.userRoot().child("bikes").child(id).setValue(b)
                .addOnFailureListener(e -> Ui.toast(getApplicationContext(), "Save failed: " + e.getMessage()));
        if (bikeId == null) Prefs.setSelectedBike(id);    // new bike becomes the active one
        com.utshob.mybikes.work.ReminderScheduler.checkNow(this);
        Ui.toast(this, bikeId == null ? "Bike added" : "Bike updated");
        finish();
    }

    private void busy(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!b);
        btnPick.setEnabled(!b);
        btnRemove.setEnabled(!b);
    }
}
