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
import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.compat.BookCatalogueFragment;
import com.eleybourn.bookcatalogue.filechooser.FileLister.FileListerListener;
import com.eleybourn.bookcatalogue.filechooser.cifs.CifsFileService;
import com.eleybourn.bookcatalogue.filechooser.local.LocalFileService;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueue.SimpleTaskContext;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment;
import com.eleybourn.bookcatalogue.utils.SimpleTaskQueueProgressFragment.FragmentTask;
import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter;
import com.eleybourn.bookcatalogue.widgets.FastScroller.SectionIndexerV2;
import com.eleybourn.bookcatalogue.widgets.SimpleListAdapter.ViewProvider;

/**
 * Fragment to display a simple directory/file browser.
 * 
 * @author pjw
 *
 * @param <T>		Class for file details, used in showing list.
 */
public class FileChooserFragment extends BookCatalogueFragment implements FileListerListener {
	private FileSnapshot mRootPath;
	protected static final String ARG_FILE_SERVICE = "fileService";
	protected static final String ARG_ROOT_PATH = "rootPath";
	protected static final String ARG_FILE_NAME = "fileName";
	protected static final String ARG_LIST = "list";
	// Create an empty one in case we are rotated before generated.
	protected ArrayList<FileListItem> mList = new ArrayList<FileListItem>();
	private FileService mFileService;

	private static final ArrayList<FileService> mFileServices = new ArrayList<FileService>();
	static {
		mFileServices.add(new LocalFileService());
		mFileServices.add(new CifsFileService());
	}

	/**
	 * Interface that the containing Activity must implement. Called when user changes path.
	 *
	 * @author pjw
	 */
	public interface PathChangedListener {
		public void onPathChanged(FileSnapshot root);
	}

	/** Create a new chooser fragment 
	 * 
	 * IMPORTANT NOTE: If this is called on the UI thread it will cause crashes due to network traffic
	 * in Android 3+.
	 * 
	 * @throws IOException
	 */
	public static FileChooserFragment newInstance(FileSnapshot root, String fileName) throws IOException {
		FileSnapshot path;
		// Turn the passed File into a directory
		if (root.isDirectory()) {
			path = root;
		} else {
			path = new FileSnapshot(root.getUnderlyingFile().getParentFile());
		}
		
		// Build the fragment and save the details
		FileChooserFragment frag = new FileChooserFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROOT_PATH, path);
        args.putString(ARG_FILE_NAME, fileName);
        frag.setArguments(args);

        return frag;
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
				SimpleTaskQueueProgressFragment.runTaskWithProgress(getActivity(), 0, new DirectoryUpTask(mRootPath), true, 0);
			}
		});

		final Spinner services = (Spinner) getView().findViewById(R.id.file_service);
		//FileServiceAdapter serviceAdapter = new FileServiceAdapter(getActivity(), R.layout.file_services_list_item, mFileServices); // android.R.layout.simple_list_item_1, mFileServices);
		//services.setAdapter(serviceAdapter);
		ArrayList<String> names = new ArrayList<String>() ;
		for(FileService s: mFileServices) {
			names.add(s.getName());
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, names);
		//adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		services.setAdapter(adapter);
		int selectedService = 0;

		// If it's new, just build from scratch, otherwise, get the saved directory and list
		if (savedInstanceState == null) {
			mRootPath = (FileSnapshot) getArguments().getSerializable(ARG_ROOT_PATH);
			String fileName = getArguments().getString(ARG_FILE_NAME);
			EditText et = (EditText) getView().findViewById(R.id.file_name);
			et.setText(fileName);
			((TextView) getView().findViewById(R.id.path)).setText(mRootPath.getPathPretty());				
			tellActivityPathChanged(getActivity(), mRootPath);
		} else {
			mRootPath = (FileSnapshot) savedInstanceState.getSerializable(ARG_ROOT_PATH);
			ArrayList<FileListItem> list = savedInstanceState.getParcelableArrayList(ARG_LIST);
			this.onGotFileList(mRootPath, list);
			String prefService =  savedInstanceState.getString(ARG_FILE_SERVICE);
			if (prefService != null) {
				for(int i = 0; i < mFileServices.size(); i++) {
					FileService s = mFileServices.get(i);
					if (s.getName().equals(prefService)) {
						selectedService = i;
						break;
					}
				}
			}
		}

		ImageView bookshelfDown = (ImageView) getView().findViewById(R.id.file_service_button);
		bookshelfDown.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				services.performClick();
				return;
			}
		});

		mFileService = mFileServices.get(selectedService);
		services.setSelection(selectedService);
		services.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}});
	}

	/**
	 * Convenience method to tell our activity the path has changed.
	 */
	private static void tellActivityPathChanged(Activity a, FileSnapshot newRoot) {	
		((PathChangedListener)a).onPathChanged(newRoot);
	}

	private static class DirectoryUpTask implements FragmentTask {
		FileSnapshot mPath;

		public DirectoryUpTask(FileSnapshot path) {
			mPath = path;
		}
		@Override
		public void run(SimpleTaskQueueProgressFragment fragment, SimpleTaskContext taskContext) throws Exception {
			FileWrapper wrap = mPath.getUnderlyingFile().getParentFile();
			if (wrap != null) {
				mPath = new FileSnapshot(mPath.getUnderlyingFile().getParentFile());	
			} else {
				mPath = null;
				fragment.showToast(R.string.no_parent_directory_found);
			}
		}

		@Override
		public void onFinish(SimpleTaskQueueProgressFragment fragment, Exception exception) {
			if (exception != null) {
				Logger.logError(exception);
				fragment.showToast(R.string.unexpected_error);
			} else {
				if (mPath != null) {
					tellActivityPathChanged(fragment.getActivity(), mPath);				
				}
			}
		}
		
	}

