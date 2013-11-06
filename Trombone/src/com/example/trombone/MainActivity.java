package com.example.trombone;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends Activity implements OnClickListener{
	int frequency = 8000;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	private RealDoubleFFT transformer;
	int blockSize = 256;
	Button startStopButton, recordButton1, recordButton2, backgroundButton;
	boolean started = false;
	boolean recording_1 = false;
	boolean recording_2 = false;
	boolean recorded_1 = false;
	boolean recorded_2 = false;
	boolean recording_background = false;
	boolean recorded_background = false;
	int sampleSize = 10;
	int sampleCount = 0;

	RecordAudio recordTask;

	ImageView currentSpec, rec1Spec, rec2Spec;
	Bitmap curBitmap, rec1Bitmap, rec2Bitmap;
	Canvas curCanvas, rec1Canvas, rec2Canvas;
	Paint paint;
	
	TextView resultText, debugText;
	
	int peakSize1, peakSize2;
	double initialError1, initialError2;
	double[] toTransformBackground = new double[blockSize];
	double[] toTransformRec1 = new double[blockSize];
	double[] toTransformRec2 = new double[blockSize];
	double[][] toTransformSample = new double[blockSize][sampleSize];

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		resultText = (TextView)findViewById(R.id.resultText);
		debugText = (TextView)findViewById(R.id.debugText);

		startStopButton = (Button)findViewById(R.id.StartStopButton);
		startStopButton.setOnClickListener(this);
		
		recordButton1 = (Button)findViewById(R.id.Instrument1Record);
		recordButton1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				recording_1 = true;
			}
		});
		recordButton2 = (Button)findViewById(R.id.Instrument2Record);
		recordButton2.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				recording_2 = true;
			}
		});
		backgroundButton = (Button)findViewById(R.id.BackgroundButton);
		backgroundButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				recording_background = true;
			}
		});

		transformer = new RealDoubleFFT(blockSize);

		paint = new Paint();
		paint.setColor(Color.GREEN);
		
		currentSpec = (ImageView)findViewById(R.id.CurrentSpectrum);
		curBitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
		curCanvas = new Canvas(curBitmap);
		currentSpec.setImageBitmap(curBitmap);

		rec1Spec = (ImageView)findViewById(R.id.Instrument1Spectrum);
		rec1Bitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
		rec1Canvas = new Canvas(rec1Bitmap);
		rec1Spec.setImageBitmap(rec1Bitmap);
		
		rec2Spec = (ImageView)findViewById(R.id.Instrument2Spectrum);
		rec2Bitmap = Bitmap.createBitmap((int)256, (int)100, Bitmap.Config.ARGB_8888);
		rec2Canvas = new Canvas(rec2Bitmap);
		rec2Spec.setImageBitmap(rec2Bitmap);
	}

	private class RecordAudio extends AsyncTask<Void, double[], Void>{
		@Override
		protected Void doInBackground(Void... params) {
			try{
				int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferSize);

				short[] buffer = new short[blockSize];
				double[] toTransform = new double[blockSize];

				audioRecord.startRecording();

				while(started){
					int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

					for(int i = 0; i < blockSize && i < bufferReadResult; i++){
						toTransform[i] = (double)buffer[i] / Short.MAX_VALUE;
					}

					transformer.ft(toTransform);
					publishProgress(toTransform);
				}

				audioRecord.stop();
			}catch(Throwable t){
				Log.e("AudioRecord", "Recording Failed");
			}

			return null;
		}

		// not in background
		@Override
		protected void onProgressUpdate(double[]... toTransform) {
			curCanvas.drawColor(Color.BLACK);
			if (recording_1) {
				rec1Canvas.drawColor(Color.BLACK);
			}
			if (recording_2) {
				rec2Canvas.drawColor(Color.BLACK);
			}
			
			double maxIntensity = 0; 
			for (int i = 0; i<toTransform[0].length; i++){
				maxIntensity = Math.max(maxIntensity, Math.abs(toTransform[0][i]));
			}
			
			for(int i = 0; i < toTransform[0].length; i++){			
				int x = i;
				int downy = (int) (100 - (toTransform[0][i] * 10));
				int upy = 100;

				curCanvas.drawLine(x, downy, x, upy, paint);
				if (recording_1) {
					rec1Canvas.drawLine(x, downy, x, upy, paint);
				}
				if (recording_2) {
					rec2Canvas.drawLine(x, downy, x, upy, paint);
				}
			}
			
			if (recorded_1 && recorded_2 && recorded_background) {
				if (sampleCount < sampleSize) {
					for(int i = 0; i < toTransform[0].length; i++){
						toTransformSample[i][sampleCount] = toTransform[0][i];
					}
					sampleCount++;
				} else {
					double error1=Double.MAX_VALUE, error2=Double.MAX_VALUE;
					for(int j = 0; j < sampleSize; j++){
						double tmp_error1 = 0, tmp_error2 = 0;
						for(int i = 0; i < toTransform[0].length; i++){
							if (toTransformRec1[i] > -900) {
								tmp_error1 += Math.pow(toTransformSample[i][j]-toTransformRec1[i], 2);
							}
							if (toTransformRec2[i] > -900) {
								tmp_error2 += Math.pow(toTransformSample[i][j]-toTransformRec2[i], 2);
							}
							
							/*
							if (Math.abs(toTransformSample[i][j])/max_s > 0.1) {
								if (Math.abs(toTransformRec1[i])/max_1 > 0.1) {
									//tmp_error1 += Math.pow(toTransformSample[i][j]-toTransformRec1[i], 2);
									//tmp_error1 += Math.min(Math.abs(toTransformSample[i][j]), Math.abs(toTransformRec1[i]))/Math.max(Math.abs(toTransformSample[i][j]), Math.abs(toTransformRec1[i]));
									eff_count1++;
								}
								if (Math.abs(toTransformRec2[i])/max_2 > 0.1) {
									//tmp_error2 += Math.pow(toTransformSample[i][j]-toTransformRec2[i], 2);
									//tmp_error2 += Math.min(Math.abs(toTransformSample[i][j]), Math.abs(toTransformRec2[i]))/Math.max(Math.abs(toTransformSample[i][j]), Math.abs(toTransformRec2[i]));
									eff_count2++;
								}
							}
							*/
						}
						
						error1 = Math.min(error1, tmp_error1 / peakSize1);
						error2 = Math.min(error2, tmp_error2 / peakSize2);
						
						// TODO : normalize?
					}
					
					
					String output = "";
					/*
					if (error1 < error2 && error1 < 350) output = "1";
					if (error1 > error2 && error2 < 350) output = "2";
					*/
					
					double normalizedScore1 = (initialError1 - error1) / initialError1;
					double normalizedScore2 = (initialError2 - error2) / initialError2;
					
					if (normalizedScore1 > normalizedScore2 && normalizedScore1 > 0.15)
						output = String.format("1\n\nIntrument 1 : %.4f\nIntrument 2 : %.4f", normalizedScore1, normalizedScore2);
					else if (normalizedScore2 >= normalizedScore1 && normalizedScore2 > 0.15)
						output = String.format("2\n\nIntrument 1 : %.4f\nIntrument 2 : %.4f", normalizedScore1, normalizedScore2);
					/*
					else if (error1 > error2) output = String.format("2\n\nIntrument 1 : %.4f\nIntrument 2 : %.4f", error1, error2);
					else output = String.format("\n\nIntrument 1 : %.4f\nIntrument 2 : %.4f", error1, error2);
					*/
					
					resultText.setText(output);
					sampleCount = 0;
				}
			}
			
			currentSpec.invalidate();
			if (recording_1) {
				rec1Spec.invalidate();
				recording_1 = false;
				
				//toTransformRec1 = toTransform[0].clone();

				int peakCount = 0;
				initialError1 = 0;
				for (int i = 0; i < toTransform[0].length; i++) {
					if (Math.abs(toTransform[0][i])/maxIntensity > 0.2) {
						toTransformRec1[i] = toTransform[0][i];
						peakCount++;
						initialError1 += Math.pow((toTransformRec1[i]-toTransformBackground[i]), 2);
					} else {
						toTransformRec1[i] = -999;
					}
				}
				peakSize1 = peakCount;
				initialError1 = initialError1 / peakSize1;
				recorded_1 = true;
			}
			if (recording_2) {
				rec2Spec.invalidate();
				recording_2 = false;
				
				//toTransformRec2 = toTransform[0].clone();
				int peakCount = 0;
				initialError2 = 0;
				for (int i = 0; i < toTransform[0].length; i++) {
					if (Math.abs(toTransform[0][i])/maxIntensity > 0.2) {
						toTransformRec2[i] = toTransform[0][i];
						peakCount++;
						initialError2 += Math.pow((toTransformRec2[i]-toTransformBackground[i]), 2);
					} else {
						toTransformRec2[i] = -999;
					}
				}
				peakSize2 = peakCount;
				initialError2 = initialError2 / peakSize2;
				recorded_2 = true;
			}
			if (recording_background) {
				recording_background = false;
				
				toTransformBackground = toTransform[0].clone();
				recorded_background = true;
				recordButton1.setVisibility(0);
				recordButton2.setVisibility(0);
			}
		}
	}

	@Override
	public void onClick(View arg0) {
		if(started){
			started = false;
			startStopButton.setText("Start");
			recordTask.cancel(true);
		}else{
			started = true;
			startStopButton.setText("Stop");
			recordTask = new RecordAudio();
			recordTask.execute();
		}
	}
}