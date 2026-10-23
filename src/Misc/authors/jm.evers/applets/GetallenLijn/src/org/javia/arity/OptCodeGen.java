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

/* Optimizing Code Generator
   Reads tokens in RPN (Reverse Polish Notation) order,
   and generates VM opcodes,
   doing constant-folding optimization.
 */

class OptCodeGen extends SimpleCodeGen {
    double stack[]  = new double[CompiledFunction.MAX_STACK_SIZE];        
    int sp = -1;

    double traceConsts[] = new double[1];
    Function traceFuncs[] = new Function[1];
    byte traceCode[] = new byte[1];
    CompiledFunction tracer = new CompiledFunction(0, traceCode, traceConsts, traceFuncs);

    int intrinsicArity;
    
    OptCodeGen(SyntaxException e) {
        super(e);
    }

    //@Override
    void start() {
        super.start();
        sp = -1;
        intrinsicArity = 0;
    }

    //@Override
    void push(Token token) throws SyntaxException {
        byte op;
        switch (token.id) {
        case Lexer.NUMBER:
            op = VM.CONST;
            traceConsts[0] = token.value;
            break;
            
        case Lexer.CONST:
        case Lexer.CALL:
            Symbol symbol = symbols.lookup(token.name, token.arity);
            if (symbol == null) {
                throw exception.set("undefined '" + token.name + "' with arity " + token.arity, token.position); 
            }
            if (symbol.op > 0) { // built-in
                op = symbol.op;
                if (op >= VM.LOAD0 && op <= VM.LOAD4) {
                    int arg = op - VM.LOAD0;
                    if (arg + 1 > intrinsicArity) {
                        intrinsicArity = arg + 1;
                    }
                }
            } else if (symbol.fun != null) { // function call
                op = VM.CALL;
                traceFuncs[0] = symbol.fun;
            } else { // variable reference
                op = VM.CONST;
                traceConsts[0] = symbol.value;
            }
            break;
                        
        default:
            op = token.vmop;
            if (op <= 0) {
                throw new Error("wrong vmop: " + op);
            }
        }
        int oldSP = sp;
        traceCode[0] = op;
        if (op != VM.RND) {
            sp = tracer.execWithoutCheck(stack, sp);
        } else {
            stack[++sp] = Double.NaN;
        }

        //constant folding
        if (!Double.isNaN(stack[sp]) || op == VM.CONST) {
            code.pop(oldSP + 1 - sp);
            consts.pop(oldSP + 1 - sp);
            consts.push(stack[sp]);
            op = VM.CONST;
        } else if (op == VM.CALL) {
            funcs.push(traceFuncs[0]);
        }
        code.push(op);
    }

    CompiledFunction getFun(int arity) {
        return new CompiledFunction(arity, code.toArray(), consts.toArray(), funcs.toArray());
    }
}
