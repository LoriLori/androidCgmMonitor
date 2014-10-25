package com.nightscout;

import com.androidplot.xy.XYSeries;

import com.nightscout.data.TimeMatechedRecord;

public abstract class XYSeriesBase implements XYSeries{

	public TimeMatechedRecord[] glucoseReadRecords;

	public XYSeriesBase() {
		super();
	}

	@Override
	public String getTitle() {
	
		return "Data";
	}

	@Override
	public int size() {
		int size = 0;
		if(glucoseReadRecords!=null)
		size = glucoseReadRecords.length;
		return size;
	}

	@Override
	public Number getX(int index) {
	
		return glucoseReadRecords[index].timeIndex*300;
	}

}