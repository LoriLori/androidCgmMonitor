package com.nightscout;


import android.util.Log;

final class XYSeriesData extends XYSeriesBase  {
	@Override
	public Number getY(int index) {

        double val = (glucoseReadRecords[index].GlucoseValue) / 18d;
        //Log.d("XYSeriesData", "val = " + val + " index=" + index);
        return val;
	}
}