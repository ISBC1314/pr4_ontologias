// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
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

package org.mindswap.pellet;

import java.util.Properties;

public class PelletQueryCGI extends PelletCGI {
	public static void main(String[] args) {
	    RunThread thread = new RunThread(args);
	    thread.start();
	    
	    int timeout = 300;
	    try {
            thread.join(timeout * 1000);
        } catch(InterruptedException e) {             
        }
        
        if(thread.isAlive()) {
            System.out.print("<p><b>TIMEOUT</b>: Timeout after " + timeout + " seconds");
        }
        
	    System.exit(0);
	}
	
	static class RunThread extends Thread {
	    String[] args;
	    
	    public RunThread(String[] args) {
	        this.args = args;
	    }
	    
	    public void run() {
			try {
			    String rdqlQuery = null;
			    
			    if(args.length == 1) {
					String queryString = args[0];		
					Properties params = parseArgs(queryString);
		
					rdqlQuery = params.getProperty("query");
			    }
						    
				System.out.println("<H1>Pellet Query Engine</H1>");
			    System.out.println("Enter a RDQL query in the text area<p>");
			    System.out.println("<textarea cols=85 rows=8 name=query wrap=off>");
			    if(rdqlQuery != null)
			        System.out.println(rdqlQuery);
			    System.out.println("</textarea>");
			    System.out.println("<p><input type=\"button\" value=\"Run Query\" name=\"submitBtn\" onclick=\"sendIt()\">");
				
				if(rdqlQuery != null) {		
				    System.out.println("<H1>Results</H1>");
				    
					PelletQuery pelletQuery = new PelletQuery();
					pelletQuery.setFormatHTML( true );
					pelletQuery.run( rdqlQuery );
				}
			} catch (Exception e) {
	           	e.printStackTrace(System.out);
				System.out.flush();
			}
	    }
	}
}
