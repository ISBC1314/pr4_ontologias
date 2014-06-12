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

package org.mindswap.pellet.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

public class FileUtils {
    public static boolean exists( String file ) {
        return new File( file ).exists();
    }
    
    public static String readURL(URL fileURL) throws IOException {
		return readAll(new InputStreamReader(fileURL.openStream()));
	}
		
	public static String readFile(File file) throws FileNotFoundException, IOException {
		return readAll(new FileReader(file));
	}
	
	public static String readFile(String fileName) throws FileNotFoundException, IOException {
		return readAll(new FileReader(fileName));
	}
	
	public static String readAll(Reader reader) throws IOException {
		StringBuffer buffer = new StringBuffer();
				
		BufferedReader in = new BufferedReader(reader);
		int ch;
		while ((ch = in.read()) > -1) {
			buffer.append((char)ch);
		}
		in.close();

		return buffer.toString();
	}

    public static String toURI(String fileName) {
    	if (fileName.startsWith("http://") || fileName.startsWith("file:/"))
    		return fileName;
    
    	File localFile = new File(fileName);
    	if (!localFile.exists())
    		throw new RuntimeException(new FileNotFoundException(localFile.getAbsolutePath() + " is not found"));
    
    	try {
    		return localFile.toURL().toExternalForm();
    	} catch (MalformedURLException e) {
    		throw new RuntimeException(fileName + " is not a valid URI");
    	}
    }	
}
