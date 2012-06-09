package jp.ddo.dekuyou.liveview.plugins.gmail2;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GmailReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// 
		Log.initialize(context);

		Log.d("Gmail onReceive : " + intent.getAction());

		Bundle extras = intent.getExtras();
		
		
		// 
		intent = new Intent(context, GmailPluginService.class);
		intent.putExtras(extras);
		context.startService(intent);
		
		
		if (extras != null) {

			String extrasStr = "";
			Set<String> set = extras.keySet();
			for (String str : set) {
				Log.d(this.getClass().getPackage().getName(), "extras:" + str
						+ ":" + extras.get(str).toString());
				extrasStr = "extras:" + str + ":" + extras.get(str).toString()
						+ "\n";

			}
			
			Log.d(extrasStr);

//			Intent it = new Intent();
//			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			it.setAction(Intent.ACTION_SENDTO);
//			it.setData(Uri.parse("mailto:" + "android.dev@ucbsweb.ddo.jp"));
//			it.putExtra(Intent.EXTRA_SUBJECT, this.getClass().getPackage()
//					.getName());
//			it.putExtra(Intent.EXTRA_TEXT, intent.getAction()
//					+ "\n-----------\n" + extrasStr);
//			context.startActivity(it);

		}

	}
}
