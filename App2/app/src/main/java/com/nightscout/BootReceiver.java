package com.nightscout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String TAG = BootReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intentX) {
		Log.i(TAG, "Boot setup alarm");

		String alarm = Context.ALARM_SERVICE;
		AlarmManager am = ( AlarmManager ) context.getSystemService( alarm );
		 
		Intent intent = new Intent( "REFRESH_THIS" );
		PendingIntent pi = PendingIntent.getBroadcast( context, 0, intent, 0 );
		 
		int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
		long interval = 150000L;
		long triggerTime = SystemClock.elapsedRealtime() + interval;
		 
		am.setRepeating( type, triggerTime, interval, pi );
		Log.i(TAG, "Boot setup alarm done");
	}
}