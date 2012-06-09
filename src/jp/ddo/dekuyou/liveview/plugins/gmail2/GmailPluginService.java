package jp.ddo.dekuyou.liveview.plugins.gmail2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Xml;
import android.widget.Toast;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;

public class GmailPluginService extends AbstractPluginService {

	// Our handler.
	private Handler mHandler = null;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.initialize(this);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

		Log.d(this.getClass().getPackage().getName(),
				"GmailPluginService onStart");

		if (intent == null) {
			Log.d("intent is null.");
			return;
		}

		Bundle extras = intent.getExtras();

		if (extras == null) {
			Log.d("extras is null.");
			return;
		}else {

			
			if ("^^unseen-^i".equals(extras.getString("tagLabel"))){
				Editor e = mSharedPreferences.edit();
				e.putBoolean("tagLabel", true);
				e.commit();
				Log.d("tagLabel ^i isTrue ");

			}
			String extrasStr = "";
			Set<String> set = extras.keySet();
			for (String str : set) {
				Log.d( "extras:" + str
						+ ":" + extras.get(str).toString());
				extrasStr = "extras:" + str + ":" + extras.get(str).toString()
						+ "\n";

			}
			
			Log.d(extrasStr);


		}

		if (mSharedPreferences.getBoolean("tagLabel", false)
				&& "^^unseen-^iim".equals(extras.getString("tagLabel"))) {
			Log.d("tagLabel ^iim && ^i isTrue ");
			return; // 
		}

		// count の数から新着判定
		account = extras.getString("account");
		unreadcount = extras.getInt("count");

		SharedPreferences pref = getSharedPreferences(account, 0);
		int prev = pref.getInt("count", 0);
		Editor e = pref.edit();
		e.putInt("count", unreadcount);
		e.commit();

