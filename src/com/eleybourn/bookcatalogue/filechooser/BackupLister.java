package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

import android.app.Activity;

import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.backup.BackupManager;
import com.eleybourn.bookcatalogue.backup.BackupReader;
import com.eleybourn.bookcatalogue.filechooser.FileLister.FileListerListener;
import com.eleybourn.bookcatalogue.filechooser.FileWrapper.FileWrapperFilter;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueue.SimpleTaskContext;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTask;

/**
 * Object to provide a FileLister specific to archive files.
 * 
 * @author pjw
 */
public class BackupLister extends FileLister {
	/** Pattern to match an archive file spec */
	private static Pattern mBackupFilePattern = Pattern.compile(".bcbk$", Pattern.CASE_INSENSITIVE);

	/**
	 * Constructor
	 * 
	 * @param root
	 */
	public BackupLister(FileSnapshot root) {
		super(root);
	}

	/**
	 * Construct a file filter to select only directories and backup files.
	 */
	private FileWrapperFilter mFilter = new FileWrapperFilter() {
		@Override
		public boolean accept(FileWrapper f) {
			try {
				return (f.isDirectory() && f.canWrite()) || (f.isFile() && mBackupFilePattern.matcher(f.getName()).find());
			} catch (IOException e) {
				Logger.logError(e);
				return false;
			}
		}
	};

	/**
	 * Get the file filter we constructed
	 */
	protected FileWrapperFilter getFilter() {
		return mFilter;
	}

	/**
	 * Process an array of Files into an ArrayList of BackupFileDetails
	 * @throws IOException 
	 */
	protected ArrayList<FileListItem> processList(FileWrapper[] files) throws IOException {
		ArrayList<FileListItem> dirs = new ArrayList<FileListItem>();

		if (files != null) {
			for (FileWrapper f : files) {
				BackupFileDetails fd = new BackupFileDetails(new FileSnapshot(this.getRoot(), f));
				dirs.add(fd);
				String fileName = null;
				try {
					fileName = f.getName();
				} catch (IOException e2) {
					Logger.logError(e2);
				}
				if (fileName != null && fileName.toUpperCase().endsWith(".BCBK")) {
					BackupReader reader = null;
					try {
						reader = BackupManager.readBackup(f);
						fd.setInfo(reader.getInfo());
						reader.close();
					} catch (IOException e) {
						Logger.logError(e);
						if (reader != null)
							try { reader.close(); } catch (IOException e1) { }
					}
				}
			}
		}
		return dirs;
	}

}
