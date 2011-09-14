package com.bartap;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.*;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.*;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcF;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class Bartap extends Activity {
	
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    AlertDialog.Builder builder;
	// Hex help
	private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1',
			(byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
			(byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B',
			(byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F' };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		mAdapter = NfcAdapter.getDefaultAdapter(this);
		
		// Create a generic PendingIntent that will be deliver to this activity.
		// The NFC stack
		// will fill in the intent with the details of the discovered tag before
		// delivering to
		// this activity.
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		Intent intent = getIntent();
		resolveIntent(intent);
    }
    
	void resolveIntent(Intent intent) {
		// Parse the intent
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
			// status_Data.setText("Discovered tag with intent: " + intent);
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			MifareClassic mfc = MifareClassic.get(tagFromIntent);
			MifareUltralight mfl = MifareUltralight.get(tagFromIntent);
			byte[] data;
			try {
				mfc.connect();
				boolean auth = false;
				String cardData = null;
				// Authenticating and reading Block 0 /Sector 1
				auth = mfc.authenticateSectorWithKeyA(0,
						MifareClassic.KEY_DEFAULT);
				if (auth) {
					data = mfc.readBlock(0);
					cardData = getHexString(data, data.length);
					
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://bartap.herokuapp.com/tags/" + cardData))); 
				}
			} catch (Exception e) {
				try {
					mfl.connect();
					String cardData = null;
					// Authenticating and reading Block 0 /Sector 1
					data = mfl.readPages(1);
					cardData = getHexString(data, data.length);
					
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://bartap.herokuapp.com/tags/" + cardData)));
				} catch (Exception xx) {
					Context context = getApplicationContext();
					CharSequence text = "I couldn't figure out your card doggg";
					int duration = Toast.LENGTH_LONG;

					Toast.makeText(context, text, duration).show();
				}
			}
		} 
	}
	
	public static String getHexString(byte[] raw, int len) {
		byte[] hex = new byte[2 * len];
		int index = 0;
		int pos = 0;

		for (byte b : raw) {
			if (pos >= len)
				break;

			pos++;
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}

		return new String(hex);
	}
    
	@Override
	public void onResume() {
		super.onResume();
		mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
		resolveIntent(intent);
	}

	@Override
	public void onPause() {
		super.onPause();
		mAdapter.disableForegroundDispatch(this);
	}

}