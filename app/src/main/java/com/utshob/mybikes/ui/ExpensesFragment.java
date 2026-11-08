package com.utshob.mybikes.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.utshob.mybikes.AddEditExpenseActivity;
import com.utshob.mybikes.R;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.data.ExpenseMath;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.ExpenseRecord;
import com.utshob.mybikes.util.BikeSpinnerHelper;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tab 2: Monthly expense report (one bike or all bikes). */
public class ExpensesFragment extends Fragment implements DataStore.Listener {
    private static final String[] SHORT = {"Fuel", "Oil", "Maint.", "Repair", "Parts", "Other"};
    private static final int[] PALETTE = {
            Color.parseColor("#2E7D32"), Color.parseColor("#F9A825"), Color.parseColor("#1565C0"),
            Color.parseColor("#C62828"), Color.parseColor("#6A1B9A"), Color.parseColor("#00838F")};

    private BikeSpinnerHelper spinnerHelper;
    private TextView tvMonth, tvFuel, tvAdd, tvTotal, tvDistance, tvLitres, tvCompare, tvNoExp;
    private BarChartView chartCategory, chartCompare;
    private ExpenseAdapter adapter;
    private long monthTs = System.currentTimeMillis();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_expenses, c, false);
        tvMonth = v.findViewById(R.id.tvMonth);
        tvFuel = v.findViewById(R.id.tvFuelExp);
        tvAdd = v.findViewById(R.id.tvAddExp);
        tvTotal = v.findViewById(R.id.tvTotalExp);
        tvDistance = v.findViewById(R.id.tvDistance);
        tvLitres = v.findViewById(R.id.tvLitres);
        tvCompare = v.findViewById(R.id.tvCompare);
        tvNoExp = v.findViewById(R.id.tvNoExp);
        chartCategory = v.findViewById(R.id.chartCategory);
        chartCompare = v.findViewById(R.id.chartCompare);
        RecyclerView rv = v.findViewById(R.id.rvExpenses);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setNestedScrollingEnabled(false);
        adapter = new ExpenseAdapter(this::showExpenseDialog);
        rv.setAdapter(adapter);

        spinnerHelper = new BikeSpinnerHelper((Spinner) v.findViewById(R.id.spScope), "expense_scope", true, this::refresh);
        v.findViewById(R.id.btnPrev).setOnClickListener(x -> { monthTs = Fmt.addMonths(monthTs, -1); refresh(); });
        v.findViewById(R.id.btnNext).setOnClickListener(x -> { monthTs = Fmt.addMonths(monthTs, 1); refresh(); });
        FloatingActionButton fab = v.findViewById(R.id.fabAddExpense);
        fab.setOnClickListener(x -> {
            if (DataStore.get().bikes.isEmpty()) { Ui.toast(requireContext(), "Add a bike first"); return; }
            String id = spinnerHelper.selectedId();
            Intent i = new Intent(getContext(), AddEditExpenseActivity.class);
            if (id != null && !BikeSpinnerHelper.ALL.equals(id)) i.putExtra("bike_id", id);
            startActivity(i);
        });
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        DataStore.get().addListener(this);
        spinnerHelper.refresh();
        refresh();
    }

    @Override public void onStop() {
        super.onStop();
        DataStore.get().removeListener(this);
    }

    @Override public void onDataChanged() {
        if (!isAdded()) return;
        spinnerHelper.refresh();
        refresh();
    }

    private void refresh() {
        if (!isAdded() || getView() == null) return;
        DataStore ds = DataStore.get();
        tvMonth.setText(Fmt.month(monthTs));

        String scope = spinnerHelper.selectedId();
        List<String> ids = new ArrayList<>();
        if (scope == null || BikeSpinnerHelper.ALL.equals(scope)) { for (Bike b : ds.bikes) ids.add(b.getId()); }
        else ids.add(scope);

        long from = Fmt.monthStart(monthTs), to = Fmt.nextMonthStart(monthTs);
        long pFrom = Fmt.monthStart(Fmt.addMonths(monthTs, -1)), pTo = from;
        double fuel = 0, add = 0, litres = 0, dist = 0, pFuel = 0, pAdd = 0;
        List<ExpenseRecord> allExp = new ArrayList<>();
        List<ExpenseRecord> monthExp = new ArrayList<>();
        for (String id : ids) {
            fuel += ExpenseMath.fuelCost(ds.fuelFor(id), from, to);
            litres += ExpenseMath.fuelLitres(ds.fuelFor(id), from, to);
            dist += ExpenseMath.distance(ds.fuelFor(id), from, to);
            pFuel += ExpenseMath.fuelCost(ds.fuelFor(id), pFrom, pTo);
            List<ExpenseRecord> el = ds.expensesFor(id);
            add += ExpenseMath.additional(el, from, to);
            pAdd += ExpenseMath.additional(el, pFrom, pTo);
            allExp.addAll(el);
            for (ExpenseRecord e : el) if (e.getTimestamp() >= from && e.getTimestamp() < to) monthExp.add(e);
        }
        double total = fuel + add;          // fuel comes only from fuel records, so nothing is counted twice
        double pTotal = pFuel + pAdd;

        tvFuel.setText(Fmt.money(fuel));
        tvAdd.setText(Fmt.money(add));
        tvTotal.setText(Fmt.money(total));
        tvDistance.setText(Fmt.km(dist));
        tvLitres.setText(Fmt.litres(litres));

        // Category breakdown chart
        Map<String, Double> cats = ExpenseMath.categoryTotals(fuel, allExp, from, to);
        List<String> labels = new ArrayList<>();
        List<Float> vals = new ArrayList<>();
        List<Integer> cols = new ArrayList<>();
        int i = 0;
        for (Double d : cats.values()) {
            labels.add(SHORT[i]); vals.add(d.floatValue()); cols.add(PALETTE[i]); i++;
        }
        chartCategory.setData(labels, vals, cols);

        // Comparison with previous month
        List<String> l2 = new ArrayList<>();
        l2.add(Fmt.month(Fmt.addMonths(monthTs, -1)).split(" ")[0]);
        l2.add(Fmt.month(monthTs).split(" ")[0]);
        List<Float> v2 = new ArrayList<>();
        v2.add((float) pTotal); v2.add((float) total);
        List<Integer> c2 = new ArrayList<>();
        c2.add(Color.parseColor("#9AA5B1")); c2.add(ContextCompat.getColor(requireContext(), R.color.navy));
        chartCompare.setData(l2, v2, c2);

        double diff = total - pTotal;
        if (pTotal <= 0 && total <= 0) {
            tvCompare.setText("No expenses in either month.");
            tvCompare.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else if (pTotal <= 0) {
            tvCompare.setText("No expenses were recorded in the previous month.");
            tvCompare.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            double pct = diff / pTotal * 100.0;
            String word = diff > 0 ? "more" : "less";
            tvCompare.setText(String.format(Locale.US, "%s %s (%.1f%%) than the previous month (%s).",
                    Fmt.money(Math.abs(diff)), word, Math.abs(pct), Fmt.money(pTotal)));
            tvCompare.setTextColor(ContextCompat.getColor(requireContext(), diff > 0 ? R.color.red : R.color.green));
        }

        Collections.sort(monthExp, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapter.submit(monthExp);
        tvNoExp.setVisibility(monthExp.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showExpenseDialog(ExpenseRecord e) {
        Bike b = DataStore.get().bike(e.getBikeId());
        String msg = "Category: " + e.getCategory() + "\nAmount: " + Fmt.money(e.getAmount())
                + "\nDate: " + Fmt.date(e.getTimestamp())
                + "\nBike: " + (b == null ? "-" : b.getName())
                + (e.getNote().isEmpty() ? "" : "\nNote: " + e.getNote());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Expense")
                .setMessage(msg)
                .setPositiveButton("Edit", (d, w) -> startActivity(new Intent(getContext(), AddEditExpenseActivity.class)
                        .putExtra("bike_id", e.getBikeId()).putExtra("expense_id", e.getId())))
                .setNegativeButton("Delete", (d, w) -> new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete this expense?")
                        .setPositiveButton("Delete", (d2, w2) ->
                                DataStore.userRoot().child("expenses").child(e.getBikeId()).child(e.getId()).removeValue()
                                        .addOnSuccessListener(x -> Ui.toast(requireContext(), "Expense deleted")))
                        .setNegativeButton("Cancel", null).show())
                .setNeutralButton("Close", null)
                .show();
    }
}
