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

/**
   Compiles a textual arithmetic expression to a {@link Function}.<p>
*/
class Compiler {
    private final SyntaxException exception = new SyntaxException();
    private final Lexer lexer = new Lexer(exception);
    private final RPN rpn     = new RPN(exception);
    private final DeclarationParser declParser = new DeclarationParser(exception);
    private final OptCodeGen codeGen = new OptCodeGen(exception);
    private final SimpleCodeGen simpleCodeGen = new SimpleCodeGen(exception);
    private final Declaration decl   = new Declaration();

    double eval(Symbols symbols, String expression) throws SyntaxException {
        rpn.setConsumer(simpleCodeGen.setSymbols(symbols));
        lexer.scan(expression, rpn);
        return simpleCodeGen.getValue();
    }

    FunctionAndName compile(Symbols symbols, String source) throws SyntaxException {
        Function fun = null;
        decl.parse(source, lexer, declParser);
        if (decl.arity == DeclarationParser.UNKNOWN_ARITY) {
            try {
                double value = eval(symbols, decl.expression);
                fun = new Constant(value);
            } catch (SyntaxException e) {
                if (e != SimpleCodeGen.HAS_ARGUMENTS) {
                    throw e;
                }
                // fall-through (see below)
            }
        }
                
        if (fun == null) { // either decl.arity was set, or an ArityException happened above
            symbols.pushFrame();
            symbols.addArguments(decl.args);
            try {
                rpn.setConsumer(codeGen.setSymbols(symbols));
                lexer.scan(decl.expression, rpn);
            } finally {
                symbols.popFrame();
            }
            int arity = decl.arity;
            if (arity == DeclarationParser.UNKNOWN_ARITY) {
                arity = codeGen.intrinsicArity;
            }
            fun = codeGen.getFun(arity);

            /*
            if (decl.name != null && addDefinition) {
                symbols.addDefinition(decl.name, fun, decl.arity==DeclarationParser.UNKNOWN_ARITY);
            }
            */
        }

        return new FunctionAndName(fun, decl.name);
    }
}
