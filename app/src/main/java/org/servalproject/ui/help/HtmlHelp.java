/*
 * SATNET maintenance note:
 * This file is maintained as part of SATNET and builds on historical upstream work.
 * Copyright (C) 2012 The Serval Project.
 * Licensed under GPL-3.0-or-later; see LICENSE-SOFTWARE.md.
 */
package org.servalproject.ui.help;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.servalproject.BuildConfig;
import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;

/**
 * Help screens - guide to using SATNET.
 *
 * Original authorship retained from the upstream project.
 */

public class HtmlHelp extends Activity {
	private WebView helpBrowser;
	private String startPage;
	int viewId = R.layout.htmlhelp;
	static final String assetPrefix = "file:///android_asset/";
	public class Client extends WebViewClient {

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			ServalBatPhoneApplication.context.displayToastMessage(description);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith(assetPrefix) || url.equals("about:blank"))
				return false;

			// Load the uri using the full internet browser app.
			Uri uri = Uri.parse(url);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
			return true;
		}
	}

	class AppInfo {
		public String getVersion() {
			return HtmlHelp.this.getString(R.string.version);
		}

		public String getSupportUrl() {
			return BuildConfig.SATNET_SUPPORT_URL;
		}

		public String getDonationBitcoinAddress() {
			return BuildConfig.SATNET_DONATION_BITCOIN_ADDRESS;
		}

		public String getDonationBitcoinUri() {
			return BuildConfig.SATNET_DONATION_BITCOIN_URI;
		}
	}

	/** Called when the activity is first created. */
	// Since we're only loading our own assets from the asset folder in this
	// view, there shouldn't be any security issues.
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(viewId);
		helpBrowser = (WebView) findViewById(R.id.help_browser);
		helpBrowser.getSettings().setJavaScriptEnabled(true);
		helpBrowser.addJavascriptInterface(new AppInfo(), "appinfo");
		helpBrowser.setWebViewClient(new Client());
		helpBrowser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		helpBrowser.setBackgroundColor(Color.BLACK);
		helpBrowser.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = this.getIntent();
		startPage = assetPrefix + intent.getStringExtra("page");
		helpBrowser.clearHistory();
		helpBrowser.loadUrl(startPage);
	}

	@Override
	public void onBackPressed() {
		WebBackForwardList history = helpBrowser.copyBackForwardList();
		int index = history.getCurrentIndex();
		for (int offset = -1; index + offset >= 0; offset--) {
			if (!history.getItemAtIndex(index + offset).getUrl()
					.equals("about:blank")) {
				helpBrowser.goBackOrForward(offset);
				return;
			}
		}
		super.onBackPressed();
	}

}
