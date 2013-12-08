package com.example.trombone;


import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import com.essence.chart.Chart;
import com.essence.chart.ChartCallback;
import com.essence.chart.GridData;

import static classes.Constants.*; 
import classes.*;
import db.DBHelper;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class HistoryActivity extends Activity {
	private Chart scoreChart, countChart;
	DBHelper dbhelper = new DBHelper(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);  
		//set up full screen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
				WindowManager.LayoutParams.FLAG_FULLSCREEN);  

		setContentView(R.layout.activity_history);

		scoreChart = (Chart) findViewById(R.id.history_score_chart);
		countChart = (Chart) findViewById(R.id.history_count_chart);

		// set height of charts
		LayoutParams layoutParamsChart = scoreChart.getLayoutParams();
		layoutParamsChart.height = (int) (0.5 * nexus7_height);
		scoreChart.setLayoutParams(layoutParamsChart);

		layoutParamsChart = countChart.getLayoutParams();
		layoutParamsChart.height = (int) (0.5 * nexus7_height);
		countChart.setLayoutParams(layoutParamsChart);

		// score chart
		scoreChart.setChartType(Chart.Chart_Type_Line);
		scoreChart.setLegendVisible(true);
		// XXX : legend name
		scoreChart.setTitle("Score history");
		scoreChart.setTitleFontSize(30);
		
		// TODO : set history data
		int nRow = 3;
		int nCol = 1;
		GridData gridData = new GridData(nRow, nCol);
		for (int i = 0; i < nRow; i++) {
			for (int j = 0; j < nCol; j++) {
				gridData.setCell(i, j, 10*i + 5*j);   // XXX : dummy value
			}
		}

		scoreChart.setSourceData(gridData, 0);

		// TODO : set data name (music sheet title)

		// TODO : set X-axis name correctly 


		// chart.setYAxisMaximum(true, 2000);

		// count chart
		countChart.setChartType(Chart.Chart_Type_Clustered_Column);
		countChart.setLegendVisible(false);
		countChart.setTitle("Hits");
		countChart.setTitleFontSize(30);

		List<MusicSheet> musicSheets = dbhelper.getAllMusicSheets(1);
		nRow = 2;
		nCol = musicSheets.size();
		gridData = new GridData(nRow, nCol);
		for (int j = 0; j < nCol; j++) {
			MusicSheet musicSheet = musicSheets.get(j);
			gridData.setCell(0, j, musicSheet.getName());
			gridData.setCell(1, j, musicSheet.getPlayCount());
			// XXX : future work. finish count
		}

		countChart.setSourceData(gridData, 1);

	}

}
