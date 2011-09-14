package com.bartap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class WebViewActivity extends Activity {
	WebView mWebView;
	Button backToMainButton;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.web);
	    
	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
	    Bundle extras = getIntent().getExtras();
	    mWebView.loadUrl(extras.getString("URL"));
	    
	    mWebView.setWebViewClient(new HelloWebViewClient());
	    
	    backToMainButton = (Button)findViewById(R.id.back_to_main);
	    backToMainButton.getBackground().setColorFilter(Color.CYAN, PorterDuff.Mode.MULTIPLY);
	    
	    backToMainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent myIntent = new Intent(v.getContext(), Bartap.class);
                startActivityForResult(myIntent, 0);
            }
        });
	    
	}
	    
	    @Override
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	            mWebView.goBack();
	            return true;
	        }
	        return super.onKeyDown(keyCode, event);
	    }
	    
	    private class HelloWebViewClient extends WebViewClient {
	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            view.loadUrl(url);
	            return true;
	        }
	    }
	    
}