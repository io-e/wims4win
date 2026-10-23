/*
 * Copyright (C) 2007-2008 Mihai Preda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.javia.arity;

class EvalCase {
    String expr;
    double result;
    
    static final double ERR = -2, FUN = -3;

    EvalCase(String expr, double result) {
        this.expr = expr;
        this.result = result;
    }
}

class TestEval {
    static EvalCase cases[] = {
        new EvalCase(".", 0),
        new EvalCase("1+.", 1),
        new EvalCase("1", 1),
        new EvalCase("\u03c0", Math.PI),
        new EvalCase("2\u00d73", 6), //2*3
        new EvalCase("1+\u221a9*2", 7), //1+sqrt(9)*2
        new EvalCase("3\u221a 4", 6), //3*sqrt(4)
        new EvalCase("\u221a16sin(2\u03c0/4)", 4), //sqrt(16)*sin(2pi/4)
        new EvalCase("1+", EvalCase.ERR),
        new EvalCase("1+1", 2),
        new EvalCase("1+-1", 0),
        new EvalCase("-0.5", -.5),
        new EvalCase("+1e2", 100),
        new EvalCase("-2^3!", -64),
        new EvalCase("(-2)^3!", 64),
        new EvalCase("-2^1^2", -2),
        new EvalCase("--1", 1),
        new EvalCase("-3^--2", -9),
        new EvalCase("1+2)(2+3", 15),
        new EvalCase("1+2)!^-2", 1./36),
        new EvalCase("sin(0)", 0),
        new EvalCase("cos(0)", 1),
        new EvalCase("sin(-1--1)", 0),
        new EvalCase("-(2+1)*-(4/2)", 6),
        new EvalCase("-.5E-1", -.05),
        new EvalCase("1E1.5", EvalCase.ERR),
        new EvalCase("2 3 4", 24),
        new EvalCase("pi", Math.PI),
        new EvalCase("e", Math.E),
        new EvalCase("sin(pi/2)", 1),
        new EvalCase("f=sin(2x)", EvalCase.FUN),
        new EvalCase("f(pi/2)", 0),
        new EvalCase("a=3", 3),
        new EvalCase("b=a+1", 4),
        new EvalCase("f(x, y) = x*(y+1)", EvalCase.FUN),
        new EvalCase("f(a, b-a)", 6),
        new EvalCase(" f(a pi/4)", -1),
        new EvalCase("f (  1  +  1  , a+1)", 10),
        new EvalCase("g(foo) = f (f(foo, 1)pi/2)", EvalCase.FUN),
        new EvalCase("g(.5*2)", 0),
        new EvalCase("NaN", Double.NaN),
        new EvalCase("Inf", Double.POSITIVE_INFINITY),
        new EvalCase("Infinity", Double.POSITIVE_INFINITY),
        new EvalCase("-Inf", Double.NEGATIVE_INFINITY),
        new EvalCase("0/0", Double.NaN)
    };

    private static boolean equal(double a, double b) {
        return Math.abs(a-b) < 1E-15 ||
            Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    static boolean testEval() throws ArityException {
        final String spaces = "                                           ";
        boolean allOk = true;
        Symbols symbols = new Symbols();
        for (int i = 0; i < cases.length; ++i) {
            EvalCase c = cases[i];
            String strResult;
            boolean ok;
            try {
                double actual;
                if (Symbols.isDefinition(c.expr)) {
                    FunctionAndName fan = symbols.compile(c.expr);
                    symbols.define(fan);
                    Function f = fan.function;
                    actual = f.arity()==0 ? f.eval() : EvalCase.FUN;
                    strResult = (f.arity()==0 ? Util.doubleToString(actual, 1) : f.toString());
                } else {
                    actual = symbols.eval(c.expr);
                    strResult = Util.doubleToString(actual, 1);
                }
                ok = equal(c.result, actual);
            } catch (SyntaxException e) {
                strResult = e.toString();
                ok = c.result == EvalCase.ERR;
            }
            System.out.println((ok ? "" : "failed (expected " + c.result + "): ")
                               + c.expr
                               + spaces.substring(0, Math.max(15-c.expr.length(), 0)) + " = " 
                               + strResult);
            if (!ok) {
                allOk = false;
            }
        }
        return allOk;
    }
}


class FormatCase {
    public FormatCase(int rounding, double v, String s) {
        this.rounding = rounding;
        this.val = v;
        this.res = s;
    }
    
    public int rounding;
    public double val;
    public String res;            
}

class TestFormat {
    static FormatCase cases[] = { 
        new FormatCase(0, 0.1, "0.1"),
        new FormatCase(0, 0.12, "0.12"),
        new FormatCase(0, 0.001, "0.001"),
        new FormatCase(0, 0.0012, "0.0012"),
        new FormatCase(0, 0.0000001, "1E-7"),
        new FormatCase(0, 0.00000012, "1.2E-7"),
        new FormatCase(0, 0.123456789012345, "0.123456789012345"),

        new FormatCase(0, 0, "0"),
        new FormatCase(0, 1, "1"),
        new FormatCase(0, 12, "12"),
        new FormatCase(0, 1234567890.,   "1234567890"), 
        new FormatCase(0, 1000000000.,   "1000000000"),
        
        new FormatCase(0, 1.23456789012345,  "1.23456789012345"),
        new FormatCase(0, 12345.6789012345,  "12345.6789012345"),
        new FormatCase(0, 1234567890.12345,  "1234567890.12345"),  
        new FormatCase(0, 123456789012345.,   "1.23456789012345E14"), 
        new FormatCase(0, 100000000000000.,   "1E14"),
        new FormatCase(0, 120000000000000.,   "1.2E14"),
        new FormatCase(0, 100000000000001.,   "1.00000000000001E14"),

        new FormatCase(2, 0.1, "0.1"),
        new FormatCase(2, 0.00000012, "1.2E-7"),
        new FormatCase(1, 0.123456789012345, "0.12345678901235"),

        new FormatCase(2, 0, "0"),
        
        new FormatCase(1, 1.23456789012345,  "1.2345678901235"),
        new FormatCase(2, 1.23456789012345,  "1.234567890123"),
        
        new FormatCase(0, 12345.6789012345,  "12345.6789012345"),
        new FormatCase(1, 1234567890.12345,  "1234567890.1235"),  
        new FormatCase(2, 123456789012345.,  "1.234567890123E14"), 
        new FormatCase(1, 100000000000001.,  "1E14"),

        new FormatCase(0, 12345678901234567.,   "1.2345678901234568E16"),
        new FormatCase(1, 12345678901234567.,   "1.2345678901235E16"),

        new FormatCase(0, 99999999999999999.,   "1E17"),
        new FormatCase(0, 9999999999999999.,    "1E16"),
        new FormatCase(0, 999999999999999.,     "9.99999999999999E14"),
        new FormatCase(1, 999999999999999.,     "1E15"),
        new FormatCase(1, 999999999999994.,     "9.9999999999999E14"),

        new FormatCase(1, MoreMath.log2(1+.00002), "00000.28853612282487")
    };

    static boolean testFormat() {
        boolean ret = true;
        for (int i = 0; i < cases.length; ++i) {
            FormatCase c = cases[i];
            double v = Double.parseDouble(c.res);
            if (c.rounding == 0 && v != c.val) {
                System.out.println("wrong test? " + c.res + " " + v + " " + c.val);
            }
            String res = Util.doubleToString(c.val, c.rounding);
            if (!res.equals(c.res)) {
                System.out.println("Expected '" + c.res + "', got '" + res + "'. " + Double.toString(c.val));
                ret = false;
            }
            int nKeep = c.rounding == 0 ? 17 : 15 - c.rounding;
            //System.out.println("" + Double.toString(c.val) + " " + Util.round(c.val, nKeep) + " " + c.res + ", got " + res);
        }
        return ret;
    }
}

/**
   Runs unit-tests.<p>
   Usage: java -jar arity.jar
*/
public class UnitTest {
    /**
       Takes a single command-line argument, an expression; compiles and prints it.<p>
       Without arguments, runs the unit tests.
       @throws SyntaxException if there are errors compiling the expression.
     */
    public static void main(String argv[]) throws SyntaxException, ArityException {
        int size = argv.length;
        if (size == 0) {
            runUnitTests();
            profile();
        } else if (size == 1 && argv[0].equals("-profile")) {
            profile();
        } else {
            Symbols symbols = new Symbols();
            for (int i = 0; i < size; ++i) {
                FunctionAndName fan = symbols.compile(argv[i]);
                symbols.define(fan);
            }
            profile(symbols, argv[size-1]);
        }
    }

