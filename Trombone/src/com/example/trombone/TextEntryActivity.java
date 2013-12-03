package com.example.trombone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

public class TextEntryActivity extends Activity {
    private EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_text_entry);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        // title
        try {
            String s = getIntent().getExtras().getString("title");
            if (s.length() > 0) {
                this.setTitle(s);
            }
        } catch (Exception e) {
        }
        // value
        
        if (!getIntent().getBooleanExtra("deletable", false)) {
        	findViewById(R.id.btnDelete).setVisibility(View.GONE);
        } else {
        	findViewById(R.id.btnDelete).setVisibility(View.VISIBLE);
        }
        
        if (!getIntent().getBooleanExtra("enterOpacity", true)) {
        	findViewById(R.id.textView1).setVisibility(View.GONE);
        	findViewById(R.id.opacityBar).setVisibility(View.GONE);
        } else {
        	findViewById(R.id.textView1).setVisibility(View.VISIBLE);
        	findViewById(R.id.opacityBar).setVisibility(View.VISIBLE);
        }

        try {
            et = ((EditText) findViewById(R.id.txtValue));
            et.setText(getIntent().getExtras().getString("value"));
        } catch (Exception e) {
        }
        // button
        ((Button) findViewById(R.id.btnDone)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                executeDone();
            }
        });
        ((Button) findViewById(R.id.btnDelete)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                executeDelete();
            }
        });
    }

    @Override
    public void onBackPressed() {
        executeDone();
        super.onBackPressed();
    }

    /**
     *
     */
    private void executeDone() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("delete", false);
        resultIntent.putExtra("value", TextEntryActivity.this.et.getText().toString());
        resultIntent.putExtra("opacity", ((SeekBar) findViewById(R.id.opacityBar)).getProgress());
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
    private void executeDelete() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("delete", true);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


}