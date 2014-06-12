/*
 * Created on Dec 6, 2004
 */
package org.mindswap.pellet.utils;

/**
 * A simple class to experiment with your JVM's garbage collector
 * and memory sizes for various data types.
 *
 * @author <a href="mailto:vlad@trilogy.com">Vladimir Roubtsov</a>
 */
public class MemUtils {    
    private static final Runtime runtime = Runtime.getRuntime();

    public static void runGC() throws Exception
    {
        // It helps to call Runtime.gc()
        // using several method calls:
        for (int r = 0; r < 4; ++ r) _runGC ();
    }

    private static void _runGC () throws Exception
    {
        long usedMem1 = usedMemory (), usedMem2 = Long.MAX_VALUE;
        for (int i = 0; (usedMem1 < usedMem2) && (i < 500); ++ i)
        {
            runtime.runFinalization();
            runtime.gc();
            Thread.yield();
            
            usedMem2 = usedMem1;
            usedMem1 = usedMemory();
        }
    }
    
    public static long usedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
}
