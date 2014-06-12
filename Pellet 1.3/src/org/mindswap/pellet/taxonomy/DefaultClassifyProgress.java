/*
 * Created on Oct 20, 2005
 */
package org.mindswap.pellet.taxonomy;

import java.lang.reflect.InvocationTargetException;

import javax.swing.ProgressMonitor;


/**
 * @author Evren Sirin
 *
 */
public class DefaultClassifyProgress implements ClassifyProgress {
	private class ProxiedClassifyProgress implements ClassifyProgress {
    		private int count;

		private int current;
		private ProgressMonitor monitor;
		private int progress;
		
		private ProxiedClassifyProgress(int millisToPopup) {
			monitor = new ProgressMonitor(null, "Pellet classification", "Initializing...", 0, 0);
			monitor.setMillisToPopup(millisToPopup);
		}
		
		public void classificationStarted(int classCount) {

	        current = 0;
	        count = classCount;
	        
	        progress = 0;
	        monitor.setProgress( progress );
	        monitor.setMaximum( classCount );
	        
	        monitor.setNote( "Starting classification..." );
	        
		}

		public boolean isCanceled() {
			return monitor.isCanceled();
		}

		public void realizationStarted(int instanceCount) {
			current = 0;
			count = instanceCount;

	        int currMax = monitor.getMaximum();
	        if( currMax == 0 ) {
	            monitor.setProgress( 0 );
	            monitor.setMaximum( instanceCount );
	        }
	        else {
	            monitor.setProgress( currMax );
	            monitor.setMaximum( currMax + instanceCount );
	        }
	        monitor.setProgress( progress );
	        
	        monitor.setNote( "Starting realization..." );
		}

		public void startClass(String uri) {
			monitor.setProgress( ++progress );
	        String note = "Class (" +  ++current + " out of " + count + "): " + uri;
	        monitor.setNote( note );    
		}

		public void startIndividual(String uri) {
			monitor.setProgress( ++progress );
	        String note = "Individual (" +  ++current + " out of " + count + "): " + uri;
	        monitor.setNote( note );
		}

		public void taskFinished() {
			monitor.close();
		}
    	
    }
	private ProxiedClassifyProgress proxy;

 
    public DefaultClassifyProgress() {
        this( 1000 );
    }
    
    public DefaultClassifyProgress( final int millisToPopup ) {
    		while (proxy == null) {
    			try {
    				javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
    					public void run() {
						proxy = new ProxiedClassifyProgress(millisToPopup);
    					}
    				});
    			} catch (InterruptedException e) {
    				System.err.println("Interrupted while creating monitor proxy.");
    			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
    			}
    		}
    }
    
    public void classificationStarted(final int classCount ) {
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
    			public void run() {
    				proxy.classificationStarted(classCount);
    			}
    		});
    }
    
    public boolean isCanceled() {
        return proxy.isCanceled();
    }

    public void realizationStarted( final int instanceCount ) {
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				proxy.realizationStarted(instanceCount);
			}
		});
    }

    public void startClass( final String uri ) {
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				proxy.startClass(uri);
			}
		});    
    }    

    public void startIndividual( final String uri ) {
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				proxy.startIndividual(uri);
			}
		}); 
    }

    public void taskFinished() {
    		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				proxy.taskFinished();
			}
		});
    }
}
