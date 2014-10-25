package com.nightscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RefreshReceiver  extends BroadcastReceiver{

	private static final String TAG = RefreshReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intentX) {
		Intent intent = new Intent(context,CGMDataRetreiverService.class);
    
		context.startService(intent); 
		Log.i(TAG, "RefreshReceiver started CGMDataRetreiverService");
	}

}