		if (prev < extras.getInt("count")) {

			String t = mSharedPreferences.getString(account + "_", "");

			if ("".equals(t)) {
				sendAnnounce(account, "Gmail Received.\nunread:" + unreadcount);
			} else {

				getFeed(t, unreadcount - prev);
			}

		}

	}

	private static final int READ_TIMEOUT = 15 * 1000;

	private static final int CONNECT_TIMEOUT = 10 * 1000;

	private void getFeed(String t, int count) {

		String res = accessFeed(t);

		List<GmailFeed> gfList = gmailFeed2List(res);

		Log.d("count :" + count);
		if (gfList.size() > 0) {
			int j = (count > gfList.size() ? gfList.size() : count);

			for (int i = j - 1; i >= 0; i--) {
				GmailFeed gfs = gfList.get(i);

				StringBuilder sbs = new StringBuilder();
				sbs.append((gfs.getName() == null ? "" : gfs.getName()));
				sbs.append((gfs.getEmail() == null ? "" : "<" + gfs.getEmail()
						+ ">"));

				StringBuilder sbb = new StringBuilder();
				sbb.append((gfs.getTitle() == null ? "" : "<"
						+ getString(R.string.subject) + ":" + gfs.getTitle()
						+ ">\n"));

				if (mSharedPreferences.getBoolean("show_summary", false)) {
					sbb.append((gfs.getSummary() == null ? "" : gfs
							.getSummary()));
				}
				sbb.append("\nGmail Received." + account);
				sbb.append("\nunread:" + unreadcount);

				sendAnnounce(sbs.toString(), sbb.toString());
			}
		} else {
			sendAnnounce(account, "Gmail Received.\nunread:" + unreadcount);
		}

	}

	private List<GmailFeed> gmailFeed2List(String res) {
		List<GmailFeed> gfList = new ArrayList<GmailFeed>();
		GmailFeed gf = null;
		String pnameB = "";
		XmlPullParser xmlpp = Xml.newPullParser();
		try {
			xmlpp.setInput(new StringReader(res));
			while (xmlpp.next() != XmlPullParser.END_DOCUMENT) {
				Log.d("depth:" + xmlpp.getDepth() + ", eventType:"
						+ xmlpp.getEventType() + ", name:" + xmlpp.getName()
						+ ", text:" + xmlpp.getText());

				if (xmlpp.getEventType() == 4 && "fullcount".equals(pnameB)) {
					// 未読数の更新
					SharedPreferences pref = getSharedPreferences(account, 0);
					Editor e = pref.edit();
					unreadcount = Integer.parseInt(xmlpp.getText());
					e.putInt("count", unreadcount);
					e.commit();

				}

				if ("entry".equals(xmlpp.getName())) {
					if (xmlpp.getEventType() == 2) {
						gf = new GmailFeed();
					} else if (xmlpp.getEventType() == 3) {
						gfList.add(gf);
					}
				}
				if (xmlpp.getEventType() == 2) {
					pnameB = xmlpp.getName();
				}
				if (xmlpp.getEventType() == 4 && gf != null) {
					char lead = pnameB.charAt(0);
					String pname = Character.toUpperCase(lead)
							+ pnameB.substring(1);

					Method method;
					try {
						method = gf.getClass().getMethod("set" + pname,
								new Class[] { String.class });
					} catch (SecurityException e) {
						throw new RuntimeException(e);
					} catch (NoSuchMethodException e) {
						throw new RuntimeException(e);
					}

					try {
						method.invoke(gf, new Object[] { xmlpp.getText() });

					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}

				}

			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return gfList;
	}

	private String accessFeed(String t) {
		//
		HttpURLConnection c = null;
		String res = "";
		try {
			Authenticator.setDefault(new BasicAuthenticator(account, t));
			// HTTP https://mail.google.com/mail/feed/atom
			URL url = new URL("https://mail.google.com/mail/feed/atom");

			c = (HttpURLConnection) url.openConnection();
			c.setRequestMethod("GET");
			// c.setRequestProperty("Content-type", "application/atom+xml");
			c.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
			// c.setRequestProperty("Authorization", "AuthSub token="+t);
			c.setInstanceFollowRedirects(true);
			c.setConnectTimeout(CONNECT_TIMEOUT);
			c.setReadTimeout(READ_TIMEOUT);
			Log.d("connect ：" + c.getURL());
			// c.setDoOutput(true);
			c.connect();

			Log.d(String.valueOf(c.getResponseCode()));
			Log.d(convertStreamToString(c.getErrorStream()));

			res = convertStreamToString(c.getInputStream());
			Log.d(res);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (c != null)
					c.disconnect();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		} finally {
			c.disconnect();

		}
		return res;
	}

	public String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		if (is != null) {

			StringBuilder sb = new StringBuilder();

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));

				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	private String account;
	private int unreadcount;

	@Override
	public void onCreate() {
		super.onCreate();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopWork();
	}

	boolean sandbox = false;

	/**
	 * Plugin is sandbox.
	 */
	protected boolean isSandboxPlugin() {
		return sandbox;
	}

	private void sendAnnounce(String subject, String body) {
		if (mLiveViewAdapter == null) {
			return;
		}

		try {
			Log.d(mLiveViewAdapter.toString());
			Log.d(String.valueOf(mPluginId));
			Log.d(mMenuIcon.toString());

			mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, subject, body,
					System.currentTimeMillis(), "");
			Log.d("Announce sent to LiveView");

		} catch (Exception e) {
			Log.e(e);
		}
	}

	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

	}

	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView
	 * Service.
	 * 
	 * If needed, do additional actions here, e.g. starting any worker that is
	 * needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className,
			IBinder service) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been
	 * stopped.
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed.
	 */
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs,
			String key) {

		Log.d("onSharedPreferenceChangedExtended:" + key);

		if ("show_summary".equals(key)) {
			return;
		}

		account = key.substring(0, key.lastIndexOf("_"));

		t = prefs.getString(key, "");
		// String t = mSharedPreferences.getString(key, "");
		Log.d("key:" + t);

		if (!"".equals(t)) {

			runnable = new Runnable() {
				public void run() {
					showToast(accessFeed(t));

				}
			};
			
			mHandler.post(runnable);
			

		}
	}

	String t = "";
	private Runnable runnable;

			

	private void showToast(String res) {
		
		mHandler.removeCallbacks(runnable);

		if ("".equals(res)) {
			Toast.makeText(this, account + ". Fail!", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, account + ". SUCCESS!", Toast.LENGTH_LONG)
					.show();
		}
	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");

		sandbox = false;
		startWork();
	}

	protected void stopPlugin() {
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");

		sandbox = false;

		stopWork();
	}

	protected void button(String buttonType, boolean doublepress,
			boolean longpress) {
		Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType
				+ ", doublepress " + doublepress + ", longpress " + longpress);

		if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {

		}

	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
		Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx
				+ ", height " + displayHeigthPx);
	}

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);
		
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClassName("com.google.android.gm","com.google.android.gm.ConversationListActivityGmail");
		startActivity(intent);

	}

	protected void screenMode(int mode) {
		Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now "
				+ ((mode == 0) ? "OFF" : "ON"));

		if (mode == PluginConstants.LIVE_SCREEN_MODE_ON) {
		} else {
		}
	}

}

class BasicAuthenticator extends Authenticator {

	private String username;
	private String password;
	private int count = 0;

	public BasicAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		Log.d("BasicAuthenticator : " + count++);
		if(count > 1){
			username = "";
		}
		return new PasswordAuthentication(username, password.toCharArray());
	}

}