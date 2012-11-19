package com.clk.xboot;
/*
 * koko:
 * 		This is a boot manager application built
 * 		specifically for HD2's Black-cLK bootloader
 *      in combination with NativeSD method
 * 
 * It is able** to:
 * 		1.Reboot and pass a msg to bootloader so that
 *        the kernel from the selected boot partition will be loaded.
 * 		2.Flash a new kernel to any existing boot partition,
 * 		3.Reboot to recovery,
 * 		4.Reboot to bootloader
 *      5.Soft reset.
 *
 *		**the apk needs to be signed
 *		  and pushed to /system/app in order to work as intended.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.PowerManager;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;

public class MainActivity extends Activity {
	private final int ID_MENU_INF = 0;
	private final int ID_MENU_REC = 1;
	private final int ID_MENU_BLDR = 2;
	private final int ID_MENU_RST = 3;
	private final int ID_MENU_EXIT = 4;
	
	PowerManager pm;
	Spinner x_boot_partitions;
	Spinner rom_s_kernel;
	String flash_image = "/data/data/com.clk.xboot/files/flash_image";
	String mkbootimg = "/data/data/com.clk.xboot/files/mkbootimg";
	
	boolean found_boot = false;
	boolean found_sboot = false;
	boolean found_tboot = false;
	boolean found_vboot = false;
	boolean found_wboot = false;
	boolean found_xboot = false;
	boolean found_yboot = false;
	boolean found_zboot = false;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        RootTools.debugMode = false;
        
        // boot partitions list
        x_boot_partitions = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.xBoot_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        x_boot_partitions.setAdapter(adapter1);
        x_boot_partitions.setOnItemSelectedListener(new OnXbootSelectedListener());
        
        // list of NativeSD Roms
    	ArrayList<String> Roms = new ArrayList<String>();
    	File sdcard = new File(Environment.getExternalStorageDirectory().getPath());
        File NativeSD = new File(sdcard.getAbsolutePath() + "/NativeSD");
        if (NativeSD.isDirectory()) {
        	 File[] filelist = NativeSD.listFiles();
        	 
        	 for(int i=0; i < filelist.length; i++) {
             	if(filelist[i].isDirectory()) {
             		Roms.add(filelist[i].getName());
             	}
             }

        	 rom_s_kernel = (Spinner) findViewById(R.id.spinner2);
             ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Roms);
             adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        	 rom_s_kernel.setAdapter(adapter2);
        	 rom_s_kernel.setOnItemSelectedListener(new OnRomSelectedListener());
        } else {
        	 makeToast("No NativeSD folder found!");
        }
       
        // Tools
        File fi = this.getFileStreamPath("flash_image");
        File mkb = this.getFileStreamPath("mkbootimg");
        if(!(fi.exists() && mkb.exists())) {
        	Extract_flash_tools();
        }
        
        // Root?
        if (RootTools.isRootAvailable()) {
        	if (RootTools.isAccessGiven()) {
        		//Check which of the available extra boot partitions are present
        		Command command = new Command(0, "cat proc/mtd")
            	{
            	        @Override
            	        public void output(int id, String line)
            	        {
            	        	if (line.contains("\"boot\"")) {
            	        		found_boot = true;
            	        	} 
            	        	if (line.contains("\"sboot\"")) {
            	        		found_sboot = true;
            	        	} 
            	        	if (line.contains("\"tboot\"")) {
            	        		found_tboot = true;
            	        	} 
            	        	if (line.contains("\"vboot\"")) {
            	        		found_vboot = true;
            	        	} 
            	        	if (line.contains("\"wboot\"")) {
            	        		found_wboot = true;
            	        	} 
            	        	if (line.contains("\"xboot\"")) {
            	        		found_xboot = true;
            	        	} 
            	        	if (line.contains("\"yboot\"")) {
            	        		found_yboot = true;
            	        	} 
            	        	if (line.contains("\"zboot\"")) {
            	        		found_zboot = true;
            	        	} 
            	        }
            	};
            	try {
    				RootTools.getShell(true).add(command).waitForFinish();
    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			} catch (IOException e) {
    				e.printStackTrace();
    			} catch (TimeoutException e) {
    				e.printStackTrace();
    			}
        	} else {
        		AlertDialog.Builder NoRoot = new AlertDialog.Builder(this);
        		NoRoot.setMessage("Root access could not be obtained. Please check whether your device is rooted, or restart the application to try again.");
        		NoRoot.setPositiveButton("Exit",
        								new DialogInterface.OnClickListener() {public void onClick(DialogInterface arg0, int arg1) {finish();}}
        								);
            	NoRoot.show();
        	}
        } else {
        	AlertDialog.Builder NoRoot = new AlertDialog.Builder(this);
    		NoRoot.setMessage("Root access could not be obtained. Please check whether your device is rooted, or restart the application to try again.");
    		NoRoot.setPositiveButton("Exit",
    								new DialogInterface.OnClickListener() {public void onClick(DialogInterface arg0, int arg1) {finish();}}
    								);
        	NoRoot.show();
        }
        
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, ID_MENU_INF, Menu.NONE, R.string.menu_item_0);
    	menu.add(Menu.NONE, ID_MENU_REC, Menu.NONE, R.string.menu_item_1);
    	menu.add(Menu.NONE, ID_MENU_BLDR, Menu.NONE, R.string.menu_item_2);
    	menu.add(Menu.NONE, ID_MENU_RST, Menu.NONE, R.string.menu_item_3);
    	menu.add(Menu.NONE, ID_MENU_EXIT, Menu.NONE, R.string.menu_item_4);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == ID_MENU_INF)	{
    		AlertDialog.Builder info = new AlertDialog.Builder(this);
    		info.setMessage(R.string.Info);
    		info.setCancelable(false);
    		info.setNegativeButton("Close", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface arg0, int arg1) {
    				arg0.cancel();
    				}
    			}
    		);
        	info.show();
    		return true;
    	}
    	if(item.getItemId() == ID_MENU_REC)	{
    		ReBoot("recovery");
    		return true;
    	}
    	else if(item.getItemId() == ID_MENU_BLDR) {
    		ReBoot("bootloader");
    		return true;
    	}
    	else if(item.getItemId() == ID_MENU_RST) {
    		ReBoot(null);
    		return true;
    	}
    	else if(item.getItemId() == ID_MENU_EXIT) {
    		this.finish();
    		return true;
    	}
    	return false;
    }
    
    public void reboot_to_boot(View view) {
    	if (found_boot) {
    		ReBoot("oem-" + 0);
    	} else {
    		makeToast("Didn't detect boot partition.");
    	}
    }
    
    public void reboot_to_sboot(View view) {
    	if (found_sboot) {
    		ReBoot("oem-" + 1);
    	} else {
    		makeToast("Didn't detect sboot partition.");
    	}
    }
    
    public void reboot_to_tboot(View view) {
    	if (found_tboot) {
    		ReBoot("oem-" + 2);
    	} else {
    		makeToast("Didn't detect tboot partition.");
    	}
    }

    public void reboot_to_vboot(View view) {
    	if (found_vboot) {
    		ReBoot("oem-" + 3);
    	} else {
    		makeToast("Didn't detect vboot partition.");
    	}
    }

    public void reboot_to_wboot(View view) {
    	if (found_wboot) {
    		ReBoot("oem-" + 4);
    	} else {
    		makeToast("Didn't detect wboot partition.");
    	}
    }

    public void reboot_to_xboot(View view) {
    	if (found_xboot) {
    		ReBoot("oem-" + 5);
    	} else {
    		makeToast("Didn't detect xboot partition.");
    	}
    }

    public void reboot_to_yboot(View view) {
    	if (found_yboot) {
    		ReBoot("oem-" + 6);
    	} else {
    		makeToast("Didn't detect yboot partition.");
    	}
    }

    public void reboot_to_zboot(View view) {
    	if (found_zboot) {
    		ReBoot("oem-" + 7);
    	} else {
    		makeToast("Didn't detect zboot partition.");    		
    	}
    }
    
    public void ReBoot(String reason) {
    	/*
    	 * Exploiting "oem-" prefix of the reboot_reason to
    	 * catch the correct 'boot' partition number in cLK
    	 */
    	moveTaskToBack(true);
		pm.reboot(reason);
    }
    
    public void Extract_flash_tools() {
    	//flash_image
    	InputStream ifi = this.getResources().openRawResource (R.raw.flash_image);
    	byte[] fi_buffer = null;
    	try {
        	fi_buffer = new byte[ifi.available()];
			ifi.read(fi_buffer);
	    	ifi.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
    	FileOutputStream ofi;
    	try {
			ofi = this.openFileOutput("flash_image", Context.MODE_PRIVATE);
	    	ofi.write(fi_buffer);
	    	ofi.close();
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	//mkbootimg
    	InputStream imkb = this.getResources().openRawResource (R.raw.mkbootimg);
    	byte[] mkb_buffer = null;
		try {
			mkb_buffer = new byte[imkb.available()];
	    	imkb.read(mkb_buffer);
	    	imkb.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	FileOutputStream omkb;
		try {
			omkb = this.openFileOutput("mkbootimg", Context.MODE_PRIVATE);
	    	omkb.write(mkb_buffer);
	    	omkb.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//permissions
		Command set_perm = new Command(0, "chmod 0777 " + flash_image, "chmod 0777 " + mkbootimg)
    	{
    		@Override
    	    public void output(int id, String line) {
    			//...
    	    }
    	};
    	try {
			RootTools.getShell(true).add(set_perm).waitForFinish();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
    }
    
    public void flash(View view) {
    	String Sel_Boot = String.valueOf(x_boot_partitions.getSelectedItem());
    	String Sel_Rom = String.valueOf(rom_s_kernel.getSelectedItem());
    	String mkb_cmd = mkbootimg
    					+ " --kernel /sdcard/NativeSD/" + Sel_Rom + "/zImage"
						+ " --ramdisk /sdcard/NativeSD/" + Sel_Rom + "/initrd.gz"
						+ " --cmdline \"nand_boot=0 rel_path=" + Sel_Rom + "\""
						+ "	--base 0x11800000"
						+ " --output /sdcard/NativeSD/" + Sel_Boot + ".img";
    	String fi_cmd = flash_image
    					+ " " + Sel_Boot
    					+ " /sdcard/NativeSD/" + Sel_Boot + ".img";

    	Intent intent = new Intent(this, HelperActivity.class);
		intent.putExtra("mkbootimgCMD", mkb_cmd);
        intent.putExtra("flash_imageCMD", fi_cmd);
        intent.putExtra("img_file", Sel_Boot);
		this.startActivity(intent);
    }
    
    public void makeToast(String msg) {
        Context context = getApplicationContext();
        CharSequence text = msg;
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
