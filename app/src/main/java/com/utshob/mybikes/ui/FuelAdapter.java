package com.utshob.mybikes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.utshob.mybikes.R;
import com.utshob.mybikes.model.FuelRecord;
import com.utshob.mybikes.util.Fmt;

import java.util.ArrayList;
import java.util.List;

public class FuelAdapter extends RecyclerView.Adapter<FuelAdapter.VH> {
    public interface OnClick { void onClick(FuelRecord r); }

    private final List<FuelRecord> items = new ArrayList<>();
    private final OnClick listener;

    public FuelAdapter(OnClick listener) { this.listener = listener; }

    public void submit(List<FuelRecord> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FuelRecord r = items.get(position);
        h.date.setText(Fmt.dateTime(r.getTimestamp()));
        h.tag.setText(r.isFullTank() ? "FULL TANK" : "PARTIAL");
        h.tag.setTextColor(ContextCompat.getColor(h.itemView.getContext(), r.isFullTank() ? R.color.green : R.color.text_secondary));
        h.odo.setText(Fmt.km(r.getOdometer()) + "  ·  " + r.getFuelType());
        h.details.setText(Fmt.litres(r.getLitres()) + " × " + Fmt.money(r.getPricePerLitre()) + " = " + Fmt.money(r.getTotalCost()));
        if (r.hasMileage()) {
            h.mileage.setText("Mileage: " + Fmt.mileage(r.getMileage()) + "  ·  " + Fmt.money(r.getCostPerKm()) + "/km");
            h.mileage.setVisibility(View.VISIBLE);
        } else {
            h.mileage.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView date, tag, odo, details, mileage;

        VH(@NonNull View v) {
            super(v);
            date = v.findViewById(R.id.tvDate);
            tag = v.findViewById(R.id.tvTag);
            odo = v.findViewById(R.id.tvOdo);
            details = v.findViewById(R.id.tvDetails);
            mileage = v.findViewById(R.id.tvMileage);
        }
    }
}
