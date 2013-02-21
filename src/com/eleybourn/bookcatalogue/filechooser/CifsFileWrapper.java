package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class CifsFileWrapper implements FileWrapper {
	private static final long serialVersionUID = 8965810687185607884L;

	private SmbFile mFile;
	private NtlmPasswordAuthentication mAuth;
	private boolean mHasSnapshot = false;
	private boolean mIsDirectory;
	private boolean mIsFile;
	private long mLength;
	private long mLastModified;
	private String mName;
	private String mUncPath;
	private String mParent;
	private boolean mExists;
	private boolean mCanWrite;
	private CifsFileWrapper mParentFile;

	public CifsFileWrapper(SmbFile file, NtlmPasswordAuthentication auth) throws SmbException {
		mAuth = auth;
		mFile = file;
	}

	public void makeSnapshot() throws SmbException, MalformedURLException {
		mHasSnapshot = false;

		mIsDirectory = mFile.isDirectory();
		mIsFile = mFile.isFile();
		mLastModified = mFile.lastModified();
		mName = mFile.getName();
		mUncPath =  mFile.getUncPath();
		mParent = mFile.getParent();
		if (mFile.getType() == SmbFile.TYPE_FILESYSTEM) {
			mExists = mFile.exists();
		} else {
			mExists = true;
		}
		mCanWrite = mFile.canWrite();	
		mLength = mFile.length();

		mParentFile = getParentFile();

		mHasSnapshot = true;
	}

	@Override
	public boolean isDirectory() throws SmbException {
		if (mHasSnapshot) {
			return mIsDirectory;
		} else {
			return mFile.isDirectory();
		}
	}

	@Override
	public boolean isFile() throws SmbException {
		if (mHasSnapshot) {
			return mIsFile;
		} else {
			return mFile.isFile();
		}
	}

	@Override
	public long getLastModified() throws SmbException {
		if (mHasSnapshot) {
			return mLastModified;
		} else {
			return mFile.lastModified();
		}
	}

	@Override
	public InputStream openInput() throws SmbException, MalformedURLException, UnknownHostException {
		return new SmbFileInputStream(mFile);
	}

	@Override
	public OutputStream openOutput() throws SmbException, MalformedURLException, UnknownHostException {
		return new SmbFileOutputStream(mFile);
	}

	@Override
	public String getName() {
		if (mHasSnapshot) {
			return mName;
		} else {
			return mFile.getName();
		}
	}

	@Override
	public String getPathPretty() {
		if (mHasSnapshot) {
			return mUncPath;
		} else {
			return mFile.getUncPath();
		}
	}

	@Override
	public String getParentPathPretty() {
		if (mHasSnapshot) {
			return mParent;
		} else {
			return mFile.getParent();
		}
	}

	@Override
	public CifsFileWrapper getParentFile() throws MalformedURLException, SmbException {
		if (mHasSnapshot) {
			return mParentFile;
		} else {
			SmbFile parent = new SmbFile(mFile.getParent(), mAuth);
			if (parent.getType() == SmbFile.TYPE_FILESYSTEM || parent.getType() == SmbFile.TYPE_SHARE) {
				return new CifsFileWrapper(parent, mAuth);
			} else {
				return null;
			}
		}
	}

	@Override
	public FileWrapper getChild(String fileName) throws MalformedURLException, SmbException {
		return new CifsFileWrapper(new SmbFile(mFile.getPath() + "/" + fileName, mAuth), mAuth);
	}

	@Override
	public FileWrapper[] listFiles() throws SmbException, MalformedURLException {
		SmbFile[] files = mFile.listFiles();
		FileWrapper[] wrappers = new FileWrapper[files.length];
		for(int i = 0; i < files.length; i++) {
			CifsFileWrapper w = new CifsFileWrapper(files[i], mAuth);
			w.makeSnapshot();
			wrappers[i] = w;
		}
		return wrappers;
	}

	@Override
	public FileWrapper[] listFiles(FileWrapperFilter filter) throws SmbException, MalformedURLException {
		SmbFile[] files = mFile.listFiles();
		ArrayList<FileWrapper> list = new ArrayList<FileWrapper>();
		//FileWrapper[] wrappers = new FileWrapper[files.length];
		for(SmbFile f: files) {
			CifsFileWrapper w = new CifsFileWrapper(f, mAuth);
			if (filter.accept(w)) {
				w.makeSnapshot();
				list.add(w);
			}
		}
		FileWrapper[] wrappers = new FileWrapper[list.size()];
		for(int i = 0; i < list.size(); i++)
			wrappers[i] = list.get(i);
		return wrappers;
	}

	@Override
	public long getLength() throws SmbException {
		if (mHasSnapshot) {
			return mLength;
		} else {
			return mFile.length();
		}
	}

	@Override
	public boolean exists() throws SmbException {
		if (mHasSnapshot) {
			return mExists;
		} else {
			return mFile.exists();
		}
	}

	@Override
	public boolean canWrite() throws SmbException {
		if (mHasSnapshot) {
			return mCanWrite;
		} else {
			return mFile.canWrite();
		}
	}

	@Override
	public void delete() throws SmbException {
		mFile.delete();
	}

	@Override
	public void renameTo(FileWrapper newPath) throws SmbException {
		if (newPath instanceof CifsFileWrapper) {
			CifsFileWrapper lclNewPath = (CifsFileWrapper) newPath;
			mFile.renameTo(lclNewPath.getFile());
		} else {
			throw new RuntimeException("newPath must be the same class as the current object (" + this.getClass().getSimpleName() + ")");
		}
	}
	
	public SmbFile getFile() {
		return mFile;
	}
}