    static void profile(Symbols symbols, String str) throws SyntaxException, ArityException {
        Function f = symbols.compile(str).function;
        System.out.println("\n" + str + ": " + f);

        Runtime runtime = Runtime.getRuntime();        

        runtime.gc();
        /*
        long m1 = runtime.freeMemory();
        for (int i = 0; i < 200; ++i) {
            symbols.compile(str);
        }
        long m2 = runtime.freeMemory();
        System.out.println("compilation memory: " + (m1 - m2)/200 + " bytes");
        */
        
        runtime.gc();
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            symbols.compile(str);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("compilation time: " + (t2 - t1) + " us");
        
        double args[] = new double[f.arity()];
        /*
        runtime.gc();
        m1 = runtime.freeMemory();
        f.eval(args);
        m2 = runtime.freeMemory();
        if (m2 != m1) {
            System.out.println("execution memory: " + (m1 - m2) + " bytes");
        }
        */
  
        runtime.gc();
        t1 = System.currentTimeMillis();
        for (int i = 0; i < 100000; ++i) {
            f.eval(args);
        }
        t2 = System.currentTimeMillis();
        long delta = t2 - t1;        
        System.out.println("execution time: " + (delta > 100 ? ""+delta/100.+" us" :  ""+delta+" ns"));
    }

