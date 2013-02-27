package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.eleybourn.bookcatalogue.utils.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class CifsFileWrapper implements FileWrapper {
	private static final long serialVersionUID = 8965810687185607884L;

	private SmbFile mFile;
	private NtlmPasswordAuthentication mAuth;

	public CifsFileWrapper(SmbFile file, NtlmPasswordAuthentication auth) {
		mAuth = auth;
		mFile = file;
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(mAuth);
		out.writeUTF(mFile.getPath());
	}
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		mAuth = (NtlmPasswordAuthentication) in.readObject();
		String path = in.readUTF();
		mFile = new SmbFile(path, mAuth);
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
		if (parent.getType() == SmbFile.TYPE_FILESYSTEM || parent.getType() == SmbFile.TYPE_SHARE || parent.getType() == SmbFile.TYPE_SERVER) {
			return new CifsFileWrapper(parent, mAuth);
		} else {
			return null;
		}
	}

	@Override
	public FileWrapper getChild(String fileName) throws SmbException, MalformedURLException {
		return new CifsFileWrapper(new SmbFile(mFile.getPath() + "/" + fileName, mAuth), mAuth);
	}

	private class SimpleFilter implements SmbFileFilter {
		private FileWrapperFilter mUserFilter;
		private ArrayList<FileWrapper> mArray = new ArrayList<FileWrapper>();

		public SimpleFilter(FileWrapperFilter userFilter) {
			mUserFilter = userFilter;
		}

		private ArrayList<FileWrapper> getArray() {
			return mArray;
		}

		@Override
		public boolean accept(SmbFile file) throws SmbException {
			final int type = file.getType();
			if (type == SmbFile.TYPE_FILESYSTEM || type == SmbFile.TYPE_SHARE) {
				CifsFileWrapper fw = new CifsFileWrapper(file, mAuth);
				if (mUserFilter == null || mUserFilter.accept(fw)) {
					mArray.add(fw);
					return true;
				} else {
					return false;					
				}
			} else {
				return false;
			}
		}};
	

	@Override
	public FileWrapper[] listFiles() throws SmbException, MalformedURLException {
		SimpleFilter filter = new SimpleFilter(null);
//		SmbFile[] files = mFile.listFiles(filter);
		mFile.listFiles(filter);
		ArrayList<FileWrapper> list = filter.getArray();
		FileWrapper[] wrappers = new FileWrapper[list.size()];
		for(int i = 0; i < list.size(); i++)
			wrappers[i] = list.get(i);
		return wrappers;

//		FileWrapper[] wrappers = new FileWrapper[files.length];
//		for(int i = 0; i < files.length; i++) {
//			CifsFileWrapper w = new CifsFileWrapper(files[i], mAuth);
//			wrappers[i] = w;
//		}
//		return wrappers;
	}

	@Override
	public FileWrapper[] listFiles(FileWrapperFilter userFilter) throws IOException {
		try {
			SimpleFilter cifsFilter = new SimpleFilter(userFilter);
			mFile.listFiles(cifsFilter);
			ArrayList<FileWrapper> list = cifsFilter.getArray();

//			SmbFile[] files = mFile.listFiles(filter);
//			ArrayList<FileWrapper> list = new ArrayList<FileWrapper>();
//			//FileWrapper[] wrappers = new FileWrapper[files.length];
//			for(SmbFile f: files) {
//				CifsFileWrapper w = new CifsFileWrapper(f, mAuth);
//				if (filter.accept(w)) {
//					list.add(w);
//				}
//			}
			FileWrapper[] wrappers = new FileWrapper[list.size()];
			for(int i = 0; i < list.size(); i++)
				wrappers[i] = list.get(i);
			return wrappers;
//		} catch (SmbAuthException e) {
//			Logger.logError(e);
//			throw e;
		} catch (IOException e) {
			Logger.logError(e);
			throw e;
		} catch (Exception e) {
			Logger.logError(e);
			throw new RuntimeException("Unable to list files", e);
		}
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

	@Override
	public FileWrapper getSibling(String siblingName) throws SmbException, MalformedURLException {
		return getParentFile().getChild(siblingName);
	}
}
