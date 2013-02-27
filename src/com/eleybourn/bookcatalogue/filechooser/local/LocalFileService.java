package com.eleybourn.bookcatalogue.filechooser.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.eleybourn.bookcatalogue.BookCatalogueApp;
import com.eleybourn.bookcatalogue.BookCataloguePreferences;
import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.filechooser.FileService;
import com.eleybourn.bookcatalogue.filechooser.FileSnapshot;
import com.eleybourn.bookcatalogue.filechooser.FileService.OnServerPreparedListener;
import com.eleybourn.bookcatalogue.utils.StorageUtils;

import android.app.Activity;

public class LocalFileService implements FileService {

	@Override
	public String getName() {
		return BookCatalogueApp.getResourceString(R.string.local_files);
	}

	@Override
	public boolean canAdd() {
		// Can not 'add' local file servers
		return false;
	}

	@Override
	public ArrayList<FileSnapshot> getFavourites(String defaultPath, String defaultFileName) throws IOException {
		BookCataloguePreferences prefs = BookCatalogueApp.getAppPreferences();
		String lastBackupSpec = prefs.getString(BookCataloguePreferences.PREF_LAST_BACKUP_FILE, "");
		File defaultBackup;
		if (lastBackupSpec.equals("")) {
			String defaultDir = defaultPath; //StorageUtils.getSharedStoragePath();
			defaultBackup = new File(defaultDir + "/" + defaultFileName);
		} else {
			File lastBackup = new File(lastBackupSpec);
			defaultBackup = new File(lastBackup.getParent() + "/" + defaultFileName);			
		}
		ArrayList<FileSnapshot> list = new ArrayList<FileSnapshot>();
		LocalFileWrapper wrap = new LocalFileWrapper(defaultBackup);
		FileSnapshot snap = new FileSnapshot(wrap);
		list.add(snap);
		return null;
	}

	@Override
	public void addServer(Activity parent) {
		throw new RuntimeException("Can not add new local file services");
	}

	@Override
	public void prepareServer(Activity parent, FileSnapshot requestedServer) {
		// Just pass through; no prep required.
		((OnServerPreparedListener)parent).onServerPrepared(requestedServer, requestedServer);
	}

}
