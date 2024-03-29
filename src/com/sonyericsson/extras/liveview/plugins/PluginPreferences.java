/*
 * Copyright (c) 2010 Sony Ericsson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonyericsson.extras.liveview.plugins;

import jp.ddo.dekuyou.liveview.plugins.gmail2.Log;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * Implements PreferenceActivity and sets the project preferences to the shared
 * preferences of the current user session.
 */
public class PluginPreferences extends PreferenceActivity {
	AccountManager mAccountManager;
	Account[] accounts;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(getResources().getIdentifier("preferences",
 		"xml", getPackageName()));

		
		PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(
				this);

		mAccountManager = AccountManager.get(this);
		accounts = mAccountManager.getAccountsByType("com.google");

		for (Account account : accounts) {
			String name = account.name;
			String type = account.type;
			int describeContents = account.describeContents();
			int hashCode = account.hashCode();

			Log.d("name = " + name + "\ntype = " + type
					+ "\ndescribeContents = " + describeContents
					+ "\nhashCode = " + hashCode);

			EditTextPreference pf = new EditTextPreference(this);
			pf.setTitle(account.name);
			pf.setKey(account.name+"_");
			pf.setSummary(account.type);

	

			ps.addPreference(pf);

		}
		
		CheckBoxPreference cp = new CheckBoxPreference(this);
		cp.setTitle("Summary Notification");
		cp.setKey("show_summary");
		ps.addPreference(cp);
		
		setPreferenceScreen(ps);

	}
	


}
