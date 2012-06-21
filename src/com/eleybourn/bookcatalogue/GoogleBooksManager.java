package com.eleybourn.bookcatalogue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.os.Bundle;

// ENHANCE: Get editions via: http://books.google.com/books/feeds/volumes?q=editions:ISBN0380014300

public class GoogleBooksManager {

	static public void searchGoogle(String mIsbn, String author, String title, Bundle bookData, boolean fetchThumbnail, boolean returnBookList) {
		//replace spaces with %20
		author = author.replace(" ", "%20");
		title = title.replace(" ", "%20");

		String path = "http://books.google.com/books/feeds/volumes";
		if (mIsbn.equals("")) {
			path += "?q=" + "intitle:"+title+"+inauthor:"+author+"";
		} else {
			path += "?q=ISBN" + mIsbn;
		}
		URL url;

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser;
		SearchGoogleBooksHandler handler = new SearchGoogleBooksHandler();
		SearchGoogleBooksEntryHandler entryHandler;
		Bundle tmp = new Bundle();
		if(returnBookList){
			entryHandler = new SearchGoogleBooksEntryHandler(tmp, fetchThumbnail);
		}else{
			entryHandler = new SearchGoogleBooksEntryHandler(bookData, fetchThumbnail);
		}		
	
		try {
			url = new URL(path);
			parser = factory.newSAXParser();
			int count = 0;
			// We can't Toast anything from here; it no longer runs in UI thread. So let the caller deal 
			// with any exceptions.
			parser.parse(Utils.getInputStream(url), handler);
			count = handler.getCount();
			if (count > 0) {
				if(returnBookList){
					ArrayList<Book> books = new ArrayList<Book>();					
					String[] ids = handler.getId();
					for(String id : ids){						
						if(id != null && id.length() > 0){
							url = new URL(id);
							parser = factory.newSAXParser();
							parser.parse(Utils.getInputStream(url), entryHandler);
							Book book = new Book(tmp);
							books.add(book);
							tmp.clear();
						}
					}
					bookData.putSerializable(CatalogueDBAdapter.KEY_BOOKLIST, books);
				}else{
					String id = handler.getId()[0];
					url = new URL(id);
					parser = factory.newSAXParser();
					parser.parse(Utils.getInputStream(url), entryHandler);
				}
			}
			return;
		} catch (MalformedURLException e) {
			Logger.logError(e);
		} catch (ParserConfigurationException e) {
			Logger.logError(e);
		} catch (SAXException e) {
			Logger.logError(e);
		} catch (Exception e) {
			Logger.logError(e);
		}
		return;
	}
	
}
