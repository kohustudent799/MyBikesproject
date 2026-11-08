package com.utshob.mybikes.data;

import com.utshob.mybikes.AppConfig;
import com.utshob.mybikes.model.ExpenseRecord;
import com.utshob.mybikes.model.FuelRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monthly expense maths. A month is the range [from, to) in milliseconds.
 * Fuel expense comes ONLY from fuel records; manual expenses never use the "Fuel"
 * category, so nothing is counted twice.
 */
public final class ExpenseMath {
    private ExpenseMath() {}

    public static double fuelCost(List<FuelRecord> list, long from, long to) {
        double s = 0;
        for (FuelRecord r : list) if (r.getTimestamp() >= from && r.getTimestamp() < to) s += r.getTotalCost();
        return s;
    }

    public static double fuelLitres(List<FuelRecord> list, long from, long to) {
        double s = 0;
        for (FuelRecord r : list) if (r.getTimestamp() >= from && r.getTimestamp() < to) s += r.getLitres();
        return s;
    }

    public static double additional(List<ExpenseRecord> list, long from, long to) {
        double s = 0;
        for (ExpenseRecord e : list) if (e.getTimestamp() >= from && e.getTimestamp() < to) s += e.getAmount();
        return s;
    }

    /**
     * Monthly distance = latest odometer in the month - first odometer in the month.
     * Odometer readings come from the fuel records of that month (list must be sorted
     * oldest to newest). Fewer than two readings means the distance cannot be known (0).
     */
    public static double distance(List<FuelRecord> sortedAsc, long from, long to) {
        FuelRecord first = null, last = null;
        for (FuelRecord r : sortedAsc) {
            if (r.getTimestamp() >= from && r.getTimestamp() < to) {
                if (first == null) first = r;
                last = r;
            }
        }
        if (first == null || first == last) return 0;
        return Math.max(0, last.getOdometer() - first.getOdometer());
    }

    /** Category totals for the month: Fuel first, then the manual categories. */
    public static Map<String, Double> categoryTotals(double fuel, List<ExpenseRecord> expenses, long from, long to) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(AppConfig.CATEGORY_FUEL, fuel);
        for (String c : AppConfig.EXPENSE_CATEGORIES) m.put(c, 0.0);
        for (ExpenseRecord e : expenses) {
            if (e.getTimestamp() >= from && e.getTimestamp() < to) {
                String c = m.containsKey(e.getCategory()) ? e.getCategory() : "Other";
                m.put(c, m.get(c) + e.getAmount());
            }
        }
        return m;
    }
}
