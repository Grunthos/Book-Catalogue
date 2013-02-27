package com.eleybourn.bookcatalogue.filechooser.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.eleybourn.bookcatalogue.filechooser.FileWrapper;
import com.eleybourn.bookcatalogue.filechooser.FileWrapper.FileWrapperFilter;

public class LocalFileWrapper implements FileWrapper {
	private static final long serialVersionUID = -1956193002020131898L;

	private final File mFile;

	public LocalFileWrapper(File file) {
		mFile = file;
	}

	@Override
	public boolean isDirectory() {
		return mFile.isDirectory();
	}

	@Override
	public boolean isFile() {
		return mFile.isFile();
	}

	@Override
	public long getLastModified() {
		return mFile.lastModified();
	}

	@Override
	public InputStream openInput() throws FileNotFoundException {
		return new FileInputStream(mFile);
	}

	@Override
	public OutputStream openOutput() throws FileNotFoundException {
		return new FileOutputStream(mFile);
	}

	@Override
	public String getPathPretty() {
		return mFile.getPath();
	}

	@Override
	public String getParentPathPretty() {
		return mFile.getParent();
	}

	
	@Override
	public FileWrapper getParentFile() {
		return new LocalFileWrapper(mFile.getParentFile());
	}

	@Override
	public FileWrapper[] listFiles() {
		File[] files = mFile.listFiles();
		FileWrapper[] wrappers = new FileWrapper[files.length];
		for(int i = 0; i < files.length; i++) {
			wrappers[i] = new LocalFileWrapper(files[i]);
		}
		return wrappers;
	}

	@Override
	public FileWrapper[] listFiles(final FileWrapperFilter filter) {
		File[] files = mFile.listFiles();
		ArrayList<FileWrapper> list = new ArrayList<FileWrapper>();
		//FileWrapper[] wrappers = new FileWrapper[files.length];
		for(File f: files) {
			FileWrapper w = new LocalFileWrapper(f);
			if (filter.accept(w)) {
				list.add(w);
			}
		}
		FileWrapper[] wrappers = new FileWrapper[list.size()];
		for(int i = 0; i < list.size(); i++)
			wrappers[i] = list.get(i);
		return wrappers;
	}

	@Override
	public long getLength() {
		return mFile.length();
	}

	@Override
	public String getName() {
		return mFile.getName();
	}

	@Override
	public FileWrapper getChild(String childName) {
		return new LocalFileWrapper(new File(mFile.getAbsolutePath() + "/" + childName));
	}

	@Override
	public boolean exists() {
		return mFile.exists();
	}

	@Override
	public void delete() {
		mFile.delete();
	}

	@Override
	public void renameTo(FileWrapper newPath) {
		if (newPath instanceof LocalFileWrapper) {
			LocalFileWrapper lclNewPath = (LocalFileWrapper) newPath;
			mFile.renameTo(lclNewPath.getFile());
		} else {
			throw new RuntimeException("newPath must be the same class as the current object (" + this.getClass().getSimpleName() + ")");
		}
	}
	
	public File getFile() {
		return mFile;
	}

	@Override
	public boolean canWrite() {
		return mFile.canWrite();
	}

	@Override
	public FileWrapper getSibling(String siblingName) throws IOException {
		return getParentFile().getChild(siblingName);
	}
	
	@Override
	public String toString() {
		return getName();
	}

}
