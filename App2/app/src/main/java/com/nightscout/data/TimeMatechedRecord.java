package com.nightscout.data;

import com.turbomanage.storm.api.Entity;
import com.turbomanage.storm.api.Id;

@Entity
public class TimeMatechedRecord {
	
	public long LocalSecondsSinceDexcomEpoch;
	public short GlucoseValue;

    public long getTimeIndex() {
        return timeIndex;
    }

    public void setTimeIndex(long timeIndex) {
        this.timeIndex = timeIndex;
    }

    public short getGlucoseValue() {
        return GlucoseValue;
    }

    public void setGlucoseValue(short glucoseValue) {
        GlucoseValue = glucoseValue;
    }

    public long getLocalSecondsSinceDexcomEpoch() {
        return LocalSecondsSinceDexcomEpoch;
    }

    public void setLocalSecondsSinceDexcomEpoch(long localSecondsSinceDexcomEpoch) {
        LocalSecondsSinceDexcomEpoch = localSecondsSinceDexcomEpoch;
    }

    @Id public long timeIndex;
	
	public TimeMatechedRecord() {
		
	}
}
