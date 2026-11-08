package com.utshob.mybikes;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.utshob.mybikes.util.Ui;

/**
 * BRTA links + helpline. All URLs / the number come from AppConfig.java
 * (they are placeholders until you put the official values there).
 * Uses IMPLICIT INTENTS: ACTION_VIEW (browser) and ACTION_DIAL (phone app).
 */
public class BrtaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brta);

        MaterialButton btnPortal = findViewById(R.id.btnPortal);
        TextView tvHelpline = findViewById(R.id.tvHelpline);
        MaterialButton btnCall = findViewById(R.id.btnCall);
        MaterialButton btnCopy = findViewById(R.id.btnCopy);

        btnPortal.setOnClickListener(v -> openLink(AppConfig.BRTA_SERVICE_PORTAL_URL));

        tvHelpline.setText(AppConfig.isPlaceholder(AppConfig.BRTA_HELPLINE_NUMBER)
                ? "Not set yet (edit AppConfig.java)" : AppConfig.BRTA_HELPLINE_NUMBER);
        btnCall.setOnClickListener(v -> dial());
        btnCopy.setOnClickListener(v -> copyNumber());
    }

    private void openLink(String url) {
        if (AppConfig.isPlaceholder(url)) {
            Ui.toast(this, "This link is not set yet. Put the official URL in AppConfig.java");
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "No browser found on this phone");
        }
    }

    private void dial() {
        if (AppConfig.isPlaceholder(AppConfig.BRTA_HELPLINE_NUMBER)) {
            Ui.toast(this, "Helpline number not set yet. Put the official number in AppConfig.java");
            return;
        }
        try {
            // ACTION_DIAL only opens the dialer; the user presses the call button (no CALL_PHONE permission needed).
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(AppConfig.BRTA_HELPLINE_NUMBER))));
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "No phone app found");
        }
    }

    private void copyNumber() {
        if (AppConfig.isPlaceholder(AppConfig.BRTA_HELPLINE_NUMBER)) {
            Ui.toast(this, "Helpline number not set yet. Put the official number in AppConfig.java");
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("BRTA helpline", AppConfig.BRTA_HELPLINE_NUMBER));
        Ui.toast(this, "Number copied");
    }
}
