/*
 * Created on Oct 20, 2005
 */
package org.mindswap.pellet.taxonomy;


/**
 * @author Evren Sirin
 *
 */
public class SilentClassifyProgress implements ClassifyProgress {
    public SilentClassifyProgress() {
    }

	public void classificationStarted( int classCount ) {	    
	}
	
	public void realizationStarted( int instanceCount ) {	    
	}

    public void startClass( String uri ) {
    }

    public void startIndividual( String uri ) {
    }

    public boolean isCanceled() {
        return false;
    }

    public void taskFinished() {
    }
}
