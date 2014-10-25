package com.nightscout;


final class XYSeriesConstMax extends XYSeriesBase  {
	@Override
	public String getTitle() {

		return "Max";
	}

	@Override
	public Number getY(int index) {
		
		return 8;
	}
}