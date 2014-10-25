package com.nightscout;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.nightscout.data.dao.TimeMatechedRecordDao;
import com.nightscout.events.BTStatusEvent;
import com.nightscout.events.RefreshData;
import com.nightscout.events.RefreshError;
import com.nightscout.events.RefreshingData;
import com.nightscout.events.WidgetData;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;


import com.nightscout.data.TimeMatechedRecord;

public class MainActivity extends Activity implements View.OnTouchListener {

    private long rangeStart;

    @Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
        isDisplayed = true;
        updateUI();
        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
        mHandlerWidget.sendMessageDelayed(Message.obtain(mHandlerWidget, TICK_WHAT), mFrequencyWidget);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop");
        isDisplayed = false;
	}

	static final String TAG = MainActivity.class.getSimpleName();
	static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
	int time = 20;
	//Timer timer;
	TimerTask task;

	int range = 300*40;

    boolean isDisplayed = false;
    boolean isBTconnected = false;

	XYPlot plot;
	XYSeriesData series1 ;

	XYSeriesConstMin seriesMin ;
	XYSeriesConstMax seriesMax ;

	TextView tv1 ;
	TextView tv2 ;
	TextView tv3 ;
	TextView status ;
	TextView tvInRange ;
	

    ProgressBar progressBar;
	
	Handler handler;
	
    private Button button;

	private Button button6;

	private Button button12;
	
	private Button button24;

	private Button buttonRefresh;

    private long m_lastPacketTime;

    private final long mFrequency = 100;    // milliseconds
    private final int TICK_WHAT = 2;



    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            updateElapsedTime();
            if(isDisplayed) {
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
            }
        }
    };

    private final long mFrequencyWidget = 60000;    // milliseconds

    private Handler mHandlerWidget = new Handler() {
        public void handleMessage(Message m) {
            updateWidgets();
        }
    };

    private void updateElapsedTime() {
        if(isDisplayed) {
            long countDown = 300 - (System.currentTimeMillis() - m_lastPacketTime) / 1000;
            while(countDown<0) {
                countDown += 300;
            }
            tvInRange.setText("" + countDown);
            progressBar.setProgress((int) countDown);
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(TAG, "onCreate");
		
		setContentView(R.layout.activity_main);

        try {
            MainApp.bus().unregister(this);
        } catch (RuntimeException x) {
            // Ignore
        }

        MainApp.bus().register(this);


		
		{
			String alarm = Context.ALARM_SERVICE;
			AlarmManager am = ( AlarmManager ) getSystemService( alarm );
			 
			Intent intent = new Intent( "REFRESH_THIS" );
			PendingIntent pi = PendingIntent.getBroadcast( this, 0, intent, 0 );
			 
			int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long interval = 300000L;
			long triggerTime = SystemClock.elapsedRealtime() + interval;
			 
			am.cancel(pi);
			am.setRepeating( type, SystemClock.elapsedRealtime(), interval, pi );
		}

	    handler = new Handler();

	    tv1 = (TextView) findViewById(R.id.text);
		tv2 = (TextView) findViewById(R.id.text2);
		tv3 = (TextView) findViewById(R.id.text3);
		status = (TextView) findViewById(R.id.textViewStatus);
		tvInRange = (TextView) findViewById(R.id.textInRange);
		

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(300);

		initButtonsAndSetup();
		
		// initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        plot.setOnTouchListener(this);

        // Turn the above arrays into XYSeries':
        series1 = new XYSeriesData(); 
		
        seriesMin = new XYSeriesConstMin(); 
        seriesMax = new XYSeriesConstMax();

        
        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:

        Paint backgroundPaint = plot.getBackgroundPaint();
        backgroundPaint.setColor(Color.BLACK);
        plot.setBackgroundPaint(backgroundPaint);

        plot.setBorderPaint(null);
        
//        LineAndPointFormatter series1Format = createLineAndPointFormatter();

        // add a new series' to the xyplot:
        plot.addSeries(seriesMin,           createLineAndPointFormatter(Color.RED,      2,true));
        plot.addSeries(seriesMax,           createLineAndPointFormatter(Color.YELLOW,   2,true));


        plot.addSeries(series1,             createLineAndPointFormatter(Color.WHITE,    6,false));

        // reduce the number of range labels
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 2);
        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1800);

//        plot.getGraphWidget().setDomainLabelOrientation(-90);

        XYGraphWidget graphWidget = plot.getGraphWidget();
