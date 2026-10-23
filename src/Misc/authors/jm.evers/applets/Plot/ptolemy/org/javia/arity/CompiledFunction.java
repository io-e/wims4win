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

import java.util.Random;

class CompiledFunction extends Function {
    //static final int INI_STACK_SIZE =  16;
    static final int MAX_STACK_SIZE = 128; //if stack ever grows above this likely something is wrong
    private static Random random = new Random();

    private double consts[];
    private Function funcs[];
    private byte code[];
    private int arity; // >= 0

    private static final ThreadLocal stacks = new ThreadLocal();

    CompiledFunction(int arity, byte[] code, double[] consts, Function funcs[]) {
        init(arity, code, consts, funcs);
    }

    /* This empty constructor is used only by SimpleCodeGen,
       which calls init() explicitly later.
    */
    CompiledFunction() {
        init(0, null, null, null);
    }

    void init(int arity, byte[] code, double[] consts, Function funcs[]) {
        this.arity  = arity;
        this.code   = code;
        this.consts = consts;
        this.funcs  = funcs;
    }

    public int arity() {
        return arity;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        int cpos = 0, fpos = 0;
        if (arity != 0) {
            buf.append("arity ").append(arity).append("; ");
        }
        for (int i = 0; i < code.length; ++i) {
            byte op = code[i];
            buf.append(VM.opcodeName[op]);
            if (op == VM.CONST) {
                buf.append(' ').append(consts[cpos++]);
            } else if (op == VM.CALL) {
                ++fpos;
                //buf.append(" {").append(funcs[fpos++].toString()).append('}');
            }
            buf.append("; ");
        }
        if (cpos != consts.length) {
            buf.append("\nuses only ").append(cpos).append(" consts out of ").append(consts.length);
        }
        if (fpos != funcs.length) {
            buf.append("\nuses only ").append(fpos).append(" funcs out of ").append(funcs.length);
        }
        return buf.toString();
    }

    private void checkArity(int nArgs) throws ArityException {
        if (arity() != nArgs) {
            throw new ArityException("Expected " + arity() + " arguments, got " + nArgs);
        }        
    }

    private double[] getStack() {
        double stack[];
        if ((stack = (double[]) stacks.get()) == null) {
            stacks.set(stack = new double[MAX_STACK_SIZE]);
        }
        return stack;
    }
    
    static final double NO_ARGS[] = new double[0];
    public double eval() throws ArityException {
        return eval(NO_ARGS);
    }

    public double eval(double x) throws ArityException {
        checkArity(1);
        double stack[] = getStack();
        stack[0] = x;
        exec(stack, 0);
        return stack[0];
    }

    public double eval(double x, double y) throws ArityException {
        checkArity(2);
        double stack[] = getStack();
        stack[0] = x;
        stack[1] = y;
        exec(stack, 1);
        return stack[0];
    }

    public double eval(double args[]) throws ArityException {
        checkArity(args.length);
        double stack[] = getStack();
        if (args.length > 0) {
            System.arraycopy(args, 0, stack, 0, args.length);
        }
        exec(stack, args.length - 1);
        return stack[0];
    }

    void exec(double s[], int p) {
        int finalP = execWithoutCheck(s, p);
        int expected = p - arity + 1;
        if (finalP != expected) {
            throw new Error("stack pointer after exec: expected " + 
                            expected + ", got " + finalP);
        }
    }
    
