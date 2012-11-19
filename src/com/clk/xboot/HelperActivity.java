package com.clk.xboot;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.TextView;

public class HelperActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.helper);
        final TextView tv = (TextView)findViewById(R.id.textView3);
        tv.setTextSize(12);
        
        String mkb_cmd = (String) getIntent().getExtras().get("mkbootimgCMD");
        String fi_cmd = (String) getIntent().getExtras().get("flash_imageCMD");
        String img = (String) getIntent().getExtras().get("img_file");
        
        File sdcard = new File(Environment.getExternalStorageDirectory().getPath());
        File img_file = new File(sdcard.getAbsolutePath() + "/NativeSD/" + img + ".img");
        if (img_file.exists()) {
        	img_file.delete();
        }
        
        Command combo_command = new Command(0, mkb_cmd, fi_cmd) {
	        @SuppressLint("NewApi")
			@Override
	        public void output(int id, String line) {
	        	if (!line.isEmpty()) {
	        		tv.append(line + "\n");
	        	}
	        }
		};
		try {
			RootTools.getShell(true).add(combo_command).waitForFinish();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		tv.append("\nFinished!");
		
		final Handler handler = new Handler(); 
        Timer t = new Timer(); 
        t.schedule(new TimerTask() { 
        	@Override
			public void run() { 
        		handler.post(new Runnable() { 
        			public void run() { 
        				finish();
                    } 
                }); 
            } 
        }, 5000);
	}
}
