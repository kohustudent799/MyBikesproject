package com.utshob.mybikes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.utshob.mybikes.R;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.util.Ui;

import java.util.ArrayList;
import java.util.List;

public class BikeAdapter extends RecyclerView.Adapter<BikeAdapter.VH> {
    public interface OnBikeClick { void onClick(Bike bike, boolean alreadySelected); }

    private final List<Bike> items = new ArrayList<>();
    private String selectedId;
    private final OnBikeClick listener;

    public BikeAdapter(OnBikeClick listener) { this.listener = listener; }

    public void submit(List<Bike> bikes, String selectedId) {
        items.clear();
        items.addAll(bikes);
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bike, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Bike b = items.get(position);
        boolean selected = b.getId().equals(selectedId);
        h.name.setText(b.getName());
        h.reg.setText(b.getRegNo().isEmpty() ? b.getBrand() + " " + b.getModel() : b.getRegNo());
        Ui.loadPhoto(h.photo, b.getPhotoUrl());
        h.card.setStrokeWidth(selected ? (int) Ui.dp(h.card.getContext(), 3) : 0);
        h.itemView.setOnClickListener(v -> listener.onClick(b, selected));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView photo;
        final TextView name, reg;

        VH(@NonNull View v) {
            super(v);
            card = (MaterialCardView) v;
            photo = v.findViewById(R.id.ivBikePhoto);
            name = v.findViewById(R.id.tvBikeName);
            reg = v.findViewById(R.id.tvBikeReg);
        }
    }
}
