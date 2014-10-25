package com.nightscout.data;

import android.content.Context;

import com.turbomanage.storm.DatabaseHelper;
import com.turbomanage.storm.api.Database;
import com.turbomanage.storm.api.DatabaseFactory;

@Database(name = "DexData", version = 1)
public class DBHelper extends DatabaseHelper {

	public DBHelper(Context ctx, DatabaseFactory dbFactory) {
		super(ctx, dbFactory);

	}

	@Override
	public UpgradeStrategy getUpgradeStrategy() {
		return UpgradeStrategy.DROP_CREATE;
	}

}
