package com.utshob.mybikes.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String date(long ts) { return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(ts)); }
    public static String dateTime(long ts) { return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(ts)); }
    public static String month(long ts) { return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date(ts)); }

    public static String money(double v) { return Prefs.currency() + " " + String.format(Locale.US, "%,.2f", v); }
    public static String km(double v) { return String.format(Locale.US, "%,.0f km", v); }
    public static String litres(double v) { return String.format(Locale.US, "%,.2f L", v); }
    public static String mileage(double v) { return String.format(Locale.US, "%.2f km/L", v); }
    public static String plain(double v) {
        if (v == Math.rint(v)) return String.format(Locale.US, "%.0f", v);
        return String.format(Locale.US, "%.2f", v);
    }

    /** Returns NaN when the text is empty or not a number. */
    public static double parse(CharSequence s) {
        if (s == null) return Double.NaN;
        String t = s.toString().trim().replace(",", "");
        if (t.isEmpty()) return Double.NaN;
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return Double.NaN; }
    }

    /** First millisecond of the month that contains ts. */
    public static long monthStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** First millisecond of the month after the one containing ts (exclusive end). */
    public static long nextMonthStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(monthStart(ts));
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    public static long addMonths(long ts, int n) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.add(Calendar.MONTH, n);
        return c.getTimeInMillis();
    }
}
