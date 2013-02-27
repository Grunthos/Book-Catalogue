package com.eleybourn.bookcatalogue.filechooser.cifs;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;

import com.eleybourn.bookcatalogue.BookCatalogueApp;
import com.eleybourn.bookcatalogue.R;
import com.eleybourn.bookcatalogue.filechooser.FileService;
import com.eleybourn.bookcatalogue.filechooser.FileSnapshot;

public class CifsFileService implements FileService {

	@Override
	public String getName() {
		return BookCatalogueApp.getResourceString(R.string.windows_cifs_shares);
	}

	@Override
	public boolean canAdd() {
		return true;
	}

	@Override
	public ArrayList<FileSnapshot> getFavourites(String defaultPath, String defaultFileName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addServer(Activity parent) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void prepareServer(Activity parent, FileSnapshot requestedServer) {
		// TODO Auto-generated method stub
		
	}

}
