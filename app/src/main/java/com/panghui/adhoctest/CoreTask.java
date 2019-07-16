/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Seth Lemons.
 */

package com.panghui.adhoctest;

//import java.io.BufferedInputStream;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class CoreTask {

	public static final String MSG_TAG = "CoreTask";
	
	public String DATA_FILE_PATH;
	
	private static final String FILESET_VERSION = "6";
	
	public void setPath(String path){
		this.DATA_FILE_PATH = path;
	}

    public void chmodBin(List<String> filenames) throws Exception {
        Process process = null;
		process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
    	for (String tmpFilename : filenames) {
    		os.writeBytes("chmod 4755 "+this.DATA_FILE_PATH+"/bin/"+tmpFilename+"\n");
    	}
    	os.writeBytes("exit\n");
        os.flush();
        os.close();
        process.waitFor();
    }

    public boolean hasRootPermission() {
    	Process process = null;
    	DataOutputStream os = null;
    	boolean rooted = true;
		try {
			process = Runtime.getRuntime().exec("su");
	        os = new DataOutputStream(process.getOutputStream());
	        os.writeBytes("exit\n");
	        os.flush();	        
	        process.waitFor();
	        Log.d(MSG_TAG, "Exit-Value ==> "+process.exitValue());
	        if (process.exitValue() != 0) {
	        	rooted = false;
	        }
		} catch (Exception e) {
			Log.d(MSG_TAG, "Can't obtain root - Here is what I know: "+e.getMessage());
			rooted = false;
		}
		finally {
			if (os != null) {
				try {
					os.close();
					process.destroy();
				} catch (Exception e) {
					// nothing
				}
			}
		}
		return rooted;
    }
    
    public boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
	        os = new DataOutputStream(process.getOutputStream());
	        Log.d("COMMAND", "cd "+CoreTask.this.DATA_FILE_PATH+"/bin");
	        os.writeBytes(command+"\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        process.waitFor();
		} catch (Exception e) {
			Log.d(MSG_TAG, "Unexpected error - Here is what I know: "+e.getMessage());
			return false;
		}
		finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
    }

    public String getProp(String property) {
        String result = null;
    	Process process = null;
        BufferedReader br = null;
        try {
			process = Runtime.getRuntime().exec("getprop "+property);
        	br = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		String s = br.readLine();
	    	while (s != null){
	    		result = s;
	    		s = br.readLine();
	    	}
	    	process.waitFor();
		} catch (Exception e) {
			Log.d(MSG_TAG, "Unexpected error - Here is what I know: "+e.getMessage());
		}
		finally {
			try {
				if (br != null) {
					br.close();
				}
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return result;
    }


    public boolean filesetOutdated(){
    	boolean outdated = true;
    	InputStream is = null;
    	BufferedReader br = null;
    	File inFile = new File(this.DATA_FILE_PATH+"/bin/netcontrol");
    	try{
        	is = new FileInputStream(inFile);
        	br = new BufferedReader(new InputStreamReader(is));
    		String s = br.readLine();
    		int linecount = 0;
	    	while (s!=null){
	    		if (s.contains("@Version")){
	    			String instVersion = s.split("=")[1];
	    			if (instVersion != null && FILESET_VERSION.equals(instVersion.trim()) == true) {
	    				outdated = false;
	    			}
	    			break;
	    		}
	    		linecount++;
	    		if (linecount > 1) {
	    			break;
	    		}
	    		s = br.readLine();
	    	}
    	}
    	catch (Exception e){
    		Log.d(MSG_TAG, "Unexpected error - Here is what I know: "+e.getMessage());
    		outdated = true;
    	}
    	finally {
   			try {
   				if (is != null)
   					is.close();
   				if (br != null)
   					br.close();
   			} catch (Exception e) {
				// nothing
			}
    	}
    	return outdated;
    }
    

}
