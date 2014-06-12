//The MIT License
//
//Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to
//deal in the Software without restriction, including without limitation the
//rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
//sell copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.

package org.mindswap.pellet.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Create a table data structure that has a list of column names and list of data rows. The only
 * function of this class is to print the data in a table format. Data can be given at once by the
 * constructor or can be added incrementally with the addRow function.
 * 
 * 
 * @author Evren Sirin
 */
public class TableData {
    Collection data;

    List colNames;

    boolean[] rightAligned;

    int colWidths[] = null;

    String colSep = " | ";

    public TableData( Collection data, List colNames ) {
        this.data = data;
        this.colNames = colNames;

        int cols = colNames.size();
        colWidths = new int[cols];
        rightAligned = new boolean[cols];
    }

    public TableData( List colNames ) {
        this.data = new ArrayList();
        this.colNames = colNames;

        int cols = colNames.size();
        colWidths = new int[cols];
        rightAligned = new boolean[cols];
    }

    public TableData( String[] colNames ) {
        this( Arrays.asList( colNames ) );
    }
    
    public void setAlignment( boolean[] rightAligned ) {
        if( rightAligned.length != colNames.size() )
            throw new IllegalArgumentException( "Alignment has " + rightAligned.length
                + " elements but table has " + colNames.size() + " columns" );
        
        this.rightAligned = rightAligned;
    }

    public void setrightAligned( int colIndex, boolean rightAligned ) {
        this.rightAligned[colIndex] = rightAligned;
    }
    
    /**
     * 
     * @deprecated Use {@link add(List)} instead
     */
    public void addRow( List row ) {
        add( row );
    }

    public void add( List row ) {
        if( row.size() != colNames.size() )
            throw new IllegalArgumentException( "Row has " + row.size()
                + " elements but table has " + colNames.size() + " columns" );

        data.add( row );
    }

    public void print( OutputFormatter out ) {
        print( out.getWriter(), out.isFormatHTML() );
    }

    public void print( OutputStream writer ) {
        print( new PrintWriter( writer ), false );
    }

    public void print( Writer writer ) {
        print( writer, false );
    }

    public void print( OutputStream writer, boolean formatHTML ) {
        print( new PrintWriter( writer ), formatHTML );
    }

    public void print( Writer writer, boolean formatHTML ) {
        if( formatHTML )
            printHTML( writer );
        else
            printText( writer );
    }

    private void printHTML( Writer writer ) {
        PrintWriter pw = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(
            writer );

        pw.println( "<table border=1>" );
        // Column headings
        pw.println( "  <tr>" );
        for( int col = 0; col < colNames.size(); col++ ) {
            String s = (String) colNames.get( col );
            pw.print( "    <th>" );
            pw.print( s );
            pw.print( "</th>" );
            pw.println();
        }
        pw.println( "  </tr>" );

        for( Iterator i = data.iterator(); i.hasNext(); ) {
            Collection rowData = (Collection) i.next();
            pw.println( "  <tr>" );
            Iterator j = rowData.iterator();
            for( int col = 0; j.hasNext(); col++ ) {
                Object value = j.next();
                String s = (value == null) ? "<null>" : value.toString();

                pw.print( "    <td>" );
                pw.print( s );
                pw.print( "</td>" );
                pw.println();
            }
            pw.println( "  </tr>" );
        }
        pw.println( "</table>" );

        pw.flush();
    }

    private void printText( Writer writer ) {
        PrintWriter pw = (writer instanceof PrintWriter) ? (PrintWriter) writer : new PrintWriter(
            writer );

        computeHeaderWidths();
        computeRowWidths();

        int lineWidth = computeLineWidth();
        int numCols = colNames.size();

        String[] row = new String[numCols];
        for( int col = 0; col < row.length; col++ ) {
            row[col] = colNames.get( col ).toString();
        }
        printRow( pw, row );

        for( int i = 0; i < lineWidth; i++ )
            pw.print( '=' );
        pw.println();


        for( Iterator i = data.iterator(); i.hasNext(); ) {
            Collection rowData = (Collection) i.next();

            Iterator j = rowData.iterator();
            for( int col = 0; j.hasNext(); col++ ) {
                Object value = j.next();
                row[col] = value == null ? "<null>" : value.toString();
            }
            printRow( pw, row );
        }

        pw.flush();
    }

    private void printRow( PrintWriter pw, String[] row ) {
        for( int col = 0; col < row.length; col++ ) {
            String s = row[col];
            int pad = colWidths[col];
            StringBuffer sbuff = new StringBuffer( 120 );

            if( col > 0 )
                sbuff.append( colSep );

            if( !rightAligned[col] )
                sbuff.append( s );
            
            for( int j = 0; j < pad - s.length(); j++ )
                sbuff.append( ' ' );
            
            if( rightAligned[col] )
                sbuff.append( s );

            pw.print( sbuff );
        }
        pw.println();
    }

    private int computeLineWidth() {
        int numCols = colWidths.length;
        int lineWidth = 0;
        for( int i = 0; i < numCols; i++ )
            lineWidth += colWidths[i];

        lineWidth += (numCols - 1) * colSep.length();

        return lineWidth;
    }

    private void computeHeaderWidths() {
        Iterator k = colNames.iterator();
        for( int col = 0; k.hasNext(); col++ ) {
            Object value = k.next();
            String str = (value == null) ? "<null>" : value.toString();
            colWidths[col] = str.length();
        }
    }

    private void computeRowWidths() {
        for( Iterator i = data.iterator(); i.hasNext(); ) {
            Collection rowData = (Collection) i.next();

            Iterator j = rowData.iterator();
            for( int col = 0; j.hasNext(); col++ ) {
                Object value = j.next();
                String str = (value == null) ? "<null>" : value.toString();

                if( colWidths[col] < str.length() )
                    colWidths[col] = str.length();
            }
        }
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColCount() {
        return colNames.size();
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        printText( sw );

        return sw.toString();
    }
}