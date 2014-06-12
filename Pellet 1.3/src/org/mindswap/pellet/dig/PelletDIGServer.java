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

package org.mindswap.pellet.dig;

/*
 * Created on Jul 19, 2005
 */
import java.net.BindException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.Pellet;
import org.mindswap.pellet.utils.VersionInfo;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.MultiException;

/**
 * An implementation of DIG server. This is a simple HTTP server that listens for the DIG 
 * commands and processes them through PelletDIGReasoner. 
 * 
 * The port that the server uses can be selected by the command line option:
 * <pre>
 * Usage: java PelletDIGServer [-port portNum]
 *  -port portNum           The port number user by the server (default
 *                          port number used is 8081)
 *  -help                   Print this information
 * </pre>  
 * 
 * @author Evren Sirin
 *  
 */
public class PelletDIGServer extends AbstractHttpHandler implements HttpHandler {
    private static final long serialVersionUID = 5605350732186236386L;

    protected static Log log = LogFactory.getLog( PelletDIGServer.class );
    
    private PelletDIGReasoner reasoner;

    public PelletDIGServer() {
        reasoner = new PelletDIGReasoner();
    }

	public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response) { 
        try {
            
            response.setContentType( "text/html" );

            reasoner.process( request.getInputStream(), response.getOutputStream() );

        } catch(Exception e) {            
            e.printStackTrace();
        }
    }

    
    public static int DEFAULT_PORT = 8081;
    
	static void usage() {
		System.out.println("Pellet DIG Server");
		System.out.println("DIG Server that is backed by Pellet reasoner");
		System.out.println("");
		System.out.println("Usage: java PelletDIGServer [-port portNum]");
		System.out.println("   -port portNum           The port number user by the server (default");		
		System.out.println("                           port number used is 8081)");		
		System.out.println("   -help                   Print this information");		
	}

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equalsIgnoreCase("-help")) {
				usage();
				System.exit(0);
			} 
			else if (arg.equalsIgnoreCase("-port")) { 
			    try {
                    port = Integer.parseInt( args[++i] );
                } catch(NumberFormatException e1) {
    				System.err.println("Invalid port number: " + args[i]);
    				System.exit(1);
                }
			} 
			else {
				System.err.println("Unrecognized option: " + arg);
				usage();
				System.exit(1);
			}
		}
                
        try {
            // Create the server
            HttpServer server=new HttpServer();
              
            // Create a port listener
            SocketListener listener=new SocketListener();
            listener.setPort( port );
            listener.setMinThreads( 2 );
            listener.setMaxThreads( 10 );
            server.addListener( listener );

            // Create a context 
            HttpContext context = server.addContext("/");
            context.addHandler(new PelletDIGServer());

            // Start the http server
            server.start ();

            VersionInfo vinfo = Pellet.getVersionInfo();
            System.out.println();
            System.out.print( "PelletDIGServer " );
            System.out.print( "Version " + vinfo.getVersionString() );
            System.out.print( " (" + vinfo.getReleaseDate() + ")");
            System.out.println();
            System.out.println( "Port: " + port );
            
            log.debug( "Debug is enabled" );
        } 
        catch(Exception e) {
            if( e instanceof MultiException && ((MultiException) e).getException(0) instanceof BindException )
                System.err.println( "Cannot start server. Port " + port + " is already in use!");
            else
                e.printStackTrace();
            System.exit( 0 );
        }
    }

}