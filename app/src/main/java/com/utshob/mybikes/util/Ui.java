package com.utshob.mybikes.util;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.utshob.mybikes.R;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.function.LongConsumer;

public final class Ui {
    private Ui() {}

    public static void toast(Context c, String msg) { Toast.makeText(c, msg, Toast.LENGTH_SHORT).show(); }
    public static void snack(View v, String msg) { Snackbar.make(v, msg, Snackbar.LENGTH_LONG).show(); }

    /** Shows the error on the field (TextInputEditText + setError) and focuses it. */
    public static void err(TextInputEditText et, String msg) {
        et.setError(msg);
        et.requestFocus();
    }

    public static String str(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    public static float dp(Context c, float v) { return v * c.getResources().getDisplayMetrics().density; }

    /** Shows a bike photo (file name saved on the device) or a web URL (Google profile photo). */
    public static void loadPhoto(ImageView iv, String path) {
        boolean isUrl = path != null && path.startsWith("http");
        File local = null;
        if (path != null && !path.isEmpty() && !isUrl) local = ImageUtil.file(iv.getContext(), path);

        if (path == null || path.isEmpty() || (!isUrl && !local.exists())) {
            iv.setImageResource(R.drawable.ic_bike);
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            return;
        }
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(iv).load(isUrl ? path : local).placeholder(R.drawable.ic_bike).into(iv);
    }

    /** Builds an outlined text field in code (used inside dialogs). */
    public static TextInputLayout field(Context c, String hint, int inputType) {
        TextInputLayout til = new TextInputLayout(c, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint(hint);
        TextInputEditText et = new TextInputEditText(til.getContext());
        et.setInputType(inputType);
        et.setSingleLine(true);
        til.addView(et);
        return til;
    }

    public static TextInputEditText edit(TextInputLayout til) { return (TextInputEditText) til.getEditText(); }

    // ---------------- date / time pickers ----------------
    public static long utcToLocalMidnight(long utc) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setTimeInMillis(utc);
        Calendar l = Calendar.getInstance();
        l.clear();
        l.set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH));
        return l.getTimeInMillis();
    }

    public static long localToUtc(long local) {
        Calendar l = Calendar.getInstance();
        l.setTimeInMillis(local);
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH));
        return u.getTimeInMillis();
    }

    public static void pickDate(@NonNull FragmentActivity a, long initial, @NonNull LongConsumer cb) {
        MaterialDatePicker<Long> p = MaterialDatePicker.Builder.datePicker()
                .setSelection(localToUtc(initial)).build();
        p.addOnPositiveButtonClickListener(sel -> cb.accept(utcToLocalMidnight(sel)));
        p.show(a.getSupportFragmentManager(), "date");
    }

    public static void pickDateTime(@NonNull FragmentActivity a, long initial, @NonNull LongConsumer cb) {
        pickDate(a, initial, dayMillis -> {
            Calendar init = Calendar.getInstance();
            init.setTimeInMillis(initial);
            MaterialTimePicker tp = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(init.get(Calendar.HOUR_OF_DAY))
                    .setMinute(init.get(Calendar.MINUTE)).build();
            tp.addOnPositiveButtonClickListener(v -> {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(dayMillis);
                c.set(Calendar.HOUR_OF_DAY, tp.getHour());
                c.set(Calendar.MINUTE, tp.getMinute());
                cb.accept(c.getTimeInMillis());
            });
            tp.show(a.getSupportFragmentManager(), "time");
        });
    }
}