    int execWithoutCheck(double s[], int p) {
        byte[] code = this.code;
        int initialSP = p;
        int constp = 0;
        int funp   = 0;
        final double angleFactor = 1; // 1/Calc.cfg.trigFactor;
        
        // arguments, read from stack on exec entry
        // we don't use an array in order to avoid the dynamic allocation (new)
        // @see Compiler.MAX_ARITY
        double a0, a1, a2, a3, a4;
        a0 = a1 = a2 = a3 = a4 = Double.NaN;
        switch (arity) {
        case 5: a4 = s[p--];
        case 4: a3 = s[p--];
        case 3: a2 = s[p--];
        case 2: a1 = s[p--];
        case 1: a0 = s[p--];
        }

        //int expectedExitSP = p+1;
        int codeLen = code.length;
        for (int pc = 0; pc < codeLen; ++pc) {
            switch (code[pc]) {
            case VM.CONST: s[++p] = consts[constp++]; break;
            case VM.CALL: { 
                Function f = funcs[funp++];
                if (f instanceof CompiledFunction) { 
                    p = ((CompiledFunction) f).execWithoutCheck(s, p);
                } else {
                    int arity = f.arity();
                    p -= arity;
                    try {
                        double result;
                        switch (arity) {
                        case 0:
                            result = f.eval();
                            break;
                        case 1:
                            result = f.eval(s[p+1]);
                            break;
                        case 2:
                            result = f.eval(s[p+1], s[p+2]);
                            break;
                        default:
                            double args[] = new double[arity];
                            System.arraycopy(s, p+1, args, 0, arity);
                            result = f.eval(args);
                        }
                        s[++p] = result;                                      
                    } catch (ArityException e) {
                        throw new Error(""+e); //never
                    }
                }
                break;
            }
                
            case VM.RND: s[++p] = random.nextDouble(); break;
                    
            case VM.ADD: s[--p] += s[p+1]; break;
            case VM.SUB: s[--p] -= s[p+1]; break;
            case VM.MUL: s[--p] *= s[p+1]; break;
            case VM.DIV: s[--p] /= s[p+1]; break;
            case VM.MOD: s[--p] %= s[p+1]; break;
            case VM.POWER: s[--p] = MoreMath.pow(s[p], s[p+1]); break;
                
            case VM.UMIN: s[p] = -s[p]; break;
            case VM.FACT: s[p] = MoreMath.factorial(s[p]); break;                   
                
            case VM.SIN:  s[p] = Math.sin(s[p] * angleFactor); break;
            case VM.COS:  s[p] = Math.cos(s[p] * angleFactor); break;
            case VM.TAN:  s[p] = Math.tan(s[p] * angleFactor); break;
            case VM.ASIN: s[p] = MoreMath.asin(s[p]) / angleFactor; break;
            case VM.ACOS: s[p] = MoreMath.acos(s[p]) / angleFactor; break;
            case VM.ATAN: s[p] = MoreMath.atan(s[p]) / angleFactor; break;
                    
            case VM.EXP: s[p] = MoreMath.exp(s[p]); break;
            case VM.LOG: s[p] = MoreMath.log(s[p]); break;
                
            case VM.SQRT: s[p] = Math.sqrt(s[p]); break;
            case VM.CBRT: s[p] = MoreMath.cbrt(s[p]); break;
                
            case VM.SINH: s[p] = MoreMath.sinh(s[p]); break;
            case VM.COSH: s[p] = MoreMath.cosh(s[p]); break;
            case VM.TANH: s[p] = MoreMath.tanh(s[p]); break;
            case VM.ASINH: s[p] = MoreMath.asinh(s[p]); break;
            case VM.ACOSH: s[p] = MoreMath.acosh(s[p]); break;
            case VM.ATANH: s[p] = MoreMath.atanh(s[p]); break;
                    
            case VM.ABS:   s[p] = Math.abs(s[p]); break;
            case VM.FLOOR: s[p] = Math.floor(s[p]); break;
            case VM.CEIL:  s[p] = Math.ceil(s[p]); break;
            case VM.SIGN:  s[p] = s[p] > 0 ? 1 : s[p] < 0 ? -1 : 0; break;
                
            case VM.MIN:  s[--p] = Math.min(s[p], s[p+1]); break;
            case VM.MAX:  s[--p] = Math.min(s[p], s[p+1]); break;
            case VM.GCD:  s[--p] = MoreMath.gcd(s[p], s[p+1]); break;
            case VM.COMB: s[--p] = MoreMath.comb(s[p], s[p+1]); break;
            case VM.PERM: s[--p] = MoreMath.perm(s[p], s[p+1]); break;
                    
            case VM.LOAD0: s[++p] = a0; break;
            case VM.LOAD1: s[++p] = a1; break;
            case VM.LOAD2: s[++p] = a2; break;
            case VM.LOAD3: s[++p] = a3; break;
            case VM.LOAD4: s[++p] = a4; break;
            default:
                throw new Error("Unknown opcode " + code[pc]);
            }
        }
        return p;
        /*
        if (p != expectedExitSP) {
            throw new Error("stack pointer after exec: expected " + expectedExitSP +
                            " , got " + p);
        }
        */
    }
}
