package com.utshob.mybikes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.utshob.mybikes.AddEditBikeActivity;
import com.utshob.mybikes.AddEditFuelActivity;
import com.utshob.mybikes.BikeDetailActivity;
import com.utshob.mybikes.BrtaActivity;
import com.utshob.mybikes.R;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.ExpenseMath;
import com.utshob.mybikes.data.MileageCalculator;
import com.utshob.mybikes.data.ReminderLogic;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.model.Maintenance;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.FuelUi;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tab 1: Garage = the home dashboard. */
public class GarageFragment extends Fragment implements DataStore.Listener {
    private TextView tvUserName, tvBikeCount, tvNoBikes, tvSelName, tvSelSub, tvSelOdo, tvSelMileage,
            tvSelFuelMonth, tvSelTotalMonth, tvNoReminders, tvNoRecent;
    private ImageView ivUser;
    private View cardSelected;
    private LinearLayout llReminders;
    private BikeAdapter bikeAdapter;
    private FuelAdapter recentAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_garage, c, false);
        tvUserName = v.findViewById(R.id.tvUserName);
        tvBikeCount = v.findViewById(R.id.tvBikeCount);
        tvNoBikes = v.findViewById(R.id.tvNoBikes);
        tvSelName = v.findViewById(R.id.tvSelName);
        tvSelSub = v.findViewById(R.id.tvSelSub);
        tvSelOdo = v.findViewById(R.id.tvSelOdo);
        tvSelMileage = v.findViewById(R.id.tvSelMileage);
        tvSelFuelMonth = v.findViewById(R.id.tvSelFuelMonth);
        tvSelTotalMonth = v.findViewById(R.id.tvSelTotalMonth);
        tvNoReminders = v.findViewById(R.id.tvNoReminders);
        tvNoRecent = v.findViewById(R.id.tvNoRecent);
        ivUser = v.findViewById(R.id.ivUser);
        cardSelected = v.findViewById(R.id.cardSelected);
        llReminders = v.findViewById(R.id.llReminders);

        RecyclerView rvBikes = v.findViewById(R.id.rvBikes);
        rvBikes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        bikeAdapter = new BikeAdapter((bike, alreadySelected) -> {
            if (alreadySelected) {
                startActivity(new Intent(getContext(), BikeDetailActivity.class).putExtra("bike_id", bike.getId()));
            } else {
                Prefs.setSelectedBike(bike.getId());     // switch between bikes
                refresh();
            }
        });
        rvBikes.setAdapter(bikeAdapter);

        RecyclerView rvRecent = v.findViewById(R.id.rvRecent);
        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecent.setNestedScrollingEnabled(false);
        recentAdapter = new FuelAdapter(r -> FuelUi.showDetails(requireContext(), r));
        rvRecent.setAdapter(recentAdapter);

        v.findViewById(R.id.btnAddBike).setOnClickListener(x -> startActivity(new Intent(getContext(), AddEditBikeActivity.class)));
        v.findViewById(R.id.btnDetails).setOnClickListener(x -> {
            Bike b = DataStore.get().selectedBike();
            if (b != null) startActivity(new Intent(getContext(), BikeDetailActivity.class).putExtra("bike_id", b.getId()));
        });
        v.findViewById(R.id.btnQuickFuel).setOnClickListener(x -> {
            Bike b = DataStore.get().selectedBike();
            if (b != null) startActivity(new Intent(getContext(), AddEditFuelActivity.class).putExtra("bike_id", b.getId()));
        });
        MaterialButton portal = v.findViewById(R.id.btnBrtaPortal);
        MaterialButton helpline = v.findViewById(R.id.btnBrtaHelpline);
        portal.setOnClickListener(x -> startActivity(new Intent(getContext(), BrtaActivity.class)));
        helpline.setOnClickListener(x -> startActivity(new Intent(getContext(), BrtaActivity.class)));

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            String name = u.getDisplayName() != null && !u.getDisplayName().isEmpty() ? u.getDisplayName()
                    : (u.getEmail() != null ? u.getEmail() : "Rider");
            tvUserName.setText(name);
            if (u.getPhotoUrl() != null) Ui.loadPhoto(ivUser, u.getPhotoUrl().toString());
            else ivUser.setImageResource(R.drawable.ic_bike);
        }
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        DataStore.get().addListener(this);
        refresh();
    }

    @Override public void onStop() {
        super.onStop();
        DataStore.get().removeListener(this);
    }

    @Override public void onDataChanged() { refresh(); }

    private void refresh() {
        if (!isAdded() || getView() == null) return;
        DataStore ds = DataStore.get();
        tvBikeCount.setText(String.valueOf(ds.bikes.size()));
        Bike sel = ds.selectedBike();
        bikeAdapter.submit(ds.bikes, sel == null ? null : sel.getId());
        tvNoBikes.setVisibility(ds.bikes.isEmpty() ? View.VISIBLE : View.GONE);

        // upcoming reminders for ALL bikes
        llReminders.removeAllViews();
        List<ReminderLogic.Reminder> all = new ArrayList<>();
        for (Bike b : ds.bikes) {
            Maintenance m = ds.maintenance.get(b.getId());
            all.addAll(ReminderLogic.evaluate(b, m));
        }
        Collections.sort(all, (a, b) -> Boolean.compare(b.due, a.due));   // due first
        tvNoReminders.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
        for (ReminderLogic.Reminder r : all) {
            TextView t = new TextView(requireContext());
            t.setText((r.due ? "⚠ " : "• ") + r.title + "\n   " + r.text);
            t.setTextSize(14);
            t.setPadding(0, 6, 0, 6);
            t.setTextColor(ContextCompat.getColor(requireContext(), r.due ? R.color.red : R.color.text_primary));
            llReminders.addView(t);
        }

        if (sel == null) {
            cardSelected.setVisibility(View.GONE);
            recentAdapter.submit(new ArrayList<>());
            tvNoRecent.setVisibility(View.VISIBLE);
            return;
        }
        cardSelected.setVisibility(View.VISIBLE);
        tvSelName.setText(sel.getName());
        tvSelSub.setText((sel.getBrand() + " " + sel.getModel()).trim()
                + (sel.getRegNo().isEmpty() ? "" : "  ·  " + sel.getRegNo()));
        tvSelOdo.setText(Fmt.km(sel.getOdometer()));

        List<FuelRecord> fuel = ds.fuelFor(sel.getId());
        Double latest = MileageCalculator.latestMileage(fuel);
        tvSelMileage.setText(latest == null ? "Not yet" : Fmt.mileage(latest));

        long now = System.currentTimeMillis();
        long from = Fmt.monthStart(now), to = Fmt.nextMonthStart(now);
        double fuelMonth = ExpenseMath.fuelCost(fuel, from, to);
        double add = ExpenseMath.additional(ds.expensesFor(sel.getId()), from, to);
        tvSelFuelMonth.setText(Fmt.money(fuelMonth));
        tvSelTotalMonth.setText(Fmt.money(fuelMonth + add));

        List<FuelRecord> recent = new ArrayList<>(fuel);
        Collections.reverse(recent);
        if (recent.size() > 3) recent = new ArrayList<>(recent.subList(0, 3));
        recentAdapter.submit(recent);
        tvNoRecent.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
