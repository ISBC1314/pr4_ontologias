/*
 * Created on Jul 18, 2005
 */
package org.mindswap.pellet.dig;

/**
 * @author Evren Sirin
 *
 */
public class DIGErrors {
    public static int	GENERAL_UNSPECIFIED_ERROR	=	0	;
    public static int	UNKNOWN_REQUEST	=	1	;
    public static int	MALFORMED_REQUEST	=	2	;
    public static int	UNSUPPORTED_OPERATION	=	3	;
    public static int	CANNOT_CREATE_NEW_KNOWLEDGE	=	4	;
    public static int	MALFORMED_KB_URI	=	5	;
    public static int	UNKNOWN_OR_STALE_KB_URI	=	6	;
    public static int	KB_RELEASE_ERROR	=	7	;
    public static int	MISSING_URI	=	8	;
    public static int	GENERAL_TELL_ERROR	=	9	;
    public static int	UNSUPPORTED_TELL_OPERATION	=	10	;
    public static int	UNKNOWN_TELL_OPERATION	=	11	;
    public static int	GENERAL_ASK_ERROR	=	12	;
    public static int	UNSUPPORTED_ASK_OPERATION	=	13	;
    public static int	UNKNOWN_ASK_OPERATION	=	14	;


     public static String[] codes = {
        "100","General Unspecified Error",
        "101","Unknown Request",
        "102","Malformed Request (XML error)",
        "103","Unsupported Operation",
        "201","Cannot create new knowledge",
        "202","Malformed KB URI",
        "203","Unknown or stale KB URI",
        "204","KB Release Error",
        "205","Missing URI",
        "300","General Tell Error",
        "301","Unsupported Tell Operation",
        "302","Unknown Tell Operation",
        "400","General Ask Error",
        "401","Unsupported Ask Operation",
        "402","Unknown Ask Operation"
    };
    
}
