package com.utshob.mybikes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.utshob.mybikes.AddEditFuelActivity;
import com.utshob.mybikes.R;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.util.BikeSpinnerHelper;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.FuelUi;
import com.utshob.mybikes.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tab 3: Fuel history with sort, filter and totals. */
public class HistoryFragment extends Fragment implements DataStore.Listener {
    private BikeSpinnerHelper spinnerHelper;
    private ChipGroup chipGroup;
    private MaterialButton btnSort;
    private TextView tvLitres, tvCost, tvAvg, tvEmpty;
    private FuelAdapter adapter;
    private boolean newestFirst = true;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_history, c, false);
        chipGroup = v.findViewById(R.id.chipGroup);
        btnSort = v.findViewById(R.id.btnSort);
        tvLitres = v.findViewById(R.id.tvTotalLitres);
        tvCost = v.findViewById(R.id.tvTotalCost);
        tvAvg = v.findViewById(R.id.tvAvgMileage);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        RecyclerView rv = v.findViewById(R.id.rvFuel);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FuelAdapter(r -> FuelUi.showDetails(requireContext(), r));
        rv.setAdapter(adapter);

        spinnerHelper = new BikeSpinnerHelper((Spinner) v.findViewById(R.id.spBike), "selected_bike", false, this::refreshList);
        chipGroup.setOnCheckedStateChangeListener((g, ids) -> refreshList());
        btnSort.setOnClickListener(x -> {
            newestFirst = !newestFirst;
            btnSort.setText(newestFirst ? "Newest first" : "Oldest first");
            refreshList();
        });
        FloatingActionButton fab = v.findViewById(R.id.fabAddFuel);
        fab.setOnClickListener(x -> {
            Bike b = DataStore.get().selectedBike();
            if (b == null) Ui.toast(requireContext(), "Add a bike first");
            else startActivity(new Intent(getContext(), AddEditFuelActivity.class).putExtra("bike_id", b.getId()));
        });
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        DataStore.get().addListener(this);
        spinnerHelper.refresh();
        refreshList();
    }

    @Override public void onStop() {
        super.onStop();
        DataStore.get().removeListener(this);
    }

    @Override public void onDataChanged() {
        if (!isAdded()) return;
        spinnerHelper.refresh();
        refreshList();
    }

    private String filterType() {
        int id = chipGroup.getCheckedChipId();
        if (id == R.id.chipPetrol) return "Petrol";
        if (id == R.id.chipOctane) return "Octane";
        if (id == R.id.chipDiesel) return "Diesel";
        return null;
    }

    private void refreshList() {
        if (!isAdded() || getView() == null) return;
        Bike sel = DataStore.get().selectedBike();
        List<FuelRecord> list = new ArrayList<>();
        if (sel != null) {
            String type = filterType();
            for (FuelRecord r : DataStore.get().fuelFor(sel.getId())) {
                if (type == null || type.equals(r.getFuelType())) list.add(r);
            }
        }
        double litres = 0, cost = 0, mSum = 0;
        int mCount = 0;
        for (FuelRecord r : list) {
            litres += r.getLitres();
            cost += r.getTotalCost();
            if (r.hasMileage()) { mSum += r.getMileage(); mCount++; }
        }
        tvLitres.setText(Fmt.litres(litres));
        tvCost.setText(Fmt.money(cost));
        tvAvg.setText(mCount == 0 ? "—" : Fmt.mileage(mSum / mCount));
        if (newestFirst) Collections.reverse(list);
        adapter.submit(list);
        tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(sel == null ? "Add a bike in the Garage tab first." : "No fuel records yet. Tap + to add one.");
    }
}
