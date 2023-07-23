package com.medas.nfc_reader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private NFCManager nfcManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button activateButton = findViewById(R.id.ActivateButton);

        nfcManager = new NFCManager(this);

        activateButton.setOnClickListener(v -> {
            byte[] idBytes = {-30, -92, 70, 68};
            nfcManager.writeNFC(idBytes);
        });
    }

    @Override
    protected void onPause() {
        nfcManager.disableForegroundDispatch();
        super.onPause();
    }

    @Override
    protected void onResume() {
        nfcManager.enableForegroundDispatch();
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        nfcManager.onNewIntent(intent);
    }
}