    private static final String profileCases[] = {
        //"1+1",
        "(100.5 + 20009.999)*(7+4+3)/(5/2)^3!)*2",
        "fun1(x)=(x+2)*(x+3)",
        "otherFun(x)=(fun1(x-1)*x+1)*(fun1(2-x)+10)",
        "log(x+30.5, 3)^.7*sin(x+.5)"
    };

    private static void profile() {
        String cases[] = profileCases;
        Symbols symbols = new Symbols();
        try {
            for (int i = 0; i < cases.length; ++i) {
                symbols.define(symbols.compile(cases[i]));
                profile(symbols, cases[i]);
            }
        } catch (Exception e) {
            throw new Error("" + e);
        }
    }

    static void runUnitTests() {
        checkCounter = 0;

        cheq(MoreMath.log(-1), Double.NaN);
        cheq(MoreMath.log(-0.03), Double.NaN);
        cheq(MoreMath.intLog10(-0.03), 0);
        cheq(MoreMath.intLog10(0.03), -2);
        cheq(MoreMath.intExp10(3), 1000);
        cheq(MoreMath.intExp10(-1), 0.1);
        cheq(Util.shortApprox( 1.235, 0.02),  1.24);
        cheq(Util.shortApprox( 1.235, 0.4),   1.2000000000000002);
        cheq(Util.shortApprox(-1.235, 0.02), -1.24);
        cheq(Util.shortApprox(-1.235, 0.4),  -1.2000000000000002);

        check(TestFormat.testFormat());
        try {
            check(TestEval.testEval());
        } catch (ArityException e) {
            System.out.println(""+e);
        }

        if (!allOk) {
            System.out.println("\n*** Some tests FAILED ***\n");
            System.exit(1);
        } else {
            System.out.println("\n*** All tests passed OK ***\n");
        }
    }

    static void cheq(double v1, double v2) {
        ++checkCounter;
        if (v1 != v2 && !(Double.isNaN(v1) && Double.isNaN(v2))) {
            allOk = false;
            //Log.log("check equal " + checkCounter + " failed: " + v1 + " " + v2);
        }
    }
    
    static void check(boolean cond) {
        ++checkCounter;
        if (!cond) {
            allOk = false;
            //Log.log("check " + checkCounter + " failed");
        }
    }

    static boolean allOk = true;
    static int checkCounter = 0;
}
