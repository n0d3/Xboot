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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import android.R.color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
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
	private final int ID_MENU_UPD = 5;
	
	PowerManager pm;
	
	Spinner x_boot_partitions;
	Spinner rom_s_kernel;
	
	String flash_image = "/data/data/com.clk.xboot/files/flash_image";
	String mkbootimg = "/data/data/com.clk.xboot/files/mkbootimg";
	
	String thisRom;
	
	boolean found_boot = false;
	boolean found_sboot = false;
	boolean found_tboot = false;
	boolean found_vboot = false;
	boolean found_wboot = false;
	boolean found_xboot = false;
	boolean found_yboot = false;
	boolean found_zboot = false;	

	boolean valid_boot = false;
	boolean valid_sboot = false;
	boolean valid_tboot = false;
	boolean valid_vboot = false;
	boolean valid_wboot = false;
	boolean valid_xboot = false;
	boolean valid_yboot = false;
	boolean valid_zboot = false;
	
	String boot_mtd = "";
	String sboot_mtd = "";
	String tboot_mtd = "";
	String vboot_mtd = "";
	String wboot_mtd = "";
	String xboot_mtd = "";
	String yboot_mtd = "";
	String zboot_mtd = "";
	
	boolean boot_part_selected;
	boolean rom_kernel_selected;
	
	boolean clk = false;
	int clk_ver;
	
	PendingIntent RESTART_INTENT;
	SharedPreferences XbootPreferences;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        RESTART_INTENT = PendingIntent.getActivity(this.getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags());
        
        Button b1 = (Button)this.findViewById(R.id.button1); //boot
        Button b2 = (Button)this.findViewById(R.id.button2); //sboot
        Button b3 = (Button)this.findViewById(R.id.button3); //tboot
        Button b4 = (Button)this.findViewById(R.id.button4); //vboot
        Button b5 = (Button)this.findViewById(R.id.button5); //wboot
        Button b6 = (Button)this.findViewById(R.id.button6); //xboot
        Button b7 = (Button)this.findViewById(R.id.button7); //yboot
        Button b8 = (Button)this.findViewById(R.id.button8); //zboot
        Button b9 = (Button)this.findViewById(R.id.button9); //flash
        
        RootTools.debugMode = true;
        boot_part_selected = false;
        rom_kernel_selected = false;
        
        XbootPreferences = getSharedPreferences("XbootPreferences", 0);
        
        // Root?
        if (RootTools.isRootAvailable()) {
        	if (RootTools.isAccessGiven()) {
	        		//Check bootloader
	        		clk = false;
	        		Command get_bldr = new Command(0, "sed -n 's/clk/&/p' /proc/cmdline")
	            	{
	        			@Override
	        	        public void output(int id, String line)
	        	        {
	        				if(line.contains("clk=")) {
	        					clk = true;
	        					String ver = line.substring(line.indexOf("clk=") + 4, line.length()).trim().replace(".", "");
	        					if(ver.length() == 2) {
	        						ver = ver + "00";
	        					}
	        					else if(ver.length() == 3) {
	        						ver = ver + "0";
	        					}
	        					clk_ver = Integer.decode(ver);
	        				}
	        	        }
	            	};
	        		try {
	    				RootTools.getShell(true).add(get_bldr).waitForFinish();
	    			} catch (InterruptedException e) {
	    				e.printStackTrace();
	    			} catch (IOException e) {
	    				e.printStackTrace();
	    			} catch (TimeoutException e) {
	    				e.printStackTrace();
	    			}
	        		if(clk) {
	        			if(clk_ver < 1515) {
	        				AlertDialog.Builder WrongVersion = new AlertDialog.Builder(this);
	        				WrongVersion.setMessage("Current version of cLK is outdated!\nXboot is compatible with version higher than 1.5.1.5");
	        				WrongVersion.setPositiveButton("Exit",
	                								new DialogInterface.OnClickListener() {public void onClick(DialogInterface arg0, int arg1) {finish();}}
	                								);
	        				WrongVersion.show();
	        			}
	        		} else {
	        			AlertDialog.Builder NotCLK = new AlertDialog.Builder(this);
	        			NotCLK.setMessage("Xboot is compatible ONLY with cLK bootloader");
	        			NotCLK.setPositiveButton("Exit",
	            								new DialogInterface.OnClickListener() {public void onClick(DialogInterface arg0, int arg1) {finish();}}
	            								);
	        			NotCLK.show();
	        		}
        		//Check which of the available extra boot partitions are present
        		Command check_mtd = new Command(0, "cat proc/mtd")
            	{
            	        @Override
            	        public void output(int id, String line)
            	        {
            	        	if (line.contains("\"boot\"")) {
            	        		found_boot = true;
            	        		boot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"sboot\"")) {
            	        		found_sboot = true;
            	        		sboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"tboot\"")) {
            	        		found_tboot = true;
            	        		tboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"vboot\"")) {
            	        		found_vboot = true;
            	        		vboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"wboot\"")) {
            	        		found_wboot = true;
            	        		wboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"xboot\"")) {
            	        		found_xboot = true;
            	        		xboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"yboot\"")) {
            	        		found_yboot = true;
            	        		yboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        	if (line.contains("\"zboot\"")) {
            	        		found_zboot = true;
            	        		zboot_mtd = line.substring(0, line.indexOf(":"));
            	        	} 
            	        }
            	};
            	try {
    				RootTools.getShell(true).add(check_mtd).waitForFinish();
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
        
        //Current Rom
        thisRom = XbootPreferences.getString("currentRom", null);
        if(thisRom == null) {
        	get_running_rom();
        	SharedPreferences.Editor XbootEditor = XbootPreferences.edit();
        	XbootEditor.putString("currentRom", thisRom);
        	XbootEditor.commit();
        }
        
        // Tools
        File fi = this.getFileStreamPath("flash_image");
        File mkb = this.getFileStreamPath("mkbootimg");
        if(!(fi.exists() && mkb.exists())) {
        	Extract_flash_tools();
        }        
        
        // boot partitions buttons and list
        ArrayList<String> xtraboot = new ArrayList<String>();
        xtraboot.add("");
        if(found_boot) {
        	xtraboot.add("boot");
        	String boot_rom = parse_rom_name(boot_mtd).trim();
        	if(boot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b1.setBackgroundColor(color.darker_gray);
        	}        	
    		b1.setText("boot (" + boot_mtd + ")\n[" + boot_rom + "]"); 
    		if(boot_rom.compareToIgnoreCase("null") == 0) {
        		valid_boot = false;
        	} else {
        		valid_boot = true;
        	}
        } else {
    		b1.setText("boot (----)\n[----]");
        }
        if(found_sboot) {
        	xtraboot.add("sboot");
        	String sboot_rom = parse_rom_name(sboot_mtd).trim();
        	if(sboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b2.setBackgroundColor(color.darker_gray);
        	}
    		b2.setText("sboot (" + sboot_mtd + ")\n[" + sboot_rom + "]");
    		if(sboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_sboot = false;
	       	} else {
	       		valid_sboot = true;
	       	}
        } else {
    		b2.setText("sboot (----)\n[----]");
        }
        if(found_tboot) {
        	xtraboot.add("tboot");
        	String tboot_rom = parse_rom_name(tboot_mtd).trim();
        	if(tboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b3.setBackgroundColor(color.darker_gray);
        	}        	
    		b3.setText("tboot (" + tboot_mtd + ")\n[" + parse_rom_name(tboot_mtd) + "]");
   			if(tboot_rom.compareToIgnoreCase("null") == 0) {
   				valid_tboot = false;
	       	} else {
	       		valid_tboot = true;
	       	}
        } else {
    		b3.setText("tboot (----)\n[----]");
        }
        if(found_vboot) {
        	xtraboot.add("vboot");
        	String vboot_rom = parse_rom_name(vboot_mtd).trim();
        	if(vboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b4.setBackgroundColor(color.darker_gray);
        	}
    		b4.setText("vboot (" + vboot_mtd + ")\n[" + parse_rom_name(vboot_mtd) + "]");
    		if(vboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_vboot = false;
        	} else {
        		valid_vboot = true;
        	}
        } else {
    		b4.setText("vboot (----)\n[----]");
        }
        if(found_wboot) {
        	xtraboot.add("wboot");
        	String wboot_rom = parse_rom_name(wboot_mtd).trim();
        	if(wboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b5.setBackgroundColor(color.darker_gray);
        	}
    		b5.setText("wboot (" + wboot_mtd + ")\n[" + parse_rom_name(wboot_mtd) + "]");
    		if(wboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_wboot = false;
	       	} else {
	       		valid_wboot = true;
	       	}
        } else {
    		b5.setText("wboot (----)\n[----]");
        }
        if(found_xboot) {
        	xtraboot.add("xboot");
        	String xboot_rom = parse_rom_name(xboot_mtd).trim();
        	if(xboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b6.setBackgroundColor(color.darker_gray);
        	}
    		b6.setText("xboot (" + xboot_mtd + ")\n[" + parse_rom_name(xboot_mtd) + "]");
    		if(xboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_xboot = false;
	       	} else {
	       		valid_xboot = true;
	       	}
        } else {
    		b6.setText("xboot (----)\n[----]");
        }
        if(found_yboot) {
        	xtraboot.add("yboot");
        	String yboot_rom = parse_rom_name(yboot_mtd).trim();
        	if(yboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b7.setBackgroundColor(color.darker_gray);
        	}
    		b7.setText("yboot (" + yboot_mtd + ")\n[" + parse_rom_name(yboot_mtd) + "]");
    		if(yboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_yboot = false;
	       	} else {
	       		valid_yboot = true;
	       	}
        } else {
    		b7.setText("yboot (----)\n[----]");
        }
        if(found_zboot) {
        	xtraboot.add("zboot");
        	String zboot_rom = parse_rom_name(zboot_mtd).trim();
        	if(zboot_rom.compareToIgnoreCase(thisRom) == 0) {
        		b8.setBackgroundColor(color.darker_gray);
        	}
    		b8.setText("zboot (" + zboot_mtd + ")\n[" + parse_rom_name(zboot_mtd) + "]");
    		if(zboot_rom.compareToIgnoreCase("null") == 0) {
	       		valid_zboot = false;
	       	} else {
	       		valid_zboot = true;
	       	}
        } else {
    		b8.setText("zboot (----)\n[----]");
        }
        x_boot_partitions = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, xtraboot);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        x_boot_partitions.setAdapter(adapter1);
        x_boot_partitions.setOnItemSelectedListener(
        	new OnItemSelectedListener() {
        		@SuppressLint("NewApi")
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                	if (arg0.getItemAtPosition(arg2).toString().isEmpty()) {
                		boot_part_selected = false;
                    } else {
                    	boot_part_selected = true;
                    }
            	}
                public void onNothingSelected(AdapterView<?> arg0) {
                	boot_part_selected = false;
                }
        	}
        );
        
        // list of NativeSD Roms
    	ArrayList<String> Roms = new ArrayList<String>();
    	Roms.add("");
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
        	 rom_s_kernel.setOnItemSelectedListener(
        	 	new OnItemSelectedListener() {
        	    	@SuppressLint("NewApi")
					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        	    		if (arg0.getItemAtPosition(arg2).toString().isEmpty()) {
        	    			rom_kernel_selected = false;
        	           	} else {
        	           		rom_kernel_selected = true;
        	            }
        	    	}
        	        public void onNothingSelected(AdapterView<?> arg0) {
        	        	rom_kernel_selected = false;
        	        }
        		}
        	);
        } else {
        	 makeToast("No NativeSD folder found!");
        	 b9.setClickable(false);
        }
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        makeToast("Loaded rom: " + thisRom);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, ID_MENU_INF, Menu.NONE, R.string.menu_item_0);
    	menu.add(Menu.NONE, ID_MENU_REC, Menu.NONE, R.string.menu_item_1);
    	menu.add(Menu.NONE, ID_MENU_BLDR, Menu.NONE, R.string.menu_item_2);
    	menu.add(Menu.NONE, ID_MENU_RST, Menu.NONE, R.string.menu_item_3);
    	menu.add(Menu.NONE, ID_MENU_EXIT, Menu.NONE, R.string.menu_item_4);
    	menu.add(Menu.NONE, ID_MENU_UPD, Menu.NONE, R.string.menu_item_5);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == ID_MENU_INF)	{
    		final TextView link = new TextView(this);
         	final SpannableString s = new SpannableString(getText(R.string.Info) + "\nMore info at xda thread:\nforum.xda-developers.com/showthread.php?t=2001686");
        	Linkify.addLinks(s, Linkify.WEB_URLS);
        	link.setText(s);
        	link.setMovementMethod(LinkMovementMethod.getInstance());
        	
    		AlertDialog.Builder info = new AlertDialog.Builder(this);
    		info.setInverseBackgroundForced(true);
    		info.setTitle("Info");
    		info.setIcon(android.R.drawable.ic_dialog_info);
    		info.setView(link);
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
    	else if(item.getItemId() == ID_MENU_UPD) {
    		updatelk();
    		return true;
    	}
    	
    	return false;
    }
    
    public void reboot_to_boot(View view) {
    	if (found_boot) {
    		if (valid_boot) {
    			ReBoot("oem-" + 0);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("boot partition not detected.");
    	}
    }
    
    public void reboot_to_sboot(View view) {
    	if (found_sboot) {
    		if (valid_sboot) {
    			ReBoot("oem-" + 1);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("sboot partition not detected.");
    	}
    }
    
    public void reboot_to_tboot(View view) {
    	if (found_tboot) {
    		if (valid_tboot) {
    			ReBoot("oem-" + 2);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("tboot partition not detected.");
    	}
    }

    public void reboot_to_vboot(View view) {
    	if (found_vboot) {
    		if (valid_vboot) {
    			ReBoot("oem-" + 3);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("vboot partition not detected.");
    	}
    }

    public void reboot_to_wboot(View view) {
    	if (found_wboot) {
    		if (valid_wboot) {
    			ReBoot("oem-" + 4);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("wboot partition not detected.");
    	}
    }

    public void reboot_to_xboot(View view) {
    	if (found_xboot) {
    		if (valid_xboot) {
    			ReBoot("oem-" + 5);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("xboot partition not detected.");
    	}
    }

    public void reboot_to_yboot(View view) {
    	if (found_yboot) {
    		if (valid_vboot) {
    			ReBoot("oem-" + 6);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("yboot partition not detected.");
    	}
    }

    public void reboot_to_zboot(View view) {
    	if (found_zboot) {
    		if (valid_zboot) {
    			ReBoot("oem-" + 7);
    		} else {
    			makeToast("invalid image header.");
    		}
    	} else {
    		makeToast("zboot partition not detected.");    		
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
    
    String s;
    boolean hdr_is_valid;
    public String parse_rom_name(String mtd) {
    	s = "NAND";
    	hdr_is_valid = false;
    	Command check_header = new Command(0, "sed '1 q' /dev/mtd/" + mtd)
    	{
    		@Override
    	    public void output(int id, String line) {
    			if (line.contains("ANDROID!")) {
    				hdr_is_valid = true;
    				this.terminate(null);
    			}
    	    }
    	};
    	try {
			RootTools.getShell(true).add(check_header).waitForFinish();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
    	if(hdr_is_valid) {
	    	Command get_name = new Command(0, "sed '40 q' /dev/mtd/" + mtd)
	    	{
	    		@Override
	    	    public void output(int id, String line) {
	    			if (line.contains("rel_path")) {
	    				s = line.substring(line.indexOf("rel_path") + 9, line.length()).trim();
	    				this.terminate(null);
	    			}
	    	    }
	    	};
	    	try {
				RootTools.getShell(true).add(get_name).waitForFinish();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
    	} else {
    		s = "null";
    	}
    	return s;
    }    
    
    public void get_running_rom() {
    	thisRom = "NAND";
    	Command get_name = new Command(0, "sed -rn '/system/ {{s!/system.*!!} {s!.*/!!} p}' /proc/self/mountinfo")
    	{
    		@SuppressLint("NewApi")
			@Override
    	    public void output(int id, String line) {
    			if (!line.isEmpty()) {
    				if(line.length() < 2) {
    					thisRom = "NAND";
    				} else {
    					thisRom = line.trim();
    				}
    				this.terminate(null);
    			}
    	    }
    	};
    	try {
			RootTools.getShell(true).add(get_name).waitForFinish();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
    }
    
    public void flash(View view) {
    	if(boot_part_selected && rom_kernel_selected) {
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
    	} else {
    		makeToast("No Rom or partition selection made.");
    	}
    }   

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
    
    public void updatelk() {
    	boolean update = false;
    	if(isNetworkAvailable()) {
    		update = checkLink("https://github.com/n0d3/lk-img/raw/master/version.txt");
    		if(update) {
    			downloadToSD("https://github.com/n0d3/lk-img/raw/master/lk.img");
    			flashlk();
    		}
    	} else {
    		makeToast("No network available.");
    	}
    }
    
    public boolean checkLink(String link) {
    	String result = null;
    	Integer lk_update;
    	try {
			URL url = new URL(link);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			InputStream in = urlConnection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			result = line;
			while((line=reader.readLine())!=null){
			    result+=line;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
    		makeToast(e.toString());
		} catch (IOException e) {
			e.printStackTrace();
    		makeToast(e.toString());
		}
    	if(result!=null) {
    		if(result.length() == 2) result = result + "00";
    		else if(result.length() == 3) result = result + "0";
    		
    		lk_update = Integer.decode(result);
        	if(lk_update > clk_ver) {
        		makeToast("Version " + result + " will be downloaded and installed.");
        		return true;
        	} else {
        		makeToast("No update found.Latest version is "  + result.substring(0,1) 
        														+ "." + result.substring(1,2) + "."
        														 + result.substring(2,3) + "."
        														 + result.substring(3,4));
        		return false;
        	}
    	}
    	return false;    	
    }
    
    public void downloadToSD(String link) {
    	try {
			//set the download URL, a url that points to a file on the internet
			//this is the file to be downloaded
			URL url = new URL(link);

			//create the new connection
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			//set up some things on the connection
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);

			//and connect!
			urlConnection.connect();

			//set the path where we want to save the file
			//in this case, going to save it on the root directory of the
			//sd card.
			File SDCardRoot = Environment.getExternalStorageDirectory();
			//create a new file, specifying the path, and the filename
			//which we want to save the file as.
			File file = new File(SDCardRoot,"lk.img");

			//this will be used to write the downloaded data into the file we created
			FileOutputStream fileOutput = new FileOutputStream(file);

			//this will be used in reading the data from the internet
			InputStream inputStream = urlConnection.getInputStream();

			//this is the total size of the file
			@SuppressWarnings("unused")
			int totalSize = urlConnection.getContentLength();
			//variable to store total downloaded bytes
			@SuppressWarnings("unused")
			int downloadedSize = 0;

			//create a buffer...
			byte[] buffer = new byte[1024];
			int bufferLength = 0; //used to store a temporary size of the buffer

			//now, read through the input buffer and write the contents to the file
			while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
				//add the data in the buffer to the file in the file output stream (the file on the sd card
				fileOutput.write(buffer, 0, bufferLength);
				//add up the size so we know how much is downloaded
				downloadedSize += bufferLength;
				//this is where you would do something to report the prgress, like this maybe
				//updateProgress(downloadedSize, totalSize);
			}
			//close the output stream when done
			fileOutput.close();

		//catch some possible errors...
		} catch (MalformedURLException e) {
			e.printStackTrace();
    		makeToast(e.toString());
		} catch (IOException e) {
			e.printStackTrace();
    		makeToast(e.toString());
		}
    }
    
    public void flashlk() {
    	String mkb_cmd = "echo Updating cLK...";
    	String fi_cmd = flash_image	+ " lk /sdcard/lk.img";
    	Intent intent = new Intent(this, HelperActivity.class);
		intent.putExtra("mkbootimgCMD", mkb_cmd);
        intent.putExtra("flash_imageCMD", fi_cmd);   
        intent.putExtra("img_file", "");
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
