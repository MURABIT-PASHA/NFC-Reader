package com.medas.nfc_reader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private NFCManager nfcManager;
    private TextView edit_message;
    private TextView nfc_contents;
    private Button ActivateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edit_message = findViewById(R.id.edit_message);
        nfc_contents = findViewById(R.id.nfc_contents);
        ActivateButton = findViewById(R.id.ActivateButton);

        nfcManager = new NFCManager(this);

        ActivateButton.setOnClickListener(v -> {
            nfcManager.writeNFC(edit_message.getText().toString());
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
