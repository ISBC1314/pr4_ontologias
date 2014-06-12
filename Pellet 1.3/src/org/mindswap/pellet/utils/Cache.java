// The MIT License
//
// Copyright (c) 2004 Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

/*
 * Created on Dec 30, 2003
 *
 */
package org.mindswap.pellet.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Maps remote URI's to local cached files so {#link org.mindswap.pellet.jena.ModelReader# ModelReader}
 * can use cached files when needed. Mappings can be added one by one or read from an index file. If the
 * use of cache is forced then local file will be used by the reader even if the remote file is available.
 * Otherwise, local files will only be used when the remote file cannot be read.   
 * 
 * 
 * @author Evren Sirin
 *
 */
public class Cache {
	protected Properties cache = new Properties();
	protected String localCacheDirectory = null;
	protected boolean forced = true;
	
	/**
	 * Forces the readers to use the cached copies of the files even if the remote
	 * file may be available. When the forcing of cache is enabled the reader will
	 * first check if the cached copy exists and then only try to use the remote file
	 * when there is no cached copy.
	 * 
	 * @param b
	 */
	public void setForced(boolean b) {
		forced = b;
	}
	
	/**
	 * Returns if the using of cache is forced.
	 * 
	 * @return
	 */
	public boolean isForced() {
		return forced;
	}
	
	/**
	 * Sets the cache dir for the inference engine to find the cached files when a
	 * file cannot be downparseed from its original URL. The cache dir should include
	 * a file named service.idx. This index file is a text file where each line is in
	 * the format
	 * [service description url]=[local filename]
	 * 
	 * The ':' characters in the url's should be escaped as "\:"  
	 * 
	 * @param dir sets the local cache directory. if null it forces not to use the cache.
	 * 			  if the given dir or index file inthat dir does not exist then nothing is
	 * 			  done   
	 */
	public void setLocalCacheDirectory(String dir) {
		localCacheDirectory = dir;
		if(dir == null) {
			cache.clear();
			System.out.println("INFO: Local cache directory is disabled");
		}
		else {
			String indexFileName = localCacheDirectory + File.separator + "service.idx"; 
			try {
				File indexFile = new File(indexFileName);
	
				cache = new Properties();				
			
				cache.load(new FileInputStream(indexFile));
	
				System.out.println("INFO: Cache has been initialized with " + cache.size() + " entries");
			} catch(FileNotFoundException e) {
				System.err.println("ERROR: Cache index file " + indexFileName + " cannot be found");
				localCacheDirectory = null;
			} catch(IOException e) {
				System.err.println("ERROR: Cache index file " + indexFileName + " has an invalid format");
				localCacheDirectory = null;
			}	
		}
	}

	
	/**
	 * Returns the cached File object for the given URI. Returns null if there isn't an entry in the
	 * cache for the given file or cached file is not found.
	 * 
	 * @param fileURI
	 * @return
	 */
	public File getCachedFile(String fileURI) {		
		File file = null;
		
		try {
			if(cache == null) return null;
			
			if(fileURI.endsWith("#")) fileURI = fileURI.substring(0, fileURI.length() - 1);
			
			String localFileName = cache.getProperty(fileURI);
			if(localFileName != null) {
			    if(localCacheDirectory == null)
			        file = new File(localFileName);
			    else
			        file = new File(localCacheDirectory + File.separator + localFileName);
				if(!file.exists()) {
					System.out.println("WARNING: Cached file does not exist " + file);
					file = null;
				}
			}
		}
		catch(Exception e) {
		}
		
		return file;		
	}
	
	
	/**
	 * Add a local file to be used as a cached copy for the given URI.
	 * 
	 * @param fileURI URI for the remote file
	 * @param localFile Path for the local cached copy
	 */
	public void addCachedFile(String fileURI, String localFile) {	
		cache.setProperty(fileURI, localFile);
	}
}
