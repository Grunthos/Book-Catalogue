package com.eleybourn.bookcatalogue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class to hold books data. Used if user want to show search results in the list (only for results of searching by author and title).
 * 
 * @author novacej
 */
public class Book implements Serializable {
	// XXXX: TODO: RELEASE: While I agree with the philosophy of creating a 'book' object, if we do it, we need to include all fields that can be retrieved from a search provider, not just the subset provided here.
	// A more representative list if fields can be found in CatalogueDBAdapter, under the various KEY_xxx fields.
	// Missing fields include: FORMAT, LIST_PRICE
	// It may even be worth storing a clone of the underlying bundle and have the accessors just use keys into the bundle since the book is
	// created from a bundle and ultimately turned back into a bundle.
	private static final long serialVersionUID = -8476561696513558565L;
	private String AUTHOR;
	private String TITLE;
	private String ISBN;
	private String DATE_PUBLISHED;
	private String PUBLISHER;
	private String PAGES;
	private String THUMBNAIL;
	private String GENRE;
	private String DESCRIPTION;
	private String SERIES_NAME;
	private ArrayList<Author> AUTHOR_ARRAY;
	private ArrayList<Series> SERIES_ARRAY;
	
	
    /**
     * Constructor without parameters.
     * 
     */		
	public Book(){	
		AUTHOR = "";
		TITLE = "";
		ISBN = "";
		DATE_PUBLISHED = "";
		PUBLISHER = "";
		PAGES = "";
		THUMBNAIL = "";
		GENRE = "";
		DESCRIPTION = "";
		SERIES_NAME = "";
		AUTHOR_ARRAY = new ArrayList<Author>();
		SERIES_ARRAY = new ArrayList<Series>();
	}

    /**
     * Constructor using a Parcel.
     * 
     * @param in
     */	
	public Book(Parcel in) {
		   this();
		   readFromParcel(in); 
	}
	
	
    /**
     * Constructor using a bundle as a source of data.
     * 
     * @param b
     * @throws IOException 
     */		
	public Book(Bundle b) throws IOException{
		AUTHOR = b.getString(CatalogueDBAdapter.KEY_AUTHOR_DETAILS) == null ? "" : b.getString(CatalogueDBAdapter.KEY_AUTHOR_DETAILS);
		TITLE = b.getString(CatalogueDBAdapter.KEY_TITLE) == null ? "" : b.getString(CatalogueDBAdapter.KEY_TITLE);
		ISBN = b.getString(CatalogueDBAdapter.KEY_ISBN) == null ? "" : b.getString(CatalogueDBAdapter.KEY_ISBN);
		DATE_PUBLISHED = b.getString(CatalogueDBAdapter.KEY_DATE_PUBLISHED) == null ? "" : b.getString(CatalogueDBAdapter.KEY_DATE_PUBLISHED);
		PUBLISHER = b.getString(CatalogueDBAdapter.KEY_PUBLISHER) == null ? "" : b.getString(CatalogueDBAdapter.KEY_PUBLISHER);
		PAGES = b.getString(CatalogueDBAdapter.KEY_PAGES) == null ? "" : b.getString(CatalogueDBAdapter.KEY_PAGES);
		GENRE = b.getString(CatalogueDBAdapter.KEY_GENRE) == null ? "" : b.getString(CatalogueDBAdapter.KEY_GENRE);
		DESCRIPTION = b.getString(CatalogueDBAdapter.KEY_DESCRIPTION) == null ? "" : b.getString(CatalogueDBAdapter.KEY_DESCRIPTION);
		SERIES_NAME = "";
		AUTHOR_ARRAY = new ArrayList<Author>();
		SERIES_ARRAY = new ArrayList<Series>();
		
		// See if we have a thumbnail, if we do, rename it.
		if (b.getString("__thumbnail") == null) {
			THUMBNAIL = "";
		} else {
			File file = new File(b.getString("__thumbnail"));
			if (!file.exists()) {
				THUMBNAIL = "";				
			} else {
				File newFile = File.createTempFile("tmp", ".jpg", StorageUtils.getSharedStorage());
				file.renameTo(newFile);
				THUMBNAIL = newFile.getAbsolutePath();
			}
		}
	}

	public void getThumbnailFromUrl(String url) throws IOException {
		File newFile = File.createTempFile("tmp", ".jpg", StorageUtils.getSharedStorage());
		Utils.saveThumbnailFromUrl(url, newFile);
		THUMBNAIL = newFile.getAbsolutePath();		
	}

	/**
	 * Tidy up external resources
	 */
	public void close() {
		deleteThumbnail();
	}

	/**
	 * Warn if close() was not called/effective
	 */
	@Override
	public void finalize() {
		if (deleteThumbnail())
			Logger.logError(new RuntimeException("Book object finalized and temp thumbnail exists"));
	}

