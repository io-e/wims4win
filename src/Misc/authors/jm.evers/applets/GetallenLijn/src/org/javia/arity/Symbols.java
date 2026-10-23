/*
 * Copyright (C) 2008 Mihai Preda.
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

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
   The collection of names (<em>symbols</em>) used for parsing an expression 
   (the context in which the parsing takes place).<p>

   Each symbol maps to either a {@link Function} or a constant.<p>

   A symbol is identified by the pair (name, arity).
   So a constant and a function with the same name,
   or two function with the same name but with different arity 
   are distinct symbols.<p>

   Symbols functions as a stack of <em>frames</em>: 
   when you pop the topmost frame, all the symbols added in that frame dissapear 
   (i.e. all the modifications done between the push and the pop are lost).<p>

   Example:
   <pre>
   <code>
   Symbols symbols = new Symbols();
   symbols.eval("1+1"); //doesn't update symbols
   symbols.define(symbols.compile("f(x)=x^2")); //updates symbols
   </code>
   </pre>
   <p>

   <h3>Thread safety</h3>
   The Symbols class is thread-safe
   (the same Symbols instance can be used by multiple threads without locking).<p>

   The compile() methods are synchronized, so parallel compile() calls on the same Symbols 
   instance are serialized.
 */

public class Symbols {
    /**
       Constructs a Symbols containing the built-in symbols (such as sin, log).
    */
    public Symbols() {
        for (int i = 0; i < builtin.length; ++i) {
            Symbol s = builtin[i];
            symbols.put(s, s);
        }
        try {
            for (int i = 0; i < defines.length; ++i) {
                define(compile(defines[i]));
            }
        } catch (SyntaxException e) {
            throw new Error(""+e); //never
        }
    }

    /**
       @param source the expression
       @return true if the expression is a definition (i.e. contains a '=').<p>
       These are definitions: "a=1+1";  "f(k)=2^k"<p>
       These are not definitions: "1+1"; "x+1"       
     */
    public static boolean isDefinition(String source) {
        return source.indexOf('=') != -1;
    }

    /**
       Evaluates a simple expression (such as "1+1") and returns its value.
       @throws SyntaxException in these cases:
       <ul>
       <li> the expression is not well-formed
       <li> the expression is a definition (such as "a=1+1")
       <li> the expression is an implicit function (such as "x+1")
       </ul>
     */
    public synchronized double eval(String expression) throws SyntaxException {
        return getCompiler().eval(this, expression);
    }

    /**
       Compiles an expression in the context of this Symbols.
       Does not modify the symbols.
       <p>

       An expression is one of these cases (@see Symbols.isDefinition()):
       <ul>
       <li> constant value: 1+1
       <li> implicit function: x+1
       <li> constant definition: a=1+1
       <li> function definition with explicit arguments: f(a)=a+1
       <li> function definition with implicit arguments: f=x+1
       </ul>
       <p>

       @param source the expression; may contain '=' to denote a definition (with a name).
       
       @return the function together with its eventual name.<p>

       If this is not a definition (e.g. "1+1", "x^2"), the name is null.<p>
       
       If the expression is a constant (e.g. "1+1", "a=2"), 
       the returned Function is an instance of {@link Constant}.

       @throws SyntaxException if there are errors compiling the expression.
    */
    public synchronized FunctionAndName compile(String source) throws SyntaxException {
        return getCompiler().compile(this, source);
    }

    /**
       Adds a new function symbol to the top-most frame of this Symbols.
       @param name the name of the function (e.g. "sin")
       @param function the function to which the name maps
    */    
    synchronized public void define(String name, Function function) {
        if (function instanceof Constant) {            
            addConstant(name, ((Constant) function).eval());
        } else {
            add(new Symbol(name, function));
        }
    }

    /**
       Adds a new function symbol to the top-most frame of this Symbols.
       @param funAndName structure containing the function and its name
    */    
    synchronized public void define(FunctionAndName funAndName) {
        if (funAndName.name != null) {
            define(funAndName.name, funAndName.function);
        }
    }
    

