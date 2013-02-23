package com.eleybourn.bookcatalogue.filechooser;

import java.io.Serializable;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;

import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

/** Interface for details of files in current directory */
public interface FileSnapshot extends ViewProvider, Parcelable, Serializable {
	/** Get the underlying File object */
	FileWrapper getUnderlyingFile();
	/** Called to fill in the details of this object in the View provided by the ViewProvider implementation */
	public void onSetupView(Context context, int position, View target);
	/** Snapshot data */
	public boolean isDirectory();
	/** Snapshot data */
	public String getName();
	/** Snapshot data */
	public String getPathPretty();
	/** Snapshot data */
	public FileSnapshot getParentFile();
}