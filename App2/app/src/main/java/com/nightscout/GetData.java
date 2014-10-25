package com.nightscout;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.nightscout.data.dao.TimeMatechedRecordDao;
import com.turbomanage.storm.TableHelper.Column;



import com.nightscout.data.TimeMatechedRecord;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

public class GetData extends AsyncTask<String, Void, TimeMatechedRecord[]> {

	private static final String TAG = GetData.class.getSimpleName();

	private Throwable err;

	private Context context;
	
	public Throwable error()
	{
		return err;
		
	}
	
	public GetData()
	{
		
	}
	
	public GetData(Context c)
	{
		this.context = c;
	}
	
	@Override
	protected TimeMatechedRecord[] doInBackground(String... arg0) {
		
		return getDataFromMLW();

	}

	public TimeMatechedRecord[] getDataFromMLW() {
		try {
			Log.d(TAG, "Connected");

			TimeMatechedRecordDao dataDao = new TimeMatechedRecordDao(context);
			Column[] columns = dataDao.getTableHelper().getColumns();
			SQLiteDatabase db = dataDao.getDbHelper(context).getReadableDatabase();
			Cursor curs = db.rawQuery("SELECT MAX(_id) from TimeMatechedRecord", null);
			long maxTimeIndex = 0;
			if(curs.moveToFirst()){ 
				maxTimeIndex = curs.getLong(0);
			}
			curs.close();

            TimeMatechedRecord[] matched = null;


            String uri = "mongodb://user:password@server.mongolab.com:53109/datbase_name";
            MongoClientURI mongoURI = new MongoClientURI(uri);
            MongoClient client = new MongoClient(mongoURI);

            // get db
            DB mongoDb = client.getDB(mongoURI.getDatabase());
            String collectionName = "data_col";

            // get collection
            DBCollection coll = mongoDb.getCollection(collectionName.trim());

            BasicDBObject fields = new BasicDBObject()
                    .append("date", 1)
                    .append("_id", 0)
                    .append("sgv", 1);

            BasicDBObject query = new BasicDBObject();

            long lastDate = 0;
            DBCursor cursor = coll.find(query,fields);

            int count = 0;
            try {
                while(cursor.hasNext()) {
                    DBObject row = cursor.next();
                    Long date = (Long) row.get("date")/1000/300;

                    TimeMatechedRecord cor = new TimeMatechedRecord();
                    cor.timeIndex = date;
                    cor.LocalSecondsSinceDexcomEpoch = (Long) row.get("date")/1000;
                    cor.GlucoseValue = Short.parseShort((String) row.get("sgv"));
//                    Log.d(TAG,"got point "+date + " "+cor.GlucoseValue);
                    try {
                        dataDao.insert(cor);
                    } catch (Exception e) {
                        dataDao.update(cor);
                    }
                    count++;
                }
            } finally {
                cursor.close();
            }
            Log.e(TAG,"got data "+count);

			this.err =null;

			return matched;
		
		} catch (Throwable e) {
			Log.e(TAG,"getData "+e.getMessage(),e);
			this.err =e;
			return null;

		}  finally {
		}
	}
}
