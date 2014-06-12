/*
 * Created on Oct 21, 2005
 */
package org.mindswap.pellet.taxonomy;


public interface ClassifyProgress {
	public void classificationStarted( int classCount );
	public void realizationStarted( int instanceCount );
	public void taskFinished();
	
	public void startClass( String uri );
	public void startIndividual( String uri );
	
	public boolean isCanceled();
}