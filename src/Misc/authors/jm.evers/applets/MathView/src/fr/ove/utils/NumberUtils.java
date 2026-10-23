/*
$Id: NumberUtils.java,v 1.2 2003/02/18 11:48:48 sander Exp $
*/


package fr.ove.utils;

import java.text.NumberFormat;

/**
*
* @author © 1999 DIRAT Laurent
* @version 2.0  28/06/1999
*/
public class NumberUtils {
    /**
    * Analyzes a token and determines whether it is a number (integer or float) or
    * not.<BR>
    * If not, considers this token as a variable.
    *
    * @param token the token to anlazye.
    * @return <CODE>true</CODE> if the token is a number. <CODE>false</CODE> otherwise.
    */
    public static boolean isNumber(String token) {
        char oneChar;
        
        for (int i = 0; i < token.length(); i++) {
            oneChar = token.charAt(i);
            
            if (!Character.isDigit(oneChar) && (oneChar != '.'))
                return false;
        }
        return true;
    }
    
    /**
    * Analyzes the token and determines wheter it is an integer or a float. We assume that
    * the specified token is already a number.
    * @param token the token to analyze.
    * @return <CODE>true</CODE> if the token is a float. <CODE>false</CODE>
    * if the token is an integer.
    */
    public static boolean isFloat(String token) {
        for (int i = 0; i < token.length(); i++)
            if (token.charAt(i) == '.')
                return true;
                
        return false;
    }
    
    /**
    * Formats the specified double value with two digits after decimal place
    * @param dValue the value to format.
    */
    public static String formatDouble(String dValue) {
        NumberFormat form;
        form = NumberFormat.getInstance();//get local number format instance
        form.setMaximumFractionDigits(2);//set for 2 digits only after decimal place
        form.setMinimumFractionDigits(1);//set for 2 digits after decimal place
        form.setGroupingUsed(false);//no comma for thousands
        form.setMinimumIntegerDigits(1);//leading 0 if < 1

        try {
            return form.format((Double.valueOf(dValue)).doubleValue());
        } catch (Exception execp) {
             return dValue;
        }
    }
}