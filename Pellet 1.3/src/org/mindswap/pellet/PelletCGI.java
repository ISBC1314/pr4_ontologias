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


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.Properties;


public class PelletCGI {
	// We should use a servlet or something better for this
	// but let's do it quick and dirty way for now
	public static void main(String[] args) {
		printHeader();
        Pellet pellet = null;
		try {
			BufferedReader in = (args.length == 0)
					? new BufferedReader(new InputStreamReader(System.in))
					: new BufferedReader(new StringReader(args[0]));
			String queryString = in.readLine();		
			Properties params = parseArgs(queryString);

			pellet = new Pellet();

			pellet.setFormatHTML(true);
			pellet.setTimeout(60); // in seconds

			pellet.setInFile  (params.getProperty("inputFile"));
			pellet.setInFormat(params.getProperty("inputFormat"));
			pellet.setInString(params.getProperty("inputString"));
			pellet.setConclusionsFile  (params.getProperty("conclusionsFile"));
			pellet.setConclusionsFormat(params.getProperty("conclusionsFormat"));
			pellet.setConclusionsString(params.getProperty("conclusionsString"));
			pellet.setClassifyFormat(params.getProperty("classifyFormat"));
			pellet.setQueryFile    (params.getProperty("queryFile"));
			pellet.setQueryString  (params.getProperty("queryString"));
            pellet.setQueryFormat  (params.getProperty("queryFormat"));

            pellet.setSpecies((params.getProperty("Species") != null)?Pellet.SPECIES_ON:Pellet.SPECIES_OFF);
			pellet.setConsistency(params.getProperty("Consistency") != null);
			pellet.setUnsat(params.getProperty("Unsat") != null);
			pellet.setRealize(params.getProperty("Realize") != null);
			pellet.setEconnEnabled(params.getProperty("Econn") != null);

			pellet.run();

            System.out.println(PelletErrorForm.getForm2(pellet));
		} catch (Exception e) {

			//   log error
			//   url/text of ontology
			//   give form for leaving their email
			//   and comments
            if (pellet != null)
            	System.out.println(PelletErrorForm.getForm(e,pellet));
            else 
            	e.printStackTrace(System.out);
			System.out.flush();
		}

		printFooter();
	}

	public static Properties parseArgs(String queryString) throws Exception {
		Properties props = new Properties();

		String[] params = queryString.split("&");
		for(int i = 0; i < params.length; i++) {
			String[] p = params[i].split("=");
			if(p.length == 2) {
			    String decoded = URLDecoder.decode(p[1], "ISO-8859-1");
				props.setProperty(p[0], decoded);
			}
		}

		return props;
	}

	public static void printHeader() {
		System.out.println("<html>");
		System.out.println("<head>");
		System.out.println("<title>Pellet Results</title>");
		System.out.println("<style>");
		System.out.println("table {");
		System.out.println("  background-color:#FFF;");
		System.out.println("  border-collapse:collapse;");
		System.out.println("}");
		System.out.println("th {");
		System.out.println("  background-color:#FFF;");
		System.out.println("  border:2px solid black;");
		System.out.println("  padding:2px;");
		System.out.println("}");
		System.out.println("td {");
		System.out.println("  background-color:#FFF;");
		System.out.println("  border:2px solid black;");
		System.out.println("  padding:2px;");
		System.out.println("}");
		System.out.println("td.NoBorder {");
		System.out.println("  background-color:#FFF;");
		System.out.println("  border: none;");
		System.out.println("  padding:2px;");
		System.out.println("}");
		System.out.println("</style>");
		System.out.println("</head>");
		System.out.println("<body>");
		System.out.println("<H1>Results</H1>");
	}
	
	public static void printFooter() {	    
	    System.out.println("</body>");
	    System.out.println("</html>");
	}
}
