package com.nightscout;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;

import com.nightscout.events.WidgetData;

public class DexAppWidgetProvider extends AppWidgetProvider {

	private static final String TAG = DexAppWidgetProvider.class.getSimpleName();
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        MainApp.bus().post(new WidgetData());
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);
		Log.i(TAG, "onDisabled ");
        MainApp.bus().post(new WidgetData());
	}

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
        //MainApp.bus().register(this);
		Log.i(TAG, "onEnabled ");
        MainApp.bus().post(new WidgetData());
	}
}