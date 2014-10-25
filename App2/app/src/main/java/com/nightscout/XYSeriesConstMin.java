package com.nightscout;


final class XYSeriesConstMin extends XYSeriesBase {
	@Override
	public String getTitle() {

		return "Min";
	}

	

	@Override
	public Number getY(int index) {
		
		return 5;
	}

	
}