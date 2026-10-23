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

/* Non-optimizing Code Generator
   Reads tokens in RPN (Reverse Polish Notation) order,
   and generates VM opcodes,
   without any optimization.
 */

class SimpleCodeGen extends TokenConsumer {
    private CompiledFunction compiledFunction = new CompiledFunction();
    static final SyntaxException HAS_ARGUMENTS = new SyntaxException();

    ByteStack code      = new ByteStack();
    DoubleStack consts  = new DoubleStack();
    FunctionStack funcs = new FunctionStack();

    //String argNames[];
    Symbols symbols;

    SyntaxException exception;

    SimpleCodeGen(SyntaxException exception) {
        this.exception = exception;
    }

    SimpleCodeGen setSymbols(Symbols symbols) {
        this.symbols = symbols;
        return this;
    }

    //@Override
    void start() {
        code.clear();
        consts.clear();
        funcs.clear();
    }
    
    void push(Token token) throws SyntaxException {
        byte op;
        switch (token.id) {
        case Lexer.NUMBER:
            op = VM.CONST;
            consts.push(token.value);
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
                    throw HAS_ARGUMENTS.set("eval() on implicit function", exception.position);
                }
            } else if (symbol.fun != null) { // function call
                op = VM.CALL;
                funcs.push(symbol.fun);
            } else { // variable reference
                op = VM.CONST;
                consts.push(symbol.value);
            }
            break;
                        
        default:
            op = token.vmop;
            if (op <= 0) {
                throw new Error("wrong vmop: " + op);
            }
        }
        code.push(op);
    }
    
    double getValue() {
        compiledFunction.init(0, code.toArray(), consts.toArray(), funcs.toArray());
        try {
            return compiledFunction.eval();
        } catch (ArityException e) {
            throw new Error("" + e); //never
        }
    }
}