//	/**
//	 * Handle the 'Up' action
//	 */
//	private void handleUp() {
//		FileSnapshot parent = null;
//
//		parent = mRootPath.getParentFile();			
//
//		if (parent == null) {
//			Toast.makeText(getActivity(), R.string.no_parent_directory_found, Toast.LENGTH_LONG).show();
//			return;
//		}
//		mRootPath = parent;
//		
//		tellActivityPathChanged();
//	}

	/**
	 * Save our root path and list
	 */
	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putSerializable(ARG_ROOT_PATH, mRootPath);
		state.putParcelableArrayList(ARG_LIST, mList);
		if (mFileService != null) {
			state.putString(ARG_FILE_SERVICE, mFileService.getName());
		}
	}

	/**
	 * List Adapter for FileDetails objects
	 * 
	 * @author pjw
	 */
	public class DirectoryAdapter extends SimpleListAdapter<FileListItem> implements SectionIndexerV2 {
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
		public DirectoryAdapter(Context context, int rowViewId, ArrayList<FileListItem> items) {
			super(context, rowViewId, items);
		}

		@Override
		protected void onSetupView(FileListItem fileDetails, int position, View target) {
			fileDetails.onSetupView(getActivity(), position, target);
		}

		@Override
		protected void onRowClick(FileListItem fileDetails, int position, View v) {
			if (fileDetails != null) {
				boolean isDir;
				String fileName;
				isDir = fileDetails.getUnderlyingFile().isDirectory();
				fileName = fileDetails.getUnderlyingFile().getName();
				
				if (isDir) {
					//mRootPath = fileDetails.getUnderlyingFile();
					tellActivityPathChanged(getActivity(), fileDetails.getUnderlyingFile());
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

		@Override
		public String[] getSectionTextForPosition(int position) {
			if (position < 0 || position >= getCount())
				return null;
			FileListItem item = this.getItem(position);
			final String name = item.getUnderlyingFile().getName();
			return new String[] {name.substring(0,1).toUpperCase(), name};
		}	
	}

	/**
	 * List Adapter for FileDetails objects
	 * 
	 * @author pjw
	 */
	public class FileServiceAdapter extends SimpleListAdapter<FileService> {
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
		public FileServiceAdapter(Context context, int rowViewId, ArrayList<FileService> items) {
			super(context, rowViewId, items);
		}

		@Override
		protected void onSetupView(FileService service, int position, View target) {
			TextView text = (TextView) target.findViewById(android.R.id.text1);
			text.setText(service.getName());
		}

		@Override
		protected void onRowClick(FileService service, int position, View v) {
			if (service != null) {
			}
		};

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			View v = super.getDropDownView(position, convertView, parent);
			return v;
		}
	}
	
	/** 
	 * Accessor
	 * 
	 * @return
	 */
	public String getFileName() {
		EditText et = (EditText) getView().findViewById(R.id.file_name);
		return et.getText().toString();
	}

	/** 
	 * Accessor
	 * 
	 * @return
	 */
	public FileSnapshot getRoot() {
		return mRootPath;
	}

	/**
	 * Display the list
	 * 
	 * @param root		Root directory
	 * @param dirs		List of FileDetials
	 */
	@Override
	public void onGotFileList(FileSnapshot root, ArrayList<FileListItem> list) {
		String prettyPath;
		mRootPath = root;

		prettyPath = mRootPath.getPathPretty();
		((TextView) getView().findViewById(R.id.path)).setText(prettyPath);

		// Setup and display the list
		mList = list;
		// We pass 0 as view ID since each item can provide the view id
		DirectoryAdapter adapter = new DirectoryAdapter(getActivity(), 0, mList);
		ListView lv = ((ListView) getView().findViewById(android.R.id.list));
		lv.setAdapter(adapter);

		// Force a rebuild
		lv.setFastScrollEnabled(false);
		lv.setFastScrollEnabled(true);

	}

}
