package com.eleybourn.bookcatalogue.filechooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import android.os.Parcelable;

public interface FileWrapper extends Serializable {
	
	public interface FileWrapperFilter {
		public boolean accept(FileWrapper f);
	}

	public boolean isDirectory() throws IOException;
	public boolean isFile() throws IOException;
	public long getLastModified() throws IOException;
	public InputStream openInput() throws IOException;
	public OutputStream openOutput() throws IOException;
	public String getName() throws IOException;
	public String getPathPretty() throws IOException;
	public String getParentPathPretty() throws IOException;
	public FileWrapper getParentFile() throws IOException;
	public FileWrapper getChild(String fileName) throws IOException;
	public FileWrapper[] listFiles() throws IOException;
	public FileWrapper[] listFiles(FileWrapperFilter filter) throws IOException;
	public long getLength() throws IOException;
	public boolean exists() throws IOException;
	public boolean canWrite() throws IOException;
	public void delete() throws IOException;
	public void renameTo(FileWrapper newPath) throws IOException;
	public FileWrapper getSibling(String siblingName) throws IOException;
}