//        graphWidget.position(-10, XLayoutStyle.ABSOLUTE_FROM_LEFT, 0, YLayoutStyle.ABSOLUTE_FROM_TOP);

        Paint gridBackgroundPaint = graphWidget.getGridBackgroundPaint();
        gridBackgroundPaint.setColor(Color.GRAY);
        gridBackgroundPaint.setAlpha(250);
        gridBackgroundPaint.setStyle(Paint.Style.STROKE);
        graphWidget.setGridBackgroundPaint(gridBackgroundPaint);

        graphWidget.setRangeAxisLeft(false);

//        Paint borderPaint = new Paint();
//        borderPaint.setColor(Color.GRAY);
//        borderPaint.setStyle(Paint.Style.STROKE);
//        graphWidget.setBorderPaint(borderPaint);

//        graphWidget.setRangeLabelHorizontalOffset(PixelUtils.dpToPix(13));
        graphWidget.setRangeLabelVerticalOffset(PixelUtils.dpToPix(-4));
        graphWidget.setRangeTick(true);
        graphWidget.setDomainTick(true);

        Paint rangeLabelPaint = graphWidget.getRangeLabelPaint();
        rangeLabelPaint.setTextAlign(Paint.Align.LEFT);
//        rangeLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        rangeLabelPaint.setTypeface(Typeface.create("sans-serif-light",Typeface.BOLD));
        rangeLabelPaint.setColor(Color.WHITE);
        graphWidget.setRangeLabelPaint(rangeLabelPaint);


        Paint domainLabelPaint = graphWidget.getDomainLabelPaint();
        domainLabelPaint.setTypeface(Typeface.create("sans-serif-light",Typeface.BOLD));
        domainLabelPaint.setColor(Color.WHITE);
        graphWidget.setDomainLabelPaint(domainLabelPaint);

        //	For vertical lines:
        Paint domainGridLine =  graphWidget.getDomainGridLinePaint();
        domainGridLine.setColor(Color.GRAY);
        domainGridLine.setAlpha(230);

        graphWidget.setDomainGridLinePaint(domainGridLine);//ridLinePaint().setColor(Color.TRANSPARENT);
        graphWidget.setDomainSubGridLinePaint(null);
        graphWidget.setDomainLabelTickExtension(10);

        //For Horizontal lines:
        Paint gridLine = graphWidget.getRangeGridLinePaint();
        gridLine.setColor(Color.GRAY);
        gridLine.setAlpha(230);

        graphWidget.setRangeGridLinePaint(gridLine);//idLinePaint().setColor(Color.TRANSPARENT);
        graphWidget.setRangeSubGridLinePaint(null);

        graphWidget.setRangeLabelTickExtension(5);

        graphWidget.setBackgroundPaint(null);
        
//        SizeMetrics sm = new SizeMetrics(0, SizeLayoutType.FILL, 0,SizeLayoutType.FILL);
//        graphWidget.setSize(sm);

        graphWidget.setDrawMarkersEnabled(false);
        
        plot.getLayoutManager().remove(plot.getLegendWidget());
        plot.getLayoutManager().remove(plot.getDomainLabelWidget());
        plot.getLayoutManager().remove(plot.getRangeLabelWidget());
        plot.getLayoutManager().remove(plot.getTitleWidget());
        plot.getLayoutManager().refreshLayout();
//        plot.getLegendWidget().setVisible(false);
//        plot.getDomainLabelWidget().setVisible(false);
//        plot.getRangeLabelWidget().setVisible(false);
//        plot.getTitleWidget().setVisible(false);
        
//        plot.getGraphWidget().position(-0.5f, XLayoutStyle.RELATIVE_TO_RIGHT, 
//                -0.5f, YLayoutStyle.RELATIVE_TO_BOTTOM,
//                AnchorPosition.CENTER);
//        graphWidget.setRangeLabelWidth(30);
//        graphWidget.setDomainLabelWidth(10);

        plot.setRangeValueFormat(new DecimalFormat("#"));

        Format domainValueFormat = new Format() {
			@Override
			public Object parseObject(String string, ParsePosition position) {
				return null;
			}
			
			@Override
			public StringBuffer format(Object object, StringBuffer buffer,FieldPosition field) {

                long object1 = ((Double) object).longValue();
                Date tmp = new Date(object1*1000);
                String formated = timeFormat.format(tmp);

//                Log.d(TAG,"formatting "+tmp + " to "+formated);
                return buffer.append(formated);
			}
		};
		plot.setDomainValueFormat(domainValueFormat);//mat(new DecimalFormat("#"));

        plot.setRangeBoundaries(2, BoundaryMode.FIXED, 22,BoundaryMode.FIXED);
//        plot.setDomainBoundaries(6, 12, BoundaryMode.FIXED);
		//startTimer();
        updateUI();
	}

	private void initButtonsAndSetup() {
		button = (Button) findViewById(R.id.button3);
		button.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        range = 300*40;
		        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1800);
				plot.setDomainBoundaries( series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch-range, series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch, BoundaryMode.FIXED);

