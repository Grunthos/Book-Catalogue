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

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.compat.BookCatalogueFragment;
import com.eleybourn.bookcatalogue.filechooser.FileLister.FileListerListener;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter;
import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

/**
 * Fragment to display a simple directory/file browser.
 * 
 * @author pjw
 *
 * @param <T>		Class for file details, used in showing list.
 */
public class FileChooserFragment extends BookCatalogueFragment implements FileListerListener {
	private FileWrapper mRootPath;
	protected static final String ARG_ROOT_PATH = "rootPath";
	protected static final String ARG_FILE_NAME = "fileName";
	protected static final String ARG_LIST = "list";
	// Create an empty one in case we are rotated before generated.
	protected ArrayList<FileDetails> mList = new ArrayList<FileDetails>();

	/**
	 * Interface that the containing Activity must implement. Called when user changes path.
	 *
	 * @author pjw
	 */
	public interface PathChangedListener {
		public void onPathChanged(FileWrapper root);
	}

	/** Create a new chooser fragment 
	 * @throws IOException */
	public static FileChooserFragment newInstance(FileWrapper root, String fileName) throws IOException {
		FileWrapper path;
		// Turn the passed File into a directory
		if (root.isDirectory()) {
			path = root;
		} else {
			path = root.getParentFile();
		}
		
		// Build the fragment and save the details
		FileChooserFragment frag = new FileChooserFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROOT_PATH, path);
        args.putString(ARG_FILE_NAME, fileName);
        frag.setArguments(args);

        return frag;
	}

	/** Interface for details of files in current directory */
	public interface FileDetails extends ViewProvider, Parcelable {
		/** Get the underlying File object */
		FileWrapper getFile();
		/** Called to fill in the defails of this object in the View provided by the ViewProvider implementation */
		public void onSetupView(Context context, int position, View target);
	}

	/**
	 * Ensure activity supports event
	 */
	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);

		checkInstance(a, PathChangedListener.class);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.file_chooser, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Handle the 'up' item; go to the next directory up
		final View root = getView();
		((ImageView) root.findViewById(R.id.up)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleUp();
			}
		});

		// If it's new, just build from scratch, otherwise, get the saved directory and list
		if (savedInstanceState == null) {
			mRootPath = (FileWrapper) getArguments().getSerializable(ARG_ROOT_PATH);
			String fileName = getArguments().getString(ARG_FILE_NAME);
			EditText et = (EditText) getView().findViewById(R.id.file_name);
			et.setText(fileName);
			try {
				((TextView) getView().findViewById(R.id.path)).setText(mRootPath.getPathPretty());				
			} catch (IOException e) {
				Logger.logError(e);
				Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
				return;			
			}
			tellActivityPathChanged();
		} else {
			mRootPath = (FileWrapper) savedInstanceState.getSerializable(ARG_ROOT_PATH);
			ArrayList<FileDetails> list = savedInstanceState.getParcelableArrayList(ARG_LIST);
			this.onGotFileList(mRootPath, list);
		}
	}

	/**
	 * Convenience method to tell our activity the path has changed.
	 */
	private void tellActivityPathChanged() {	
		((PathChangedListener)getActivity()).onPathChanged(mRootPath);
	}

	/**
	 * Handle the 'Up' action
	 */
	private void handleUp() {
		FileWrapper parent = null;

		try {
			parent = mRootPath.getParentFile();			
		} catch (IOException e) {
			Logger.logError(e);
			Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
			return;			
		}
		if (parent == null) {
			Toast.makeText(getActivity(), R.string.no_parent_directory_found, Toast.LENGTH_LONG).show();
			return;
		}
		mRootPath = parent;
		
		tellActivityPathChanged();
	}

	/**
	 * Save our root path and list
	 */
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putSerializable(ARG_ROOT_PATH, mRootPath);
		state.putParcelableArrayList(ARG_LIST, mList);
	}

	/**
	 * List Adapter for FileDetails objects
	 * 
	 * @author pjw
	 */
	public class DirectoryAdapter extends SimpleListAdapter<FileDetails> {
		boolean series = false;

		/**
		 * 
		 * Pass the parameters directly to the overridden function
		 * 
		 * @param context
		 * @param layout
		 * @param cursor
		 * @param from
		 * @param to
		 */
		public DirectoryAdapter(Context context, int rowViewId, ArrayList<FileDetails> items) {
			super(context, rowViewId, items);
		}

		@Override
		protected void onSetupView(FileDetails fileDetails, int position, View target) {
			fileDetails.onSetupView(getActivity(), position, target);
		}

		@Override
		protected void onRowClick(FileDetails fileDetails, int position, View v) {
			if (fileDetails != null) {
				boolean isDir;
				String fileName;
				try {
					isDir = fileDetails.getFile().isDirectory();
					fileName = fileDetails.getFile().getName();
				} catch (IOException e) {
					Logger.logError(e);
					Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
					return;
				}
				
				if (isDir) {
					mRootPath = fileDetails.getFile();
					tellActivityPathChanged();
				} else {
					EditText et = (EditText) FileChooserFragment.this.getView().findViewById(R.id.file_name);
					et.setText(fileName);
				}
			}
		};

		@Override
		protected void onListChanged() {
			// Just ignore it. They never change.
		};
	}

	/** 
	 * Accessor
	 * 
	 * @return
	 * @throws IOException 
	 */
	public FileWrapper getSelectedFile() throws IOException {
		EditText et = (EditText) getView().findViewById(R.id.file_name);
		return mRootPath.getChild(et.getText().toString());
	}

	/**
	 * Display the list
	 * 
	 * @param root		Root directory
	 * @param dirs		List of FileDetials
	 */
	@Override
	public void onGotFileList(FileWrapper root, ArrayList<FileDetails> list) {
		String prettyPath;
		try {
			prettyPath = mRootPath.getPathPretty();
		} catch (IOException e) {
			Logger.logError(e);
			Toast.makeText(getActivity(), R.string.unexpected_error, Toast.LENGTH_LONG).show();
			return;
		}

		mRootPath = root;
		((TextView) getView().findViewById(R.id.path)).setText(prettyPath);

		// Setup and display the list
		mList = list;
		// We pass 0 as view ID since each item can provide the view id
		DirectoryAdapter adapter = new DirectoryAdapter(getActivity(), 0, mList);
		ListView lv = ((ListView) getView().findViewById(android.R.id.list));
		lv.setAdapter(adapter);
	}

}
