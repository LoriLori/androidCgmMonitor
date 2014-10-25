package com.nightscout;

import com.nightscout.data.TimeMatechedRecord;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.nightscout.events.RefreshData;
import com.nightscout.events.RefreshError;
import com.nightscout.events.RefreshingData;
import com.nightscout.events.RequestRefreshData;
import com.squareup.otto.Subscribe;

public class CGMDataRetreiverService extends Service {
	private static final String TAG = CGMDataRetreiverService.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		
		while(mediaPlayer.isPlaying()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		mediaPlayer.reset();
		mediaPlayer.release();
		mediaPlayer = null;

	}

	MediaPlayer mediaPlayer ;
	
	@Override
	public int onStartCommand(Intent intentX, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
		
		mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.little_bell);

        refreshData();

		Log.i(TAG, "onStartCommand end");
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    @Subscribe
    private void onRequestRefreshData(RequestRefreshData event) {
        Log.d(TAG," onRequestRefreshData ");
        refreshData();
    }

	private void refreshData() {

		GetData getData = new GetData(getApplicationContext());
        MainApp.bus().post(new RefreshingData());
		AsyncTask<String, Void, TimeMatechedRecord[]> ret = getData.execute("");
		try {
			TimeMatechedRecord[] glucoseReadRecords = ret.get();

            MainApp.bus().post(new RefreshData());

		} catch (Throwable e) {
            RefreshError refreshError = new RefreshError();
            refreshError.mMessage = e.getMessage();
            MainApp.bus().post(refreshError);
			Log.d(TAG,"getDataFromMLW error ",e);
		}

		stopSelf();
	}
}