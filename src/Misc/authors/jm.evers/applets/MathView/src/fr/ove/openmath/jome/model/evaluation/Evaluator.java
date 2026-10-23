/*
$Id: Evaluator.java,v 1.3 2003/02/18 11:48:47 sander Exp $
*/


/*
Copyright (C) 2001-2002 Mainline Project (I3S - ESSI - CNRS -UNSA)

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For further information on the GNU Lesser General Public License,
see: http://www.gnu.org/copyleft/lesser.html
For further information on this library, contact: mainline@essi.fr
*/


package fr.ove.openmath.jome.model.evaluation;

public final class Evaluator {
    public static String evaluate(String operand1, String operand2, String anOperator) {
        String oper1 = removeBrackets(operand1);
        String oper2 = removeBrackets(operand2);
        
        double result;
        
        if (anOperator.equals("+")) {
            if (isNumber(oper1) && isNumber(oper2))
                return cutDecimal((Double.valueOf(oper1)).doubleValue() + (Double.valueOf(oper2)).doubleValue());
            else
                // Si la seconde opérande est un -, alors il ne faudra pas insérer le + entre les 2
                anOperator = (oper2.charAt(0) == '-') ? "" : anOperator;
        }
        else if (anOperator.equals("-")) {
            if (isNumber(oper1) && isNumber(oper2))
                return cutDecimal((Double.valueOf(oper1)).doubleValue() - (Double.valueOf(oper2)).doubleValue());
        }
        else if (anOperator.equals("*")) {
            if (isNumber(oper1) && isNumber(oper2))
                return cutDecimal((Double.valueOf(oper1)).doubleValue() * (Double.valueOf(oper2)).doubleValue());
        }
        else if (anOperator.equals("/")) {
            if (isNumber(oper1) && isNumber(oper2))
                return cutDecimal((Double.valueOf(oper1)).doubleValue() / (Double.valueOf(oper2)).doubleValue());
        }
        else if (anOperator.equals("^")) {
            if (isNumber(oper1) && isNumber(oper2))
                return cutDecimal(Math.pow((Double.valueOf(oper1)).doubleValue(), (Double.valueOf(oper2)).doubleValue()));
        }
            
        return operand1 + anOperator + operand2;
    }
    
    public static String evaluate(String function, String argument) {
        function = function.toLowerCase();
        
        argument = removeBrackets(argument);
        
        if (function.equals("sin")) {
            if (isNumber(argument))
                return cutDecimal(Math.sin((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("arcsin")) {
            if (isNumber(argument))
                return cutDecimal(Math.asin((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("cos")) {
            if (isNumber(argument))
                return cutDecimal(Math.cos((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("arccos")) {
            if (isNumber(argument))
                return cutDecimal(Math.acos((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("tan")) {
            if (isNumber(argument))
                return cutDecimal(Math.tan((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("arctan")) {
            if (isNumber(argument))
                return cutDecimal(Math.atan((Double.valueOf(argument)).doubleValue()));
        }
        if (function.equals("sqrt")) {
            if (isNumber(argument))
                return cutDecimal(Math.sqrt((Double.valueOf(argument)).doubleValue()));
        }
            
        return function + "(" + argument + ")";
    }
    
    public static int type(String argument) {

        argument = removeBrackets(argument);
        
        if (isNumber(argument))
            return 0;
        else
            return 1;
    }
    
    /**
    * Analyzes a token and determines whether it is a number or not.<BR>
    *
    * @param token the token to analyze.
    * @return <CODE>true</CODE> if the token is a number. <CODE>false</CODE> otherwise.
    */
    private static boolean isNumber(String token) {
        char oneChar;
        
        token = removeBrackets(token);
        
        for (int i = 0; i < token.length(); i++) {
            oneChar = token.charAt(i);
            if (!Character.isDigit(oneChar) && (oneChar != '.'))
                if (!((i == 0) && (oneChar == '-')))
                    return false;
        }
        return true;
    }
    
    /**
    *
    */
    private static String removeBrackets(String token) {
        if ((token.charAt(0) == '(') && (token.charAt(token.length() - 1) == ')'))
            return token.substring(1, token.length() - 1);
        else
            return token;
    }
    
    /**
    *
    */
    private static String cutDecimal(double value) {
        if ((value - Math.floor(value)) < 10e-5)
            return "" + (int) Math.floor(value);
        else
            return "" + value;
    }
}
