package com.utshob.mybikes.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utshob.mybikes.AppConfig;
import com.utshob.mybikes.LoginActivity;
import com.utshob.mybikes.R;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;
import com.utshob.mybikes.work.ReminderScheduler;

import java.util.HashMap;
import java.util.Map;

/** Tab 4: Settings. */
public class SettingsFragment extends Fragment {
    private TextInputEditText etPetrol, etOctane, etDiesel;
    private boolean pricesFilled = false;
    private final DataStore.Listener listener = this::fillPrices;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_settings, c, false);

        // ---- Account ----
        TextView tvName = v.findViewById(R.id.tvAccName);
        TextView tvEmail = v.findViewById(R.id.tvAccEmail);
        ImageView ivAcc = v.findViewById(R.id.ivAcc);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            tvName.setText(u.getDisplayName() == null || u.getDisplayName().isEmpty() ? "Rider" : u.getDisplayName());
            tvEmail.setText(u.getEmail() == null ? "" : u.getEmail());
            if (u.getPhotoUrl() != null) Ui.loadPhoto(ivAcc, u.getPhotoUrl().toString());
            else ivAcc.setImageResource(R.drawable.ic_bike);
        }

        // ---- Fuel prices ----
        etPetrol = v.findViewById(R.id.etPricePetrol);
        etOctane = v.findViewById(R.id.etPriceOctane);
        etDiesel = v.findViewById(R.id.etPriceDiesel);
        v.findViewById(R.id.btnSavePrices).setOnClickListener(x -> savePrices());

        // ---- Notifications ----
        SwitchMaterial sw = v.findViewById(R.id.switchNotif);
        sw.setChecked(Prefs.remindersEnabled());
        sw.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setRemindersEnabled(checked);
            if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        // ---- Theme ----
        RadioGroup rg = v.findViewById(R.id.rgTheme);
        int mode = Prefs.themeMode();
        rg.check(mode == AppCompatDelegate.MODE_NIGHT_YES ? R.id.rbDark
                : mode == AppCompatDelegate.MODE_NIGHT_NO ? R.id.rbLight : R.id.rbSystem);
        rg.setOnCheckedChangeListener((group, id) -> {
            int m = id == R.id.rbDark ? AppCompatDelegate.MODE_NIGHT_YES
                    : id == R.id.rbLight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (m != Prefs.themeMode()) {
                Prefs.setThemeMode(m);
                AppCompatDelegate.setDefaultNightMode(m);
            }
        });

        // ---- Currency ----
        Spinner spCur = v.findViewById(R.id.spCurrency);
        spCur.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, AppConfig.CURRENCIES));
        for (int i = 0; i < AppConfig.CURRENCIES.length; i++) {
            if (AppConfig.CURRENCIES[i].equals(Prefs.currency())) spCur.setSelection(i);
        }
        spCur.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                String chosen = AppConfig.CURRENCIES[pos];
                if (!chosen.equals(Prefs.currency())) {
                    Prefs.setCurrency(chosen);
                    Ui.toast(requireContext(), "Currency set to " + chosen);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        // ---- Logout ----
        v.findViewById(R.id.btnLogout).setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("You will need to sign in again.")
                .setPositiveButton("Log out", (d, w) -> logout())
                .setNegativeButton("Cancel", null)
                .show());
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        DataStore.get().addListener(listener);
        fillPrices();
    }

    @Override public void onStop() {
        super.onStop();
        DataStore.get().removeListener(listener);
    }

    private void fillPrices() {
        if (pricesFilled || !isAdded() || !DataStore.get().isLoaded()) return;
        pricesFilled = true;
        Map<String, Double> p = DataStore.get().prices;
        if (p.containsKey("Petrol")) etPetrol.setText(Fmt.plain(p.get("Petrol")));
        if (p.containsKey("Octane")) etOctane.setText(Fmt.plain(p.get("Octane")));
        if (p.containsKey("Diesel")) etDiesel.setText(Fmt.plain(p.get("Diesel")));
    }

    private void savePrices() {
        String[] names = AppConfig.FUEL_TYPES;
        TextInputEditText[] fields = {etPetrol, etOctane, etDiesel};
        Map<String, Object> out = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            String t = Ui.str(fields[i]);
            if (t.isEmpty()) continue;                       // leave a price unset
            double d = Fmt.parse(t);
            if (Double.isNaN(d) || d <= 0) { Ui.err(fields[i], "Enter a price greater than 0"); return; }
            out.put(names[i], d);
        }
        if (out.isEmpty()) { Ui.err(etPetrol, "Enter at least one price"); return; }
        // Only the default for NEW records changes. Old fuel records keep the price they were saved with.
        DataStore.userRoot().child("fuelPrices").updateChildren(out)
                .addOnSuccessListener(v -> Ui.toast(requireContext(), "Fuel prices saved"))
                .addOnFailureListener(e -> Ui.toast(requireContext(), "Could not save: " + e.getMessage()));
    }

    private void logout() {
        DataStore.get().stop();
        ReminderScheduler.cancel(requireContext());
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        startActivity(new Intent(getContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        requireActivity().finish();
    }
}
