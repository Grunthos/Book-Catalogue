package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;

import com.eleybourn.bookcatalogue.filechooser.FileWrapper.FileWrapperFilter;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueue.SimpleTaskContext;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTask;

/**
 * Partially implements a FragmentTask to build a list of files in the background.
 * 
 * @author pjw
 */
public abstract class FileLister implements FragmentTask {
	protected ArrayList<FileListItem> dirs;
	protected FileSnapshot mRoot;

	/**
	 * Interface for the creating activity to allow the resulting list to be returned.
	 * 
	 * @author pjw
	 */
	public interface FileListerListener {
		public void onGotFileList(FileSnapshot root, ArrayList<FileListItem> list);
	}

	/**
	 * Constructor
	 * 
	 * @param root
	 */
	public FileLister(FileSnapshot root) {
		mRoot = root;
	}

	public FileSnapshot getRoot() {
		return mRoot;
	}

	/** Return a FileFilter appropriate to the types of files being listed */
	protected abstract FileWrapperFilter getFilter();
	/** Turn an array of Files into an ArrayList of FileDetails. 
	 * @throws IOException */
	protected abstract ArrayList<FileListItem> processList(FileWrapper[] files) throws IOException;

	@Override
	public void run(SimpleTaskQueueProgressFragment fragment, SimpleTaskContext taskContext) throws IOException {
		// Get a file list
		FileWrapper[] files = mRoot.getUnderlyingFile().listFiles(getFilter());
		// Filter/fill-in using the subclass
		dirs = processList(files);
		// Sort it
		Collections.sort(dirs, mComparator);
	}

	@Override
	public void onFinish(SimpleTaskQueueProgressFragment fragment, Exception exception) {
		// Display it in UI thread.
		Activity a = fragment.getActivity();
		if (a != null && a instanceof FileListerListener) {
			((FileListerListener)a).onGotFileList(mRoot, dirs);
		}
	}

	/**
	 * Perform case-insensitive sorting using default locale.
	 */
	private static class FileDetailsComparator implements Comparator<FileListItem> {
		public int compare(FileListItem f1, FileListItem f2) {
			return f1.getUnderlyingFile().getName().toUpperCase().compareTo(f2.getUnderlyingFile().getName().toUpperCase());
		}
	}

	private FileDetailsComparator mComparator = new FileDetailsComparator();

}
