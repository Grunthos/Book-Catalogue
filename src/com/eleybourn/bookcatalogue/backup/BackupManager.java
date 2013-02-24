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
package com.eleybourn.bookcatalogue.backup;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.eleybourn.bookcatalogue.BookCatalogueApp;
import com.eleybourn.bookcatalogue.BookCataloguePreferences;
import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.backup.BackupReader.BackupReaderListener;
import com.eleybourn.bookcatalogue.backup.BackupWriter.BackupWriterListener;
import com.eleybourn.bookcatalogue.backup.tar.TarBackupContainer;
import com.eleybourn.bookcatalogue.compat.BookCatalogueActivity;
import com.eleybourn.bookcatalogue.filechooser.FileSnapshot;
import com.eleybourn.bookcatalogue.filechooser.FileWrapper;
import com.eleybourn.bookcatalogue.filechooser.LocalFileWrapper;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueue.SimpleTaskContext;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTask;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTaskAbstract;
import com.eleybourn.bookcatalogue.utils.Utils;

/**
 * Class for public static methods relating to backup/restore
 * 
 * @author pjw
 */
public class BackupManager {

	public interface OnBackupCompleteListener {
		public void onBackupComplete(FileSnapshot file, boolean success, boolean cancelled);
	}
	/**
	 * Create a BackupReader for the specified file.
	 * 
	 * @param file	File to read
	 * 
	 * @return	a new reader
	 * 
	 * @throws IOException (inaccessible, invalid other other errors)
	 */
	public static BackupReader readBackup(FileWrapper file) throws IOException {
		if (!file.exists())
			throw new java.io.FileNotFoundException("Attempt to open non-existent backup file");
		
		// We only support one backup format; so we use that. In future we would need to 
		// explore the file to determine which format to use
		TarBackupContainer bkp = new TarBackupContainer(file);
		// Each format should provide a validator of some kind
		if (!bkp.isValid())
			throw new IOException("Not a valid backup file");

		return bkp.newReader();
	}

	/**
	 * Esnure the file name extension is what we want
	 * @throws IOException 
	 */
	private static FileSnapshot cleanupFile(FileSnapshot requestedFile) throws IOException {
		if (!requestedFile.getName().toUpperCase().endsWith(".BCBK")) {
			return requestedFile.getParentFile().getChild(requestedFile.getName() + ".bcbk");
		} else {
			return requestedFile;
		}

	}

	/**
	 * Start a foreground task that backs up the entire catalogue.
	 * 
	 * We use a FragmentTask so that long actions do not occur in the UI thread.
	 * @throws IOException 
	 */
	public static void backupCatalogue(final BookCatalogueActivity context, final FileSnapshot requestedFile, int taskId) throws IOException {
		//final FileWrapper tempFile = resultingFile.getParentFile().getChild(resultingFile.getName() + ".tmp");

		FragmentTask task = new FragmentTaskAbstract() {
			FileSnapshot resultingFile;
			FileWrapper tempFile;

			private boolean mBackupOk = false;
			private String mBackupDate = Utils.toSqlDateTime(new Date());

			@Override
			public void run(final SimpleTaskQueueProgressFragment fragment, SimpleTaskContext taskContext) throws IOException {
				BackupWriter wrt = null;

				try {
					resultingFile = cleanupFile(requestedFile);
					tempFile = requestedFile.getParentFile().getChild(resultingFile.getName() + ".tmp").getUnderlyingFile();

					System.out.println("Starting " + tempFile.getPathPretty());
					TarBackupContainer bkp = new TarBackupContainer(tempFile);
					wrt = bkp.newWriter();

					wrt.backup(new BackupWriterListener() {
						@Override
						public void setMax(int max) {
							fragment.setMax(max);
						}

						@Override
						public void step(String message, int delta) {
							fragment.step(message, delta);
						}

						@Override
						public boolean isCancelled() {
							return fragment.isCancelled();
						}});

					if (fragment.isCancelled()) {
						System.out.println("Cancelled " + resultingFile.getPathPretty());
						if (tempFile.exists())
							tempFile.delete();
					} else {
						if (resultingFile.exists())
							resultingFile.getUnderlyingFile().delete();
						tempFile.renameTo(resultingFile.getUnderlyingFile());
						mBackupOk = true;
						// Refresh the snapshot data
						resultingFile = resultingFile.newSnapshot();
						System.out.println("Finished " + resultingFile.getPathPretty() + ", size = " + resultingFile.getLength());
					}
				} catch (Exception e) {
					Logger.logError(e);
					if (tempFile.exists())
						try {
							tempFile.delete();
						} catch (Exception e2) {
							// Ignore
						}
					throw new RuntimeException("Error during backup", e);
				} finally {
					if (wrt != null) {
						try {
							wrt.close();
						} catch (Exception e2) {
							// Ignore
						}
					}
				}
			}

			@Override
			public void onFinish(SimpleTaskQueueProgressFragment fragment, Exception exception) {
				super.onFinish(fragment, exception);
				if (exception != null) {
					try {
						if (tempFile.exists())
							tempFile.delete();
					} catch (IOException e) {
						Logger.logError(e);
					}
				}
				fragment.setSuccess(mBackupOk);
				if (mBackupOk) {
					BookCataloguePreferences prefs = BookCatalogueApp.getAppPreferences();
					prefs.setString(BookCataloguePreferences.PREF_LAST_BACKUP_DATE, mBackupDate);
					// Save the path if it's local
					if (resultingFile.getUnderlyingFile() instanceof LocalFileWrapper) {
						prefs.setString(BookCataloguePreferences.PREF_LAST_BACKUP_FILE, ((LocalFileWrapper)resultingFile.getUnderlyingFile()).getFile().getAbsolutePath());						
					}
				}
				if (fragment.getActivity() instanceof OnBackupCompleteListener) {
					OnBackupCompleteListener l = (OnBackupCompleteListener) fragment.getActivity();
					l.onBackupComplete(resultingFile, mBackupOk, fragment.isCancelled());
				}
			}

		};
		SimpleTaskQueueProgressFragment frag = SimpleTaskQueueProgressFragment.runTaskWithProgress(context, R.string.backing_up_ellipsis, task, false, taskId);
		frag.setNumberFormat(null);
	}

	/**
	 * Start a foreground task that backs up the entire catalogue.
	 * 
	 * We use a FragmentTask so that long actions do not occur in the UI thread.
	 */
	public static void restoreCatalogue(final BookCatalogueActivity context, final FileSnapshot inputFile, int taskId) {

		FragmentTask task = new FragmentTaskAbstract() {
			@Override
			public void run(final SimpleTaskQueueProgressFragment fragment, SimpleTaskContext taskContext) {
				FileWrapper file = inputFile.getUnderlyingFile(); //new File(StorageUtils.getSharedStoragePath() + "/bookCatalogue.bcbk");
				try {
					System.out.println("Starting " + file.getPathPretty());
					BackupReader rdr = BackupManager.readBackup(file);
					rdr.restore(new BackupReaderListener() {
						@Override
						public void setMax(int max) {
							fragment.setMax(max);
						}

						@Override
						public void step(String message, int delta) {
							fragment.step(message, delta);
						}

						@Override
						public boolean isCancelled() {
							return fragment.isCancelled();
						}});
				} catch (Exception e) {
					Logger.logError(e);
					throw new RuntimeException("Error during restore", e);
				}
				try {
					System.out.println("Finished " + file.getPathPretty() + ", size = " + file.getLength());
				} catch (IOException e) {
					Logger.logError(e);
				}
			}
		};
		SimpleTaskQueueProgressFragment frag = SimpleTaskQueueProgressFragment.runTaskWithProgress(context,
				R.string.importing_ellipsis, task, false, taskId);
		frag.setNumberFormat(null);
	}
}