    /**
       Adds a new constant symbol to the top-most frame of this Symbols.
       @param name the name of the constant (e.g. "pi")
       @param value the value of the constant
    */
    synchronized public void addConstant(String name, double value) {
        add(new Symbol(name, value));
    }

    /**
       Pushes a new top frame.<p>

       All modifications (defining new symbols) happen in the top-most frame.
       When the frame is pop-ed the modifications that happened in it are reverted.
    */
    synchronized public void pushFrame() {
        frames.push(delta);
        delta = null;
    }

    /**
       Pops the top frame.<p>

       All the modifications done since this frame was pushed are reverted.
       @throws EmptyStackException if there were fewer <code>pushFrame</code> than <code>popFrame</code>.
    */
    synchronized public void popFrame() {
        if (delta != null) {
            for (int i = delta.size() - 1; i >= 0; --i) {
                Symbol previous = (Symbol) delta.elementAt(i);
                if (previous.isEmpty()) {
                    symbols.remove(previous);
                } else {
                    symbols.put(previous, previous);
                }
            }
        }
        delta = (Vector) frames.pop();
    }



    //--- non-public below

    private final static Symbol builtin[];
    private static Symbol shell = new Symbol(null, 0);
    private static ThreadLocal compilers = new ThreadLocal();
    
    private static Compiler getCompiler() {
        Compiler compiler;
        if ((compiler = (Compiler) compilers.get()) == null) {
            compilers.set(compiler = new Compiler());
        }
        return compiler;
    }

    private Hashtable symbols = new Hashtable(); //Hashtable<Symbol, Symbol>
    private Vector delta = null;                 //Vector<Symbol>
    private Stack frames = new Stack();          //Stack<Vector>

    static {
        Vector vect = new Vector();
        int arity;
        for (byte i = 0; i < VM.BYTECODE_END; ++i) {
            if ((arity = VM.builtinArity[i]) >= 0) {
                vect.addElement(new Symbol(VM.opcodeName[i], arity, i));
            }
        }

        final String IMPLICIT_ARGS[] = {"x", "y", "z"};
        for (byte i = 0; i < IMPLICIT_ARGS.length; ++i) {
            vect.addElement(new Symbol(IMPLICIT_ARGS[i], Symbol.CONST_ARITY, (byte)(VM.LOAD0 + i)));
        }

        vect.addElement(new Symbol("pi", Math.PI));
        vect.addElement(new Symbol("\u03c0", Math.PI));
        vect.addElement(new Symbol("e", Math.E));

        vect.addElement(new Symbol("Inf", Double.POSITIVE_INFINITY));
        vect.addElement(new Symbol("Infinity", Double.POSITIVE_INFINITY));
        vect.addElement(new Symbol("NaN", Double.NaN));

        int size = vect.size();
        builtin = new Symbol[size];
        vect.copyInto(builtin);
    }

    private static final String defines[] = {
        "ln(x)    = log(x)",
        "log2(x)  = log(x) * 1.4426950408889634074",  //*log2(e)
        "log10(x) = log(x) * 0.43429448190325182765", //*log10(e)
        "lg(x)    = log10(x)",
        "lb(x)    = log2(x)",
        "log(x, base) = log(x) / log(base)"
    };
    
    void addArguments(String args[]) {
        for (int i = 0; i < args.length; ++i) {
            add(new Symbol(args[i], Symbol.CONST_ARITY, (byte)(VM.LOAD0 + i)));
        }
    }

    void add(Symbol s) {
        Object previous = symbols.put(s, s);
        if (delta == null) {
            delta = new Vector();
        }
        delta.addElement(previous != null ? previous : Symbol.newEmpty(s));
    }

    synchronized Symbol lookup(String name, int arity) {
        return (Symbol) symbols.get(shell.setKey(name, arity));
    }
}
