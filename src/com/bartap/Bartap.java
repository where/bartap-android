package com.bartap;

import java.io.IOException;

//import android.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Charsets;

public class Bartap extends Activity {
	Button scanButton;
	
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private TextView card_id;
    private TextView prompt;
    private boolean tapped;
    private String cardData;
    byte[] data = new byte[0];
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
		prompt = (TextView) findViewById(R.id.prompt);
		card_id = (TextView) findViewById(R.id.card_id);
		tapped = false;
		
		scanButton = (Button)findViewById(R.id.scan);
		scanButton.setText("Scanning for Coaster");
		scanButton.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);

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
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if (!tapped) {
				try {
					readMifareClassic(tagFromIntent);
				} catch (Exception e) {
					try {
						readMifareUltralight(tagFromIntent);
					} catch (Exception xx) {
						prompt.setText("Scan a MiFare Classic");
					}
				}
			} else {
				try {
					writeMifareClassic(cardData, tagFromIntent);
				} catch (Exception e) {
					Tag tagTwoFuckYou = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
					new Thread(new WriteRunner(cardData, tagTwoFuckYou)).run();
				}
			}
		} 
	}
	
	void readMifareUltralight(Tag tagFromIntent) throws IOException {
		MifareUltralight mfl = MifareUltralight.get(tagFromIntent);
		mfl.connect();
		data = mfl.readPages(1);
		cardData = "http://bartapapp.com/tags/" + getHexString(data, data.length);
		tapped = true;
		prompt.setText("Tap again to save URL!"); 
	}
	
	void writeMifareUltralight(String cardData, Tag tagFromIntent) throws IOException {
		NdefFormatable tag = NdefFormatable.get(tagFromIntent);
		final byte[] textBytes = cardData.getBytes(Charsets.UTF_8);
		NdefRecord record = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI, new byte[0], textBytes);
	
	    NdefRecord[] records = {record};
	    NdefMessage message = new NdefMessage(records);
	    tag.connect();
	    try {
			tag.format(message);
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    prompt.setText("SUCCESS!!!");
	    
	}
	
	void readMifareClassic(Tag tagFromIntent) throws IOException {
		MifareClassic mfc = MifareClassic.get(tagFromIntent);
		mfc.connect();
		boolean auth = false;
		// Authenticating and reading Block 0 /Sector 1
		
		auth = mfc.authenticateSectorWithKeyA(0,
				MifareClassic.KEY_DEFAULT);
		if (auth) {
			data = mfc.readBlock(0);
			cardData = "http://bartapapp.com/tags/" + getHexString(data, data.length);
			tapped = true;
			prompt.setText("Tap again to save URL!");
			
			scanButton.setText("Waiting to Write");
			scanButton.getBackground().setColorFilter(Color.MAGENTA, PorterDuff.Mode.MULTIPLY);
			
		} else {
			prompt.setText("Miss, Scan again to save URL!");
		}
	}
	
	void writeMifareClassic(String cardData, Tag tagFromIntent) throws Exception {
		NdefFormatable tag = NdefFormatable.get(tagFromIntent);
		final byte[] textBytes = cardData.getBytes(Charsets.UTF_8);
		NdefRecord record = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, NdefRecord.RTD_URI, data, textBytes);
	
	    NdefRecord[] records = {record};
	    NdefMessage message = new NdefMessage(records);
	    try {
			tag.connect();
		    tag.format(message);
	    } catch (Exception e) {
	    	tag.close();
	    	throw e;
	    }
	    
	    prompt.setText("SUCCESS!!!");
	    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cardData));
        startActivityForResult(myIntent, 0);
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

	public class WriteRunner implements Runnable {
		private String cardData;
		private Tag tagFromIntent;
		
		public WriteRunner(String mCardData, Tag mTagFromIntent) {
			cardData = mCardData;
			tagFromIntent = mTagFromIntent;
		}
		
		public void run() {
			try {
				writeMifareUltralight(cardData, tagFromIntent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}