package com.utshob.mybikes.util;

import android.content.Context;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.utshob.mybikes.AddEditFuelActivity;
import com.utshob.mybikes.data.DataStore;
import com.utshob.mybikes.model.FuelRecord;

/** Shared dialog for viewing / editing / deleting one fuel record. */
public final class FuelUi {
    private FuelUi() {}

    public static void showDetails(Context ctx, FuelRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(Fmt.dateTime(r.getTimestamp())).append('\n');
        sb.append("Odometer: ").append(Fmt.km(r.getOdometer())).append('\n');
        sb.append("Fuel type: ").append(r.getFuelType()).append('\n');
        sb.append("Quantity: ").append(Fmt.litres(r.getLitres())).append('\n');
        sb.append("Price per litre: ").append(Fmt.money(r.getPricePerLitre())).append('\n');
        sb.append("Total cost: ").append(Fmt.money(r.getTotalCost())).append('\n');
        sb.append("Refill: ").append(r.isFullTank() ? "Full tank" : "Partial").append('\n');
        if (r.hasMileage()) {
            sb.append("Mileage: ").append(Fmt.mileage(r.getMileage())).append('\n');
            sb.append("Cost per km: ").append(Fmt.money(r.getCostPerKm()));
        } else {
            sb.append("Mileage: not available (needs a completed full-tank cycle)");
        }
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Fuel record")
                .setMessage(sb.toString())
                .setPositiveButton("Edit", (d, w) -> ctx.startActivity(new Intent(ctx, AddEditFuelActivity.class)
                        .putExtra("bike_id", r.getBikeId())
                        .putExtra("fuel_id", r.getId())))
                .setNegativeButton("Delete", (d, w) -> confirmDelete(ctx, r))
                .setNeutralButton("Close", null)
                .show();
    }

    private static void confirmDelete(Context ctx, FuelRecord r) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Delete this fuel record?")
                .setMessage("Mileage of the affected records will be recalculated automatically.")
                .setPositiveButton("Delete", (d, w) ->
                        DataStore.userRoot().child("fuel").child(r.getBikeId()).child(r.getId()).removeValue()
                                .addOnSuccessListener(v -> Ui.toast(ctx, "Fuel record deleted"))
                                .addOnFailureListener(e -> Ui.toast(ctx, "Delete failed: " + e.getMessage())))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
