package com.utshob.mybikes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.utshob.mybikes.R;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.Bike;
import com.utshob.mybikes.model.ExpenseRecord;
import com.utshob.mybikes.util.Fmt;

import java.util.ArrayList;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {
    public interface OnClick { void onClick(ExpenseRecord e); }

    private final List<ExpenseRecord> items = new ArrayList<>();
    private final OnClick listener;

    public ExpenseAdapter(OnClick listener) { this.listener = listener; }

    public void submit(List<ExpenseRecord> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ExpenseRecord e = items.get(position);
        Bike b = DataStore.get().bike(e.getBikeId());
        h.category.setText(e.getCategory());
        String sub = Fmt.date(e.getTimestamp()) + (b != null ? "  ·  " + b.getName() : "");
        if (e.getNote() != null && !e.getNote().isEmpty()) sub += "\n" + e.getNote();
        h.sub.setText(sub);
        h.amount.setText(Fmt.money(e.getAmount()));
        h.itemView.setOnClickListener(v -> listener.onClick(e));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView category, sub, amount;

        VH(@NonNull View v) {
            super(v);
            category = v.findViewById(R.id.tvCategory);
            sub = v.findViewById(R.id.tvSub);
            amount = v.findViewById(R.id.tvAmount);
        }
    }
}
