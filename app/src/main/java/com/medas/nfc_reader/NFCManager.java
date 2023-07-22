package com.medas.nfc_reader;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class NFCManager {

    public static final Logger LOGGER = Logger.getLogger("MEDAŞ");
    private static final String Error_Detected = "No NFC Tag Detected";
    private static final String Write_Success = "Text Written Successfully";
    private static final String Write_Error = "Error during writing, try again";
    private Context context;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] writingTagFilters;
    private Tag myTag;
    private boolean writeMode;

    public NFCManager(Context context) {
        this.context = context;
        nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            Toast.makeText(context, "This device does not support NFC", Toast.LENGTH_LONG).show();
        }
        pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, context.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[]{tagDetected};
    }

    public void enableForegroundDispatch() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch((AppCompatActivity) context, pendingIntent, writingTagFilters, null);
    }

    public void disableForegroundDispatch() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch((AppCompatActivity) context);
    }

    public void writeNFC(String text) {
        try {
            if (myTag == null) {
                Toast.makeText(context, Error_Detected, Toast.LENGTH_LONG).show();
            } else {
                LOGGER.info("Yazılmaya çalışılıyor");
                writeNdefMessage(createNdefMessage(text));
                Toast.makeText(context, Write_Success, Toast.LENGTH_LONG).show();
            }
        } catch (IOException | FormatException e) {
            Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private NdefMessage createNdefMessage(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return new NdefMessage(new NdefRecord[]{recordNFC});
    }

    private void writeNdefMessage(NdefMessage message) throws IOException, FormatException {
        NdefFormatable ndefFormatable = NdefFormatable.get(myTag);
        if (ndefFormatable != null) {
            ndefFormatable.connect();
            ndefFormatable.format(message);
            ndefFormatable.close();
        } else {
            Ndef ndef = Ndef.get(myTag);
            if (ndef != null) {
                ndef.connect();
                ndef.writeNdefMessage(message);
                ndef.close();
            } else {
                Toast.makeText(context, "NFC Tag is not NDEF formatable.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setTag(Tag tag) {
        this.myTag = tag;
    }

    public void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
        ) {
            LOGGER.info("Founded");
            Toast.makeText(context,"Card Detected", Toast.LENGTH_LONG);
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] messages = null;
            if (rawMessages != null) {
                messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
            }
            buildTagViews(messages);
        } else {
            Toast.makeText(context, "NFC Tag not found.", Toast.LENGTH_LONG).show();
        }
    }

    private void buildTagViews(NdefMessage[] messages) {
        LOGGER.info("buildTagViews");
        if (messages == null || messages.length == 0) return;
        LOGGER.info("Mesaj var");
        String text;
        byte[] payload = messages[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;
        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength, textEncoding);
            LOGGER.info(text);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error while reading NFC content.", Toast.LENGTH_LONG).show();
        }
    }

    public void onNewIntent(Intent intent) {
        setTag(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        readFromIntent(intent);
    }
}
