package com.eleybourn.bookcatalogue.filechooser;

import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;

public interface FileListItem extends Parcelable, ViewProvider {
	public FileSnapshot getUnderlyingFile();
	public int getViewId();
	public void onSetupView(Context c, int position, View target);
}
