package com.utshob.mybikes.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.ExpenseRecord;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.model.Maintenance;
import com.utshob.mybikes.util.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the signed-in user's data in memory and updates it in REAL TIME with Firebase
 * ValueEventListeners. Everything lives under  users/{uid}  so a user can never read
 * another user's data (also enforced by the database security rules).
 *
 *   users/{uid}/bikes/{bikeId}
 *   users/{uid}/fuel/{bikeId}/{fuelId}
 *   users/{uid}/expenses/{bikeId}/{expenseId}
 *   users/{uid}/maintenance/{bikeId}
 *   users/{uid}/fuelPrices/{Petrol|Octane|Diesel}
 */
public class DataStore {
    public interface Listener { void onDataChanged(); }

    private interface Parser { void parse(DataSnapshot s); }

    private static DataStore instance;

    public static synchronized DataStore get() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public final List<Bike> bikes = new ArrayList<>();
    public final Map<String, List<FuelRecord>> fuel = new HashMap<>();
    public final Map<String, List<ExpenseRecord>> expenses = new HashMap<>();
    public final Map<String, Maintenance> maintenance = new HashMap<>();
    public final Map<String, Double> prices = new HashMap<>();

    private final List<Listener> listeners = new ArrayList<>();
    private final List<DatabaseReference> refs = new ArrayList<>();
    private final List<ValueEventListener> valueListeners = new ArrayList<>();
    private final boolean[] firstLoad = new boolean[5];
    private String startedUid;
    private boolean loaded;

    public boolean isLoaded() { return loaded; }

    public static DatabaseReference userRoot() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("Not signed in");
        return FirebaseDatabase.getInstance().getReference("users").child(u.getUid());
    }

    public static String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    /** Runs the task now if data is loaded, otherwise once when the first full load finishes. */
    public void runWhenLoaded(Runnable r) {
        if (loaded) { r.run(); return; }
        addListener(new Listener() {
            @Override public void onDataChanged() {
                if (loaded) { removeListener(this); r.run(); }
            }
        });
    }

    public void start() {
        String uid = uid();
        if (uid == null || uid.equals(startedUid)) return;
        stop();
        startedUid = uid;
        DatabaseReference root = userRoot();

        attach(0, root.child("bikes"), s -> {
            bikes.clear();
            for (DataSnapshot c : s.getChildren()) {
                Bike b = c.getValue(Bike.class);
                if (b != null) { b.setId(c.getKey()); bikes.add(b); }
            }
            Collections.sort(bikes, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        });
        attach(1, root.child("fuel"), s -> {
            fuel.clear();
            for (DataSnapshot bikeNode : s.getChildren()) {
                List<FuelRecord> list = new ArrayList<>();
                for (DataSnapshot c : bikeNode.getChildren()) {
                    FuelRecord r = c.getValue(FuelRecord.class);
                    if (r != null) { r.setId(c.getKey()); r.setBikeId(bikeNode.getKey()); list.add(r); }
                }
                MileageCalculator.compute(list);   // sorts + recalculates mileage for this bike
                fuel.put(bikeNode.getKey(), list);
            }
        });
        attach(2, root.child("expenses"), s -> {
            expenses.clear();
            for (DataSnapshot bikeNode : s.getChildren()) {
                List<ExpenseRecord> list = new ArrayList<>();
                for (DataSnapshot c : bikeNode.getChildren()) {
                    ExpenseRecord e = c.getValue(ExpenseRecord.class);
                    if (e != null) { e.setId(c.getKey()); e.setBikeId(bikeNode.getKey()); list.add(e); }
                }
                Collections.sort(list, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                expenses.put(bikeNode.getKey(), list);
            }
        });
        attach(3, root.child("maintenance"), s -> {
            maintenance.clear();
            for (DataSnapshot c : s.getChildren()) {
                Maintenance m = c.getValue(Maintenance.class);
                if (m != null) maintenance.put(c.getKey(), m);
            }
        });
        attach(4, root.child("fuelPrices"), s -> {
            prices.clear();
            for (DataSnapshot c : s.getChildren()) {
                Double d = c.getValue(Double.class);
                if (d != null) prices.put(c.getKey(), d);
            }
        });
    }

    private void attach(int index, DatabaseReference ref, Parser p) {
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                p.parse(s);
                firstLoad[index] = true;
                boolean all = true;
                for (boolean b : firstLoad) all &= b;
                loaded = all;
                notifyChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("DataStore", "Listener cancelled: " + e.getMessage());
            }
        };
        ref.addValueEventListener(l);
        refs.add(ref);
        valueListeners.add(l);
    }

    /** Stops all listeners and clears memory. Call on logout. */
    public void stop() {
        for (int i = 0; i < refs.size(); i++) refs.get(i).removeEventListener(valueListeners.get(i));
        refs.clear();
        valueListeners.clear();
        bikes.clear(); fuel.clear(); expenses.clear(); maintenance.clear(); prices.clear();
        for (int i = 0; i < firstLoad.length; i++) firstLoad[i] = false;
        loaded = false;
        startedUid = null;
    }

    private void notifyChanged() {
        for (Listener l : new ArrayList<>(listeners)) l.onDataChanged();
    }

    // ---------------- helpers ----------------
    public Bike bike(String id) {
        if (id == null) return null;
        for (Bike b : bikes) if (id.equals(b.getId())) return b;
        return null;
    }

    /** The active bike (saved in SharedPreferences). Falls back to the first bike. */
    public Bike selectedBike() {
        Bike b = bike(Prefs.selectedBike());
        if (b == null && !bikes.isEmpty()) {
            b = bikes.get(0);
            Prefs.setSelectedBike(b.getId());
        }
        return b;
    }

    public List<FuelRecord> fuelFor(String bikeId) {
        List<FuelRecord> l = fuel.get(bikeId);
        return l == null ? new ArrayList<>() : l;
    }

    public List<ExpenseRecord> expensesFor(String bikeId) {
        List<ExpenseRecord> l = expenses.get(bikeId);
        return l == null ? new ArrayList<>() : l;
    }

    public Double price(String type) { return prices.get(type); }
}
