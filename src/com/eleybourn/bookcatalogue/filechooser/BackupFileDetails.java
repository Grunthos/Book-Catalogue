package com.eleybourn.bookcatalogue.filechooser;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.backup.BackupInfo;
import com.eleybourn.bookcatalogue.utils.Utils;

/**
 * Implementation of FileDetails that record data about backup files in a background thread.
 * 
 * @author pjw
 */
public class BackupFileDetails implements FileListItem {
	// IMPORTANT NOTE: If fields are added, then writeToParcelable and the parcelable constructor
	// must also be modified.
	
	/** File for this item */
	private FileSnapshot mFile;
	/** The BackupInfo we use when displaying the object */
	private BackupInfo mInfo;
	
	/**
	 * Constructor
	 * 
	 * @param file
	 */
	public BackupFileDetails(FileSnapshot file) {
		mFile = file;
	}

	/**
	 * Accessor
	 * 
	 * @param info
	 */
	public void setInfo(BackupInfo info) {
		mInfo = info;
	}

	@Override
	public FileSnapshot getUnderlyingFile() {
		return mFile;
	}

	/**
	 * Return the view we use.
	 * 
	 * THIS SHOULD ALWAYS RETURN THE SAME VIEW. IT IS NOT A MULTI-TYPE LIST.
	 */
	@Override
	public int getViewId() {
		return R.layout.backup_chooser_item;
	}

	/**
	 * Fill in the details for the view we returned above.
	 */
	@Override
	public void onSetupView(Context c, int position, View target) {
		String fileName;
		boolean isDir;
		long length = 0;
		long modDate = 0;

		fileName = mFile.getName();
		isDir = mFile.isDirectory();
		if (!isDir) {
			length = mFile.getLength();
			modDate = mFile.getLastModified();
		}
		
		// Set the basic data
		TextView name = (TextView)target.findViewById(R.id.name);
		name.setText(fileName);
		TextView date = (TextView)target.findViewById(R.id.date);
		ImageView image = (ImageView)target.findViewById(R.id.icon);
		TextView details = (TextView)target.findViewById(R.id.details);
		
		// For directories, hide the extra data
		if (isDir) {
			date.setVisibility(View.GONE);
			details.setVisibility(View.GONE);
			image.setImageDrawable(c.getResources().getDrawable(R.drawable.ic_closed_folder));
		} else {
			// Display date and backup details
			image.setImageDrawable(c.getResources().getDrawable(R.drawable.ic_archive));
			date.setVisibility(View.VISIBLE);
			if (mInfo != null) {
				details.setVisibility(View.VISIBLE);
				details.setText(mInfo.getBookCount() + " books");	
				date.setText(Utils.formatFileSize(length) + ",  " + DateFormat.getDateTimeInstance().format(mInfo.getCreateDate()));
			} else {
				date.setText(Utils.formatFileSize(length) + ",  " + DateFormat.getDateTimeInstance().format(new Date(modDate)));
				details.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * PARCELLABLE INTERFACE.
	 * 
	 * Default to 0. Not really used.
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * PARCELLABLE INTERFACE.
	 * 
	 * Save all fields that must be persisted.
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(mFile);
		if (mInfo != null) {
			dest.writeByte((byte) 1);
			dest.writeBundle(mInfo.getBundle());
		} else {
			dest.writeByte((byte) 0);			
		}
	}

	/**
	 * PARCELLABLE INTERFACE.
	 * 
	 * Constructor, using a Parcel as source.
	 */
	private BackupFileDetails(Parcel in) {
		mFile = (FileSnapshot) in.readSerializable();
		byte infoFlag = in.readByte();
		if (infoFlag != (byte)0) {
			mInfo = new BackupInfo(in.readBundle());
		} else {
			mInfo = null;
		}
	}

	/**
	 * PARCELLABLE INTERFACE.
	 * 
	 * Need a CREATOR
	 */
	public static final Parcelable.Creator<BackupFileDetails> CREATOR = new Parcelable.Creator<BackupFileDetails>() {
		public BackupFileDetails createFromParcel(Parcel in) {
			return new BackupFileDetails(in);
		}
		public BackupFileDetails[] newArray(int size) {
			return new BackupFileDetails[size];
		}
	};

}