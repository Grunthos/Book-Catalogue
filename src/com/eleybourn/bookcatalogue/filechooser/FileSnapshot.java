package com.eleybourn.bookcatalogue.filechooser;

import java.io.IOException;
import java.io.Serializable;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

/** Interface for details of files in current directory */
public abstract class FileSnapshot implements ViewProvider, Parcelable, Serializable {
	private final FileSnapshot mParent;
	private final FileWrapper mFileWrapper;

	private final boolean mIsDirectory;
	private final boolean mIsFile;
	private final boolean mExists;
	private final String mName;
	private final String mPathPretty;
	private final String mParentPathPretty;

	public FileSnapshot(FileSnapshot parent, FileWrapper file) throws IOException {
		mParent = parent;
		mFileWrapper = file;
		mExists = file.exists();
		mIsDirectory = file.isDirectory();
		mIsFile = file.isFile();
		mName = file.getName();
		mPathPretty = file.getPathPretty();
		mParentPathPretty = file.getParentPathPretty();
	}

	/** Get the underlying File object */
	public FileWrapper getUnderlyingFile() {
		return mFileWrapper;
	}

	/** Snapshot data */
	public boolean isDirectory() {
		return mIsDirectory;
	}

	/** Snapshot data */
	public boolean exists() {
		return mExists;
	}

	/** Snapshot data */
	public boolean isFile() {
		return mIsFile;
	}
	/** Snapshot data */
	public String getName() {
		return mName;
	}
	/** Snapshot data */
	public String getPathPretty() {
		return mPathPretty;
	}
	/** Snapshot data */
	public FileSnapshot getParentFile() {
		return mParent;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}
	@Override
	public int getViewId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public FileSnapshot getChild(String fileName) throws IOException {
		return new FileSnapshot(this, mFileWrapper.getChild(fileName));
	}
}