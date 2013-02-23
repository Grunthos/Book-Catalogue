package com.eleybourn.bookcatalogue.filechooser;

import java.io.IOException;
import java.io.Serializable;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

/** Interface for details of files in current directory */
public class FileSnapshot implements Parcelable, Serializable {
	private static final long serialVersionUID = 139797438345857692L;

	// IMPORTANT NOTE: If fields are added, then writeToParcelable and the parcelable constructor
	// must also be modified.
	private final FileSnapshot mParent;
	private final FileWrapper mFileWrapper;

	private final boolean mIsDirectory;
	private final boolean mIsFile;
	private final boolean mExists;
	private final String mName;
	private final String mPathPretty;
	private final String mParentPathPretty;
	private final long mLength;
	private final long mLastModified;

	public FileSnapshot(FileSnapshot parent, FileWrapper file) throws IOException {
		// IMPORTANT NOTE: If fields are added, then writeToParcelable and the parcelable constructor
		// must also be modified.
		mParent = parent;
		mFileWrapper = file;
		mExists = file.exists();
		mIsDirectory = file.isDirectory();
		mIsFile = file.isFile();
		mName = file.getName();
		mPathPretty = file.getPathPretty();
		mParentPathPretty = file.getParentPathPretty();
		mLength = file.getLength();
		mLastModified = file.getLastModified();
	}

	/** Get the underlying File object */
	public FileWrapper getUnderlyingFile() {
		return mFileWrapper;
	}

	/** Snapshot data */
	public long getLength() {
		return mLength;
	}

	/** Snapshot data */
	public long getLastModified() {
		return mLastModified;
	}

	/** Snapshot data */
	public String getParentPathPretty() {
		return mParentPathPretty;
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

	public FileSnapshot getChild(String fileName) throws IOException {
		return new FileSnapshot(this, mFileWrapper.getChild(fileName));
	}

	public FileSnapshot(Parcel in) {
		int version = in.readInt();
	
		mParent = (FileSnapshot) in.readSerializable();
		mFileWrapper = (FileWrapper) in.readSerializable();
		
		mExists = in.readByte() == (byte)1;
		mIsDirectory = in.readByte() == (byte)1;
		mIsFile = in.readByte() == (byte)1;

		mName = in.readString();
		mPathPretty = in.readString();
		mParentPathPretty = in.readString();

		mLength = in.readLong();
		mLastModified = in.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(1);
		dest.writeSerializable(mParent);
		dest.writeSerializable(mFileWrapper);
		dest.writeByte( mExists ? (byte)1 : (byte)0 );
		dest.writeByte( mIsDirectory ? (byte)1 : (byte)0 );
		dest.writeByte( mIsFile ? (byte)1 : (byte)0 );

		dest.writeString(mName);
		dest.writeString(mPathPretty);
		dest.writeString(mParentPathPretty);
		
		dest.writeLong(mLength);
		dest.writeLong(mLastModified);
	}
}