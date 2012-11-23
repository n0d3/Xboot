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

import android.R.color;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Button b1 = (Button)this.findViewById(R.id.button1); //boot
        Button b2 = (Button)this.findViewById(R.id.button2); //sboot
        Button b3 = (Button)this.findViewById(R.id.button3); //tboot
        Button b4 = (Button)this.findViewById(R.id.button4); //vboot
        Button b5 = (Button)this.findViewById(R.id.button5); //wboot
        Button b6 = (Button)this.findViewById(R.id.button6); //xboot
        Button b7 = (Button)this.findViewById(R.id.button7); //yboot
        Button b8 = (Button)this.findViewById(R.id.button8); //zboot
        Button b9 = (Button)this.findViewById(R.id.button9); //flash
        
        RootTools.debugMode = false;
        boot_part_selected = false;
        rom_kernel_selected = false;
        
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
        SharedPreferences XbootPreferences = getSharedPreferences("XbootPreferences", 0);
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
    	return false;
    }
    
    public void reboot_to_boot(View view) {
    	if (found_boot) {
    		ReBoot("oem-" + 0);
    	} else {
    		makeToast("boot partition not detected.");
    	}
    }
    
    public void reboot_to_sboot(View view) {
    	if (found_sboot) {
    		ReBoot("oem-" + 1);
    	} else {
    		makeToast("sboot partition not detected.");
    	}
    }
    
    public void reboot_to_tboot(View view) {
    	if (found_tboot) {
    		ReBoot("oem-" + 2);
    	} else {
    		makeToast("tboot partition not detected.");
    	}
    }

    public void reboot_to_vboot(View view) {
    	if (found_vboot) {
    		ReBoot("oem-" + 3);
    	} else {
    		makeToast("vboot partition not detected.");
    	}
    }

    public void reboot_to_wboot(View view) {
    	if (found_wboot) {
    		ReBoot("oem-" + 4);
    	} else {
    		makeToast("wboot partition not detected.");
    	}
    }

    public void reboot_to_xboot(View view) {
    	if (found_xboot) {
    		ReBoot("oem-" + 5);
    	} else {
    		makeToast("xboot partition not detected.");
    	}
    }

    public void reboot_to_yboot(View view) {
    	if (found_yboot) {
    		ReBoot("oem-" + 6);
    	} else {
    		makeToast("yboot partition not detected.");
    	}
    }

    public void reboot_to_zboot(View view) {
    	if (found_zboot) {
    		ReBoot("oem-" + 7);
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
    public String parse_rom_name(String mtd) {
    	s = "NAND";
    	Command get_name = new Command(0, "sed -rn '1,50 {/rel_path/ {P q}}' /dev/mtd/" + mtd)
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

    public void makeToast(String msg) {
        Context context = getApplicationContext();
        CharSequence text = msg;
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
