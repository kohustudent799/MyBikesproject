package com.utshob.mybikes.data;

import com.utshob.mybikes.model.FuelRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Full tank-to-full tank mileage.
 *
 * Rules (see project brief):
 *  1. The first full-tank record is only a baseline (no mileage).
 *  2. Every record after it adds its litres/cost to the current cycle.
 *  3. When the next FULL tank arrives the cycle is closed:
 *        mileage   = (odometer now - baseline odometer) / total litres in the cycle
 *        cost / km = total cost in the cycle / distance
 *     (the litres of the closing full tank ARE included).
 *  4. The closing full tank becomes the new baseline.
 *  Partial records at the end (open cycle) get NO mileage.
 *
 * Mileage is never stored. It is recalculated from the whole list each time the
 * data changes, so editing or deleting an old record automatically fixes everything.
 */
public final class MileageCalculator {
    private MileageCalculator() {}

    public static final Comparator<FuelRecord> BY_TIME = (a, b) -> {
        int c = Long.compare(a.getTimestamp(), b.getTimestamp());
        return c != 0 ? c : Double.compare(a.getOdometer(), b.getOdometer());
    };

    /** Sorts the list oldest to newest and fills mileage / costPerKm on every record. */
    public static void compute(List<FuelRecord> list) {
        Collections.sort(list, BY_TIME);
        boolean haveBaseline = false;
        double baselineOdo = 0, litresInCycle = 0, costInCycle = 0;
        for (FuelRecord r : list) {
            r.setMileage(Double.NaN);
            r.setCostPerKm(Double.NaN);
            if (!haveBaseline) {
                if (r.isFullTank()) {          // first full tank = baseline only
                    haveBaseline = true;
                    baselineOdo = r.getOdometer();
                    litresInCycle = 0;
                    costInCycle = 0;
                }
                continue;
            }
            litresInCycle += r.getLitres();
            costInCycle += r.getTotalCost();
            if (r.isFullTank()) {
                double distance = r.getOdometer() - baselineOdo;
                if (distance > 0 && litresInCycle > 0) {   // prevents division by zero
                    r.setMileage(distance / litresInCycle);
                    r.setCostPerKm(costInCycle / distance);
                }
                baselineOdo = r.getOdometer();             // reset baseline
                litresInCycle = 0;
                costInCycle = 0;
            }
        }
    }

    /** Latest available mileage, or null when no full cycle has been completed yet. */
    public static Double latestMileage(List<FuelRecord> sortedAsc) {
        for (int i = sortedAsc.size() - 1; i >= 0; i--) {
            if (sortedAsc.get(i).hasMileage()) return sortedAsc.get(i).getMileage();
        }
        return null;
    }

    public static double maxOdometer(List<FuelRecord> list) {
        double m = 0;
        for (FuelRecord r : list) m = Math.max(m, r.getOdometer());
        return m;
    }

    /**
     * Validation: a record must not have a lower odometer than the record before it
     * (by date) nor a higher one than the record after it. Returns an error text or null.
     */
    public static String checkOdometer(List<FuelRecord> existing, String editingId, long ts, double odo) {
        List<FuelRecord> others = new ArrayList<>();
        for (FuelRecord r : existing) {
            if (editingId == null || !editingId.equals(r.getId())) others.add(r);
        }
        Collections.sort(others, BY_TIME);
        FuelRecord prev = null, next = null;
        for (FuelRecord r : others) {
            if (r.getTimestamp() <= ts) prev = r;
            else { next = r; break; }
        }
        if (prev != null && odo < prev.getOdometer()) {
            return String.format(Locale.US, "Odometer cannot be lower than the previous record (%,.0f km)", prev.getOdometer());
        }
        if (next != null && odo > next.getOdometer()) {
            return String.format(Locale.US, "Odometer is higher than the next record (%,.0f km). Check the date.", next.getOdometer());
        }
        return null;
    }
}
