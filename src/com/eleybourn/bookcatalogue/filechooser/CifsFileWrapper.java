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

	public CifsFileWrapper(SmbFile file, NtlmPasswordAuthentication auth) throws SmbException {
		mAuth = auth;
		mFile = file;
	}

	@Override
	public boolean isDirectory() throws SmbException {
		return mFile.isDirectory();
	}

	@Override
	public boolean isFile() throws SmbException {
		return mFile.isFile();
	}

	@Override
	public long getLastModified() throws SmbException {
		return mFile.lastModified();
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
		return mFile.getName();
	}

	@Override
	public String getPathPretty() {
		return mFile.getUncPath();
	}

	@Override
	public String getParentPathPretty() {
		return mFile.getParent();
	}

	@Override
	public CifsFileWrapper getParentFile() throws MalformedURLException, SmbException {
		SmbFile parent = new SmbFile(mFile.getParent(), mAuth);
		if (parent.getType() == SmbFile.TYPE_FILESYSTEM || parent.getType() == SmbFile.TYPE_SHARE) {
			return new CifsFileWrapper(parent, mAuth);
		} else {
			return null;
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
		return mFile.length();
	}

	@Override
	public boolean exists() throws SmbException {
		return mFile.exists();
	}

	@Override
	public boolean canWrite() throws SmbException {
		return mFile.canWrite();
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
