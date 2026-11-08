package com.utshob.mybikes.util;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.Bike;

import java.util.ArrayList;
import java.util.List;

/** Fills a Spinner with the user's bikes (optionally "All bikes") and remembers the choice in SharedPreferences. */
public class BikeSpinnerHelper {
    public static final String ALL = "ALL";

    private final Spinner spinner;
    private final Context ctx;
    private final String prefKey;
    private final boolean includeAll;
    private final Runnable onChanged;
    private final List<String> ids = new ArrayList<>();

    public BikeSpinnerHelper(Spinner spinner, String prefKey, boolean includeAll, Runnable onChanged) {
        this.spinner = spinner;
        this.ctx = spinner.getContext();
        this.prefKey = prefKey;
        this.includeAll = includeAll;
        this.onChanged = onChanged;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos < 0 || pos >= ids.size()) return;
                String chosen = ids.get(pos);
                if (!chosen.equals(current())) {
                    save(chosen);
                    BikeSpinnerHelper.this.onChanged.run();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private String current() {
        if (prefKey.equals("selected_bike")) return Prefs.selectedBike();
        return Prefs.getString(prefKey, includeAll ? ALL : null);
    }

    private void save(String id) {
        if (prefKey.equals("selected_bike")) Prefs.setSelectedBike(id);
        else Prefs.putString(prefKey, id);
    }

    public void refresh() {
        ids.clear();
        List<String> names = new ArrayList<>();
        if (includeAll) { ids.add(ALL); names.add("All bikes"); }
        for (Bike b : DataStore.get().bikes) { ids.add(b.getId()); names.add(b.getName()); }
        spinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, names));
        int idx = ids.indexOf(current());
        if (idx < 0) idx = 0;
        if (!ids.isEmpty()) spinner.setSelection(idx, false);
    }

    /** The id chosen right now (a bike id or ALL), or null when there are no bikes. */
    public String selectedId() {
        int p = spinner.getSelectedItemPosition();
        if (p < 0 || p >= ids.size()) return ids.isEmpty() ? null : ids.get(0);
        return ids.get(p);
    }
}
