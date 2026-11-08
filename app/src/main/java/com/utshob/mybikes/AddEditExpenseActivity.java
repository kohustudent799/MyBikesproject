package com.utshob.mybikes;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.ExpenseRecord;
import com.utshob.mybikes.util.Fmt;
import com.utshob.mybikes.util.Ui;

import java.util.ArrayList;
import java.util.List;

/** Add / edit a manual expense. Fuel costs are NOT entered here: they come from fuel records automatically. */
public class AddEditExpenseActivity extends AppCompatActivity {
    private Spinner spBike, spCategory;
    private TextInputEditText etAmount, etNote;
    private MaterialButton btnDate;
    private final List<String> bikeIds = new ArrayList<>();
    private String expenseId, presetBikeId;
    private long timestamp = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);
        DataStore.get().start();
        expenseId = getIntent().getStringExtra("expense_id");
        presetBikeId = getIntent().getStringExtra("bike_id");

        spBike = findViewById(R.id.spBike);
        spCategory = findViewById(R.id.spCategory);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        btnDate = findViewById(R.id.btnDate);
        TextView title = findViewById(R.id.tvTitle);

        spCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConfig.EXPENSE_CATEGORIES));
        btnDate.setText(Fmt.date(timestamp));
        btnDate.setOnClickListener(v -> Ui.pickDate(this, timestamp, d -> {
            // keep the current clock time, change only the day
            java.util.Calendar day = java.util.Calendar.getInstance();
            day.setTimeInMillis(d);
            java.util.Calendar now = java.util.Calendar.getInstance();
            day.set(java.util.Calendar.HOUR_OF_DAY, now.get(java.util.Calendar.HOUR_OF_DAY));
            day.set(java.util.Calendar.MINUTE, now.get(java.util.Calendar.MINUTE));
            timestamp = day.getTimeInMillis();
            btnDate.setText(Fmt.date(timestamp));
        }));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        title.setText(expenseId == null ? "Add expense" : "Edit expense");
        DataStore.get().runWhenLoaded(() -> {
            List<String> names = new ArrayList<>();
            for (Bike b : DataStore.get().bikes) { bikeIds.add(b.getId()); names.add(b.getName()); }
            spBike.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
            String want = presetBikeId != null ? presetBikeId : (DataStore.get().selectedBike() == null ? null : DataStore.get().selectedBike().getId());
            int idx = bikeIds.indexOf(want);
            if (idx >= 0) spBike.setSelection(idx);

            if (expenseId != null) {
                spBike.setEnabled(false);          // an expense stays with its bike
                ExpenseRecord found = null;
                for (ExpenseRecord e : DataStore.get().expensesFor(presetBikeId)) if (expenseId.equals(e.getId())) found = e;
                if (found == null) { finish(); return; }
                timestamp = found.getTimestamp();
                btnDate.setText(Fmt.date(timestamp));
                etAmount.setText(Fmt.plain(found.getAmount()));
                etNote.setText(found.getNote());
                for (int i = 0; i < AppConfig.EXPENSE_CATEGORIES.length; i++) {
                    if (AppConfig.EXPENSE_CATEGORIES[i].equals(found.getCategory())) spCategory.setSelection(i);
                }
            }
        });
    }

    private void save() {
        if (bikeIds.isEmpty() || spBike.getSelectedItemPosition() < 0) { Ui.toast(this, "Add a bike first"); return; }
        double amount = Fmt.parse(etAmount.getText());
        if (Double.isNaN(amount) || amount <= 0) { Ui.err(etAmount, "Amount must be greater than 0"); return; }
        String bikeId = bikeIds.get(spBike.getSelectedItemPosition());

        ExpenseRecord e = new ExpenseRecord();
        e.setBikeId(bikeId);
        e.setTimestamp(timestamp);
        e.setCategory((String) spCategory.getSelectedItem());
        e.setAmount(amount);
        e.setNote(Ui.str(etNote));
        String id = expenseId != null ? expenseId : DataStore.userRoot().child("expenses").child(bikeId).push().getKey();
        DataStore.userRoot().child("expenses").child(bikeId).child(id).setValue(e)
                .addOnFailureListener(x -> Ui.toast(getApplicationContext(), "Save failed: " + x.getMessage()));
        Ui.toast(this, expenseId == null ? "Expense added" : "Expense updated");
        finish();
    }
}
