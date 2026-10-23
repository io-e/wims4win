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

/**
   Abstract base class for functions.<p>
   A function has an arity (the number of arguments), and a way for evaluation
   given the values of the arguments.<p>
   Derive from this class to create user-defined functions.

   <h3>Thread safety</h3>
   Assuming the user didn't subclass Function to define user functions,
   the compiled Functions are thread-safe 
   (the same Function instance can be evaluated from multiple threads without locking).<p>

   Whatsmore, parallel evaluations of the same instance from multiple threads are not
   serialized (good).<p>

   If the user subclasses Function, he is responsible for the thread-safety of 
   his user-defined Functions.
 */

abstract public class Function {
    private int cachedArity = -1;

    /**
       Gives the arity of this function. 
       @return the arity (the number of arguments). Arity >= 0.
    */
    public int arity() {
        if (cachedArity == -1) {
            cachedArity = 0;
            for (int i = 0; i < 5; ++i) {
                double args[] = new double[i];
                try {
                    eval(args);
                    cachedArity = i;
                    break;
                } catch (ArityException e) {
                    //ignore, continue
                }
            }
        }
        return cachedArity;
    }

    /**
       Evaluates an arity-0 function (a function with no arguments).
       @return the value of the function
    */
    public double eval() throws ArityException {
        throw new ArityException("Didn't expect 0 args");
    }

    public double eval(double x) throws ArityException {
        throw new ArityException("Didn't expect 1 args");
    }

    public double eval(double x, double y) throws ArityException {
        throw new ArityException("Didn't expect 2 args");
    }

    /**
       Evaluates the function given the argument values.
       @param args array containing the arguments.
       @return the value of the function
       @throws ArityException if args.length != arity.
    */
    public double eval(double args[]) throws ArityException {
        switch (args.length) {
        case 0:
            return eval();
        case 1:
            return eval(args[0]);
        case 2:
            return eval(args[0], args[1]);
        }
        throw new ArityException("unexpected " + args.length + " args");
    }
}
