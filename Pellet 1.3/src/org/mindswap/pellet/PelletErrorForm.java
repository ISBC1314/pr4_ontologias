//The MIT License
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

/**
 * Print HTML form to submit error messages.
 *
 */
public class PelletErrorForm {
  public static String getForm(Exception ex, Pellet p)
  {
    StringBuffer sb = new StringBuffer();

    String st = "";
    java.io.StringWriter sw = new java.io.StringWriter();
    ex.printStackTrace(new java.io.PrintWriter(sw));
    st = sw.toString();

    sb.append("<h2>Pellet Error Report</h2>\n");

    sb.append("<h3>We're sorry, but you have encountered a fatal error while using Pellet.  Please take the time to fill out this report to let us know what happened.</h3><br>\n");

    sb.append("<form action=/cgi-bin/2003/pellet/roundup-submit method=post>\n");

    sb.append("<table>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Name: </td>\n");
    sb.append("<td class=\"NoBorder\"><input type=text name=\"name\"></td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Your email: </td>\n");
    sb.append("<td class=\"NoBorder\"><input type=text name=\"email\"></td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Description: </td>\n");
    sb.append("<td class=\"NoBorder\"><textarea name=description cols=30 rows=8></textarea></td>\n");
    sb.append("</tr>\n");

    sb.append("</table><br>\n");

    sb.append("<input type=submit value=\"Submit Bug Report\">\n");

    sb.append("<input type=hidden value=\""+st+"\" name=stacktrace>\n");

    if (p.inFile != null)
      sb.append("<input type=hidden value=\""+p.inFile+"\" name=inputFile>\n");

    if (p.inString != null)
    {
      String rdfString = p.inString;
      // replace any single quotes with double quotes
      rdfString = rdfString.replace('\'','"');
      // now that there's no single quotes, we'll wrap the rdf in single quotes and the escaping
      // should work ok and not kill the html.
      sb.append("<input type=hidden value='"+rdfString+"' name=inputString>\n");
    }

    if (p.inFormat != null)
      sb.append("<input type=hidden value=\""+p.inFormat+"\" name=inputFormat>\n");

    if (p.checkConsistency)
    {
      if (p.conclusionsFile != null)
        sb.append("<input type=hidden value=\""+p.conclusionsFile+"\" name=coFile>\n");
      if (p.conclusionsFormat != null)
        sb.append("<input type=hidden value=\""+p.conclusionsFormat+"\" name=coFormat>\n");
      if (p.conclusionsString != null)
        sb.append("<input type=hidden value=\""+p.conclusionsString+"\" name=coString>\n");
    } // if

    sb.append("</form>\n");
    sb.append("<h4>The error was: </h4>"+st.replaceAll("\n","<br>")+"<br>");

    return sb.toString();
  } // getForm

  public static String getForm2(Pellet p)
  {
    StringBuffer sb = new StringBuffer();

    sb.append("<br><hr><br><h4>Think these results are wrong?  Let us know.</h4><br>\n");

    sb.append("<form action=/cgi-bin/2003/pellet/roundup-submit method=post>\n");

    sb.append("<table>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Name: </td>\n");
    sb.append("<td class=\"NoBorder\"><input type=text name=\"name\"></td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Your email: </td>\n");
    sb.append("<td class=\"NoBorder\"><input type=text name=\"email\"></td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>\n");
    sb.append("<td class=\"NoBorder\">Description: </td>\n");
    sb.append("<td class=\"NoBorder\"><textarea name=description cols=30 rows=8></textarea></td>\n");
    sb.append("</tr>\n");

    sb.append("</table><br>\n");

    sb.append("<input type=submit value=\"Submit Bug Report\">\n");

    if (p.inFile != null)
      sb.append("<input type=hidden value=\""+p.inFile+"\" name=inputFile>\n");

    if (p.inString != null)
    {
      String rdfString = p.inString;
      // replace any single quotes with double quotes
      rdfString = rdfString.replace('\'','"');
      // now that there's no single quotes, we'll wrap the rdf in single quotes and the escaping
      // should work ok and not kill the html.
      sb.append("<input type=hidden value='"+rdfString+"' name=inputString>\n");
    }

    if (p.inFormat != null)
      sb.append("<input type=hidden value=\""+p.inFormat+"\" name=inputFormat>\n");

    if (p.checkConsistency)
    {
      if (p.conclusionsFile != null)
        sb.append("<input type=hidden value=\""+p.conclusionsFile+"\" name=coFile>\n");
      if (p.conclusionsFormat != null)
        sb.append("<input type=hidden value=\""+p.conclusionsFormat+"\" name=coFormat>\n");
      if (p.conclusionsString != null)
        sb.append("<input type=hidden value=\""+p.conclusionsString+"\" name=coString>\n");
    } // if

    sb.append("</form>\n");

    return sb.toString();
  } // getForm
} // PelletErrorForm
