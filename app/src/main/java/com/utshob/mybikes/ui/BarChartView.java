package com.utshob.mybikes.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.utshob.mybikes.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A tiny bar chart drawn with Canvas (no external chart library needed). */
public class BarChartView extends View {
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> labels = new ArrayList<>();
    private final List<Float> values = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();

    public BarChartView(Context c) { this(c, null); }

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        int text = ContextCompat.getColor(c, R.color.text_secondary);
        labelPaint.setColor(text);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(11));
        valuePaint.setColor(ContextCompat.getColor(c, R.color.text_primary));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(11));
        valuePaint.setFakeBoldText(true);
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    public void setData(List<String> l, List<Float> v, List<Integer> c) {
        labels.clear(); values.clear(); colors.clear();
        labels.addAll(l); values.addAll(v); colors.addAll(c);
        invalidate();
    }

    private static String compact(float v) {
        if (v >= 100000) return String.format(Locale.US, "%.0fk", v / 1000);
        if (v >= 10000) return String.format(Locale.US, "%.1fk", v / 1000);
        return String.format(Locale.US, "%.0f", v);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.size();
        float max = 0;
        for (float f : values) max = Math.max(max, f);
        if (n == 0 || max <= 0) {
            canvas.drawText("No data for this month", getWidth() / 2f, getHeight() / 2f, labelPaint);
            return;
        }
        float top = dp(22), bottom = getHeight() - dp(24);
        float slot = getWidth() / (float) n;
        float barW = slot * 0.55f;
        for (int i = 0; i < n; i++) {
            float h = (values.get(i) / max) * (bottom - top);
            float left = i * slot + (slot - barW) / 2f;
            barPaint.setColor(colors.get(i));
            canvas.drawRoundRect(new RectF(left, bottom - h, left + barW, bottom), dp(4), dp(4), barPaint);
            canvas.drawText(compact(values.get(i)), left + barW / 2f, bottom - h - dp(5), valuePaint);
            canvas.drawText(labels.get(i), left + barW / 2f, bottom + dp(15), labelPaint);
        }
        canvas.drawLine(0, bottom, getWidth(), bottom, labelPaint);
    }
}
