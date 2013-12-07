package com.example.trombone;


import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.essence.chart.Chart;
import com.essence.chart.ChartCallback;
import com.essence.chart.GridData;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class HistoryActivity extends Activity {
	private Chart chart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    //set up full screen
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);  

		setContentView(R.layout.activity_history);
		
		chart = (Chart) findViewById(R.id.history_chart);
		
		// TODO : display multiple lines, series
		chart.setChartType(Chart.Chart_Type_Line);

		 // chart.setYAxisMaximum(true, 2000);

	}

}
