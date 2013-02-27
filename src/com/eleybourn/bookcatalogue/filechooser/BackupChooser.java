/*
 * @copyright 2013 Philip Warner
 * @license GNU General Public License
 * 
 * This file is part of Book Catalogue.
 *
 * Book Catalogue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Book Catalogue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Book Catalogue.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import jcifs.Config;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import android.os.Bundle;
import android.widget.Toast;

import com.eleybourn.bookcatalogue.BookCatalogueApp;
import com.eleybourn.bookcatalogue.BookCataloguePreferences;
import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.backup.BackupManager;
import com.eleybourn.bookcatalogue.backup.BackupManager.OnBackupCompleteListener;
import com.eleybourn.bookcatalogue.dialogs.MessageDialogFragment;
import com.eleybourn.bookcatalogue.dialogs.MessageDialogFragment.OnMessageDialogResultListener;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTask;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.utils.StorageUtils;
import com.eleybourn.bookcatalogue.utils.Utils;

/**
 * FileChooser activity to choose an archive file to open/save
 * 
 * @author pjw
 */
public class BackupChooser extends FileChooser implements OnMessageDialogResultListener, OnBackupCompleteListener {
	/** The backup file that will be created (if saving) */
	//private FileWrapper mBackupFile = null;
	/** Used when saving state */
	private final static String STATE_BACKUP_FILE = "BackupFileSpec";
	
	private static final int TASK_ID_SAVE = 1;
	private static final int TASK_ID_OPEN = 2;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the correct title
		if (isSaveDialog()) {
			this.setTitle(R.string.backup_to_archive);
		} else {
			this.setTitle(R.string.import_from_archive);			
		}

		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_BACKUP_FILE)) {
			//mBackupFile = savedInstanceState.getParcelable(STATE_BACKUP_FILE);
		}
	}

	/**
	 * Setup the default file name: blank for 'open', date-based for save
	 * 
	 * @return
	 */
	private String getDefaultFileName() {
    	if (isSaveDialog()) {
    		final String sqlDate = Utils.toLocalSqlDateOnly(new Date());
    		return "BookCatalogue-" + sqlDate.replace(" ", "-").replace(":", "") + ".bcbk";
    	} else {
    		return "";
    	}
	}

	/**
	 * Create the fragment using the last backup for the path, and the default file name (if saving)
	 * @throws IOException 
	 */
	@Override
	protected FileChooserFragment getChooserFragment() throws IOException {
		BookCataloguePreferences prefs = BookCatalogueApp.getAppPreferences();
//		String lastBackup = prefs.getString(BookCataloguePreferences.PREF_LAST_BACKUP_FILE, StorageUtils.getSharedStoragePath());
//		return FileChooserFragment.newInstance(new LocalFileWrapper(new File(lastBackup)), getDefaultFileName());
//		jcifs.Config.setProperty( "jcifs.netbios.wins", "10.0.0.20" );
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("albatross", "pjw", "PASSWORD");
		Config.setProperty("jcifs.smb.client.snd_buf_size", "60416"); 
	    Config.setProperty("jcifs.smb.client.rcv_buf_size", "60416"); 
	    Config.setProperty("jcifs.smb.client.dfs.disabled", "true"); 
	    //Config.setProperty("jcifs.resolveOrder", "DNS"); 

		//String lastBackup = prefs.getString(BookCataloguePreferences.PREF_LAST_BACKUP_FILE, StorageUtils.getSharedStoragePath());
		//CifsFileWrapper root = new CifsFileWrapper(new SmbFile("smb://thoth.local.rime.com.au/multimedia/", auth), auth);
		CifsFileWrapper root = new CifsFileWrapper(new SmbFile("smb://10.0.0.142/", auth), auth);
		return FileChooserFragment.newInstance(new FileSnapshot(root), getDefaultFileName());
	}

	/**
	 * Get a task suited to building a list of backup files.
	 */
	@Override
	public FileLister getFileLister(FileSnapshot root) {
		return new BackupLister(root);
	}

	/**
	 * Save the state
	 */
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// We need the backup file, if set
		//if (mBackupFile != null) {
		//	outState.putSerializable(STATE_BACKUP_FILE, mBackupFile);
		//}
	}

	/**
	 * If a file was selected, restore the archive.
	 */
	@Override
	public void onOpen(FileSnapshot file) {
		BackupManager.restoreCatalogue(this, file, TASK_ID_OPEN);		
	}

	/**
	 * If a file was selected, save the archive.
	 */
	@Override
	public void onSave(FileSnapshot file) {
		try {
			BackupManager.backupCatalogue(this, file, TASK_ID_SAVE);
		} catch (IOException e) {
			Logger.logError(e);
			Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_LONG).show();
			return;
		}
	}

	@Override
	public void onTaskFinished(SimpleTaskQueueProgressFragment fragment, int taskId, boolean success, boolean cancelled, FragmentTask task) {
		// Is it a task we care about?
		if (taskId == TASK_ID_SAVE) {
		} else if (taskId == TASK_ID_OPEN) {
			if (!success) {
				String msg = getString(R.string.import_failed)
						+ " " + getString(R.string.please_check_sd_readable)
						+ "\n\n" + getString(R.string.if_the_problem_persists);

				MessageDialogFragment frag = MessageDialogFragment.newInstance(0, R.string.import_from_archive, msg, R.string.ok, 0, 0);
				frag.show(getSupportFragmentManager(), null);
				// Just return; user may want to try again
				return;
			}
			if (cancelled) {
				// Just return; user may want to try again
				return;
			}

			MessageDialogFragment frag = MessageDialogFragment.newInstance(TASK_ID_OPEN, R.string.import_from_archive, R.string.import_complete, R.string.ok, 0, 0);
			frag.show(getSupportFragmentManager(), null);

		}
	}

	@Override
	public void onAllTasksFinished(SimpleTaskQueueProgressFragment fragment, int taskId, boolean success, boolean cancelled) {
		// Nothing to do here; we really only care when backup tasks finish, and there's only ever one task
	}

	@Override
	public void onMessageDialogResult(int dialogId, MessageDialogFragment dialog, int button) {
		switch(dialogId) {
		case 0:
			// Do nothing.
			// Our dialogs with ID 0 are only 'FYI' type; 
			break;
		case TASK_ID_OPEN:
		case TASK_ID_SAVE:
			finish();
			break;
		}
	}

	@Override
	public void onBackupComplete(FileSnapshot file, boolean success, boolean cancelled) {
		if (!success) {
			String msg = getString(R.string.backup_failed)
					+ " " + getString(R.string.please_check_sd_writable)
					+ "\n\n" + getString(R.string.if_the_problem_persists);

			MessageDialogFragment frag = MessageDialogFragment.newInstance(0, R.string.backup_to_archive, msg, R.string.ok, 0, 0);
			frag.show(getSupportFragmentManager(), null);
			// Just return; user may want to try again
			return;
		}
		if (cancelled) {
			// Just return; user may want to try again
			return;
		}
		// Show a helpful message
		String msg = getString(R.string.archive_complete_details, file.getParentPathPretty(), file.getName(), Utils.formatFileSize(file.getLength()));

		MessageDialogFragment frag = MessageDialogFragment.newInstance(TASK_ID_SAVE, R.string.backup_to_archive, msg, R.string.ok, 0, 0);
		frag.show(getSupportFragmentManager(), null);
	}

}
