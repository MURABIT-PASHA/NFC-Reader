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
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * The NFCManager class handles NFC interactions for the NFC Reader application.
 * It provides methods to read and write data to NFC tags, and handles NFC-related events.
 */
public class NFCManager {

    public static final Logger LOGGER = Logger.getLogger("MEDAŞ");
    private static final String Error_Detected = "No NFC Tag Detected";
    private static final String Write_Success = "Text Written Successfully";
    private static final String Write_Error = "Error during writing, try again";
    private final Context context;
    private final NfcAdapter nfcAdapter;
    private final PendingIntent pendingIntent;
    private final IntentFilter[] writingTagFilters;
    private Tag myTag;

    /**
     * Constructor to create a new instance of NFCManager.
     *
     * @param context The context of the calling activity or application.
     */
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

    /**
     * Enable foreground dispatch to handle NFC events.
     */
    public void enableForegroundDispatch() {
        nfcAdapter.enableForegroundDispatch((AppCompatActivity) context, pendingIntent, writingTagFilters, null);
    }

    /**
     * Disable foreground dispatch for NFC events.
     */
    public void disableForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch((AppCompatActivity) context);
    }

    /**
     * Write data to the NFC tag using NdefMessage format.
     *
     * @param tagId The byte array representing the data to be written.
     */
    public void writeNFC(byte[] tagId) {
        try {
            if (myTag == null) {
                Toast.makeText(context, Error_Detected, Toast.LENGTH_LONG).show();
            } else {
                LOGGER.info("Trying to write");
                writeNdefMessage(createNdefMessage(bytesToHexString(tagId)));
                Toast.makeText(context, Write_Success, Toast.LENGTH_LONG).show();
            }
        } catch (IOException | FormatException e) {
            Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Create an NdefMessage object from the given text data.
     *
     * @param text The text data to be included in the NdefMessage.
     * @return The constructed NdefMessage.
     * @throws UnsupportedEncodingException If the encoding is not supported.
     */
    private NdefMessage createNdefMessage(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] langBytes = lang.getBytes(StandardCharsets.US_ASCII);
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return new NdefMessage(new NdefRecord[]{recordNFC});
    }

    /**
     * Write an NdefMessage to the NFC tag using Ndef or NdefFormatable technologies.
     *
     * @param message The NdefMessage to be written.
     * @throws IOException   If there is an I/O error during the write process.
     * @throws FormatException If the NFC tag is not in NDEF format.
     */
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
                Toast.makeText(context, "NFC Tag is not NDEF format-able.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Set the Tag object representing the detected NFC tag.
     *
     * @param tag The Tag object representing the NFC tag.
     */
    public void setTag(Tag tag) {
        this.myTag = tag;
    }

    /**
     * Handle NFC intent and read data from the NFC tag.
     *
     * @param intent The intent containing NFC-related data.
     */
    public void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
        ) {
            LOGGER.info("Founded");
            Toast.makeText(context,"Card Detected", Toast.LENGTH_LONG).show();
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                LOGGER.info("It's not a card");
            }else{
                LOGGER.info("Possible card");
                setTag(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
                LOGGER.info(Arrays.toString(myTag.getTechList()));
                LOGGER.info(String.valueOf(myTag.describeContents()));
                LOGGER.info(Arrays.toString(myTag.getId()));
            }
        } else {
            Toast.makeText(context, "NFC Tag not found.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle new NFC intent and read data from the NFC tag.
     *
     * @param intent The new NFC intent containing NFC data.
     */
    public void onNewIntent(Intent intent) {
        readFromIntent(intent);
    }

    /**
     * Convert a byte array to a hexadecimal string representation.
     *
     * @param bytes The byte array to be converted.
     * @return The hexadecimal string representation of the byte array.
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