//		        plot.redraw();
				updateUI();
		    }
		});
		
		button6 = (Button) findViewById(R.id.button6);
		button6.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	range = 300*80;
		    	plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 3600);
				plot.setDomainBoundaries( series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch-range, series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch, BoundaryMode.FIXED);

//		    	plot.redraw();
				updateUI();
		    }
		});
		
		button12 = (Button) findViewById(R.id.button12);
		button12.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	range = 300*160;
		    	plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 7200);
		    	 
		    	
				plot.setDomainBoundaries( series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch-range, series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch, BoundaryMode.FIXED);
//		    	plot.redraw();
				updateUI();
		    }
		});
		
		button24 = (Button) findViewById(R.id.button24);
		button24.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	range = 300*320;
		    	plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 14400);
				plot.setDomainBoundaries( series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch-range, series1.glucoseReadRecords[series1.glucoseReadRecords.length-1].LocalSecondsSinceDexcomEpoch, BoundaryMode.FIXED);
//		    	plot.redraw();
				updateUI();
//		    	button24.setPressed(false);
		    }
		});
		
		buttonRefresh = (Button) findViewById(R.id.buttonRefresh);
		buttonRefresh.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
					MainApp.bus().post(new RefreshData());
		    }
		});
	}

	private LineAndPointFormatter createLineAndPointFormatter(int color,float width, boolean line) {
		LineAndPointFormatter series1Format = new LineAndPointFormatter();
        PointLabelFormatter pointLabelFormatter = new PointLabelFormatter();
        series1Format.setPointLabeler(new PointLabeler() {
			
			@Override
			public String getLabel(XYSeries series, int index) {
				
				return "";
			}
		});
		series1Format.setPointLabelFormatter(pointLabelFormatter);
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);
        Paint lp = series1Format.getLinePaint();
        lp.setColor(color);
        lp.setAntiAlias(false);
        lp.setStrokeWidth(width);
        if(line) {
        	series1Format.setVertexPaint(null);
        	series1Format.setLinePaint(lp);
        } else {
        	Paint linep = new Paint(lp);
        	linep.setStrokeWidth(1);
        	linep.setColor(color);
        	series1Format.setLinePaint(null);
        	series1Format.setVertexPaint(lp);
        }
        series1Format.setPointLabelFormatter(null);
		return series1Format;
	}

    @Override
    public void onDestroy() 
    {
    	super.onDestroy();
    	
		Log.i(TAG, "Activity destroyed");

    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Subscribe
    public void onRefreshErrorEvent(final RefreshError event) {
        Log.d(TAG, "onRefreshErrorEvent");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    status.setText(event.mMessage);
                } catch (RuntimeException e) {
                    Log.e(TAG, "updateUI error " + e.getMessage(), e);
                }
            }
        });
    }


    @Subscribe
    public void onRefreshingDataEvent(RefreshingData event) {
        Log.d(TAG, "onRefreshingDataEvent");
        if(isDisplayed) {

        }
    }

    @Subscribe
    public void onRefreshDataEvent(WidgetData event) {
        updateWidgets();
    }

    @Subscribe
    public void onRefreshDataEvent(RefreshData event) {
        Log.d(TAG, "onRefreshDataEvent");
        //if(isDisplayed) {
        updateUI();
        //}
        updateWidgets();
    }

    @Subscribe
    public void onBTStatusEvent(final BTStatusEvent event) {
        Log.d(TAG, "onBTStatusEvent");
        isBTconnected = "BT".equals(event.mStatus);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                status.setText(""+ getBTstatusString());
            }});
    }


    TimeMatechedRecord current,beforeCurrent;

    public void updateUI() {
		Log.d(TAG, "updateUI");

        runOnUiThread(new Runnable() {
			@Override
			public void run() {
            try {
                DecimalFormat df = new DecimalFormat("#.00");

                tv1.setText("..");
                status.setText(""+ getBTstatusString());
                TimeMatechedRecordDao dao = new TimeMatechedRecordDao(getApplicationContext());
                List<TimeMatechedRecord> all = dao.listAll();

//                Log.d(TAG, "Data range in local DB "
//                        + timeFormat.format( all.get(0).LocalSecondsSinceDexcomEpoch )
//                        + timeFormat.format( all.get(all.size()-1).LocalSecondsSinceDexcomEpoch )
//                        );

                if(all.size()>300) {
                    all = all.subList(all.size() - 300, all.size());
                }
                TimeMatechedRecord[] glucoseReadRecords = all.toArray(new TimeMatechedRecord[all.size()]);

//                Log.d(TAG, "Data updated "+all.size());

                TimeMatechedRecord glucoseReadRecord = glucoseReadRecords[glucoseReadRecords.length - 1];

                current = glucoseReadRecord;
                beforeCurrent = glucoseReadRecords[glucoseReadRecords.length - 2];


                series1.glucoseReadRecords = glucoseReadRecords;
                seriesMin.glucoseReadRecords = glucoseReadRecords;
                seriesMax.glucoseReadRecords = glucoseReadRecords;


                rangeStart = glucoseReadRecord.LocalSecondsSinceDexcomEpoch;

                plot.setDomainBoundaries(
                        rangeStart - range,
                        rangeStart,
                        BoundaryMode.FIXED);
                lastScrolling = 0;
                plot.redraw();



                int glucoseValueWithFlags = (int) glucoseReadRecord.GlucoseValue;


                if (glucoseValueWithFlags == 10) {
                    tv1.setText("???");
                } else if (glucoseValueWithFlags == 5) {
                    tv1.setText("?");
                } else if (glucoseValueWithFlags==0 ) {
                    tv1.setText("");
                }else{
                    tv1.setText("" + df.format(glucoseValueWithFlags / 18d));
                }



                tv3.setText(timeFormat.format( glucoseReadRecord.LocalSecondsSinceDexcomEpoch*1000));

                m_lastPacketTime =glucoseReadRecord.LocalSecondsSinceDexcomEpoch*1000;

                Log.d(TAG,"Last record in DB " + glucoseReadRecord.timeIndex);

            } catch (RuntimeException e) {
                Log.e(TAG, "updateUI error "+e.getMessage(),e);
            }

			}
		});
		
	}

    private String getBTstatusString() {
        return (isBTconnected ? "BT" : "");
//        return (isBTconnected ? "" : "");
    }




    public void updateWidgets() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"updating widgets");
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

                ComponentName thisWidget = new ComponentName(getApplicationContext(),DexAppWidgetProvider.class);
                int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                DecimalFormat df = new DecimalFormat("0.00");

                for (int widgetId : allWidgetIds) {
                    AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(widgetId);
                    RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(),R.layout.widget_layout);

                    String widgetText = "No data yet";

                    TimeMatechedRecord glucoseReadRecord = current;
                    TimeMatechedRecord glucoseReadRecordOld = beforeCurrent;

                    if(glucoseReadRecord.GlucoseValue!=0) {
                        widgetText =
                                " " +
                                        df.format(glucoseReadRecord.GlucoseValue / 18d) + " (" +
                                        df.format(glucoseReadRecord.GlucoseValue / 18d - glucoseReadRecordOld.GlucoseValue / 18d) +
                                        ") " ;
                    }

                    remoteViews.setTextViewText(R.id.textViewStatus,
                            ""+
                                ((System.currentTimeMillis() - m_lastPacketTime)/60000)
                                + " mins ago "
                );
                    remoteViews.setTextViewText(R.id.update, widgetText);

                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
                if(allWidgetIds.length>0) {
                    mHandlerWidget.sendMessageDelayed(Message.obtain(mHandlerWidget, TICK_WHAT), mFrequencyWidget);
                }
            }
        });
    }

    PointF firstFinger;
    float lastScrolling;

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

        case MotionEvent.ACTION_DOWN: // Start gesture
            firstFinger = new PointF(motionEvent.getX(), motionEvent.getY());
            //Log.d(TAG,"onTouch "+view + " "+motionEvent);
        break;

        case MotionEvent.ACTION_UP:
            lastScrolling+= (motionEvent.getX() -firstFinger.x)*2;

            //Log.d(TAG,"onTouch ACTION_UP "+lastScrolling);
            plot.setDomainBoundaries(
                    rangeStart - range-lastScrolling,
                    rangeStart-lastScrolling,
                    BoundaryMode.FIXED);

            plot.redraw();


        break;

        case MotionEvent.ACTION_MOVE:
            float deltaX = (motionEvent.getX() - firstFinger.x) * 20;

            lastScrolling+= deltaX;
            //Log.d(TAG,"onTouch ACTION_MOVE "+lastScrolling + " "+deltaX) ;
            if(deltaX>60 || deltaX<-60) {
                plot.setDomainBoundaries(
                        rangeStart - range - lastScrolling,
                        rangeStart - lastScrolling,
                        BoundaryMode.FIXED);

                plot.redraw();
                firstFinger = new PointF(motionEvent.getX(), motionEvent.getY());
            }
        break;
    }
    return true;

    }

    private void scroll(float lastScrolling) {
        //Log.d(TAG,"scroll "+lastScrolling);
    }
}