	/**
	 * Delete any associated thumbnail
	 *
	 * @return
	 */
	private boolean deleteThumbnail() {
		if (THUMBNAIL != null && !THUMBNAIL.equals("")) {
			File f = new File(THUMBNAIL);
			if (!f.exists()) 
				return false;

			try {
				f.delete();
			} catch (Exception e) {
				Logger.logError(e);
			};

			return true;

		} else {
			return false;
		}
	}

	public String getAUTHOR() {
		return AUTHOR;
	}
	public void setAUTHOR(String aUTHOR) {
		AUTHOR = aUTHOR;
	}
	public String getTITLE() {
		return TITLE;
	}
	public void setTITLE(String tITLE) {
		TITLE = tITLE;
	}
	public String getISBN() {
		return ISBN;
	}
	public void setISBN(String iSBN) {
		ISBN = iSBN;
	}
	public String getDATE_PUBLISHED() {
		return DATE_PUBLISHED;
	}
	public void setDATE_PUBLISHED(String dATE_PUBLISHED) {
		DATE_PUBLISHED = dATE_PUBLISHED;
	}
	public String getPUBLISHER() {
		return PUBLISHER;
	}
	public void setPUBLISHER(String pUBLISHER) {
		PUBLISHER = pUBLISHER;
	}
	public String getPAGES() {
		return PAGES;
	}
	public void setPAGES(String pAGES) {
		PAGES = pAGES;
	}
	public String getTHUMBNAIL() {
		return THUMBNAIL;
	}
	public void setTHUMBNAIL(String tHUMBNAIL) {
		THUMBNAIL = tHUMBNAIL;
	}
	public String getGENRE() {
		return GENRE;
	}
	public void setGENRE(String gENRE) {
		GENRE = gENRE;
	}
	public String getDESCRIPTION() {
		return DESCRIPTION;
	}
	public void setDESCRIPTION(String dESCRIPTION) {
		DESCRIPTION = dESCRIPTION;
	}		
	
    public String getSERIES_NAME() {
		return SERIES_NAME;
	}

	public void setSERIES_NAME(String sERIES_NAME) {
		SERIES_NAME = sERIES_NAME;
	}

	public ArrayList<Author> getAUTHOR_ARRAY() {
		return AUTHOR_ARRAY;
	}

	public void setAUTHOR_ARRAY(ArrayList<Author> aUTHOR_ARRAY) {
		AUTHOR_ARRAY = aUTHOR_ARRAY;
	}

	public ArrayList<Series> getSERIES_ARRAY() {
		return SERIES_ARRAY;
	}

	public void setSERIES_ARRAY(ArrayList<Series> sERIES_ARRAY) {
		SERIES_ARRAY = sERIES_ARRAY;
	}

	private void readFromParcel(Parcel in) {
    	AUTHOR = in.readString();
    	TITLE = in.readString();
    	ISBN = in.readString();
    	DATE_PUBLISHED = in.readString();
    	PUBLISHER = in.readString();
    	PAGES = in.readString();
    	THUMBNAIL = in.readString();
    	GENRE = in.readString();
    	DESCRIPTION = in.readString();
    	SERIES_NAME = in.readString();
    	in.readTypedList(AUTHOR_ARRAY, Author.CREATOR);
    	in.readTypedList(SERIES_ARRAY, Series.CREATOR);
    }
	
    /**
	 * Support for creation via Parcelable
	 */	
    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
    	
    	public Book createFromParcel(Parcel in) {
    			return new Book(in);
    	}

    	public Book[] newArray(int size) {
    		return new Book[size];
    	}
    };
	
    
	/**
	 * Compare this book with book given in parameter.
	 * 
	 * @return	true if books are same otherwise returns false
	 */     
    @Override
	public boolean equals(Object obj){    	
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;   			
    	Book b = (Book) obj;    	
    	if(this.ISBN != null && this.ISBN.length() > 0 && b.getISBN() != null && b.getISBN().length() > 0){
    		if(this.ISBN.equals(b.getISBN())){
    			return true;
    		}else{
    			return false;
    		}    		
    	}else{
    		if(this.TITLE.equals(b.getTITLE()) && this.PUBLISHER.equals(b.getPUBLISHER())){    			
    			return true;
    		}else{
    			return false;
    		}    		
    	}		
		
	}
    
	/**
	 * Use ISBN like hash code for book - if ISBN is null, then use title and publisher.
	 * 
	 * @return	hash code of book
	 */    
    @Override
    public int hashCode() {    	
    	if(this.ISBN != null && this.ISBN.length() > 0){
    		return this.ISBN.hashCode();    		
    	}else{
    		String tmp = "";
    		if(this.TITLE != null)
    			tmp += this.TITLE;
    		if(this.PUBLISHER != null)
    			tmp += this.PUBLISHER;
    		return tmp.hashCode();
    	}    		    
    }    
}
