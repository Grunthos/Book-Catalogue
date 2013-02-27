package com.eleybourn.bookcatalogue.filechooser;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;

/**
 * Interface for different 'file' related services. Examples include:
 *  - Local file service
 *  - CIFS/SMB file service
 *  
 *  It is anticipated that a typical file selection dialog will have three phases:
 *  
 *  1. Select service
 *    - Local Files
 *    - Windows (CIFS) Share
 *    - (later) Dropbox
 *    
 *  2. Inside service, list of options will include:
 *    - last save location, or default location, or root
 *    - [list of favourites...]
 *    
 *    For local file service this might be:
 *      "/sdcard/bookCatalogue" <- last save
 *      "/sdcard/backups/bc"
 *      "/sdcard/ext1/backups/bc"
 *      
 *    For CIFS it might be:
 *      "//server-name/share/backups/bc" <-- Last save
 *      "//server-name2/share2"
 *      "//server-name3/share3"
 *      [Add Server]
 *      
 *      (first time, this would just have [Add Server] in list)
 * 
 * @author pjw
 */
public interface FileService {
	
	public interface OnServerAddedListener {
		public void onServerAdded(FileSnapshot resolvedServer);
	}

	public interface OnServerPreparedListener {
		public void onServerPrepared(FileSnapshot requestedServer, FileSnapshot resolvedServer);
	}

	public String getName(); // eg. "Local Files"; "CIFS/SMB/Windows"
	public boolean canAdd(); // can add new services; 

	/**
	 * Get a list of server connections to display to user
	 * 
	 * @return
	 */
	public ArrayList<FileSnapshot> getFavourites(String defaultPath, String defaultFileName) throws IOException;

	/** 
	 * Present the user with a setup dialog to add a new server and when done fire the
	 * onServerAdded event.
	 * Local files can throw an exception.
	 * For CIFS it should ask for host/domain/password.
	 *
	 * @param parent
	 */
	public void addServer(Activity parent);
	/**
	 * Use the requested server to do whatever is necessary to actually connect to the server
	 * (eg. display a password dialog), then fire the OnServerPreparedListener() event.
	 * 
	 * For local files, it can just be a pass-through.
	 * For CIFS, it might popup the server details and ask for a password.
	 * 
	 * @param parent			Activity calling this action
	 * @param requestedServer	The server we want to access
	 */
	public void prepareServer(Activity parent, FileSnapshot requestedServer);

	/**
	 * So it appears nice in lists
	 *
	 * @return
	 */
	public String toString();
}
