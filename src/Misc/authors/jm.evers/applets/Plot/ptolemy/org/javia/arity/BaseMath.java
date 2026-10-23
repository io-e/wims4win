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
 *
 */

package org.javia.arity;

// This class is a replacement for BaseMath.java to be used on Java 1.5 and later
class BaseMath {
    public static final double atan(double x) {
        return Math.atan(x);
    }
    
    public static final double asin(double x) {
        return Math.asin(x);
    }

    public static final double acos(double x) {
        return Math.acos(x);
    }

    public static final double exp(double x) {
        return Math.exp(x);
    }

    public static final double log(double x) {
        return Math.log(x);
    }

    public static final double sinh(double x) {
        return Math.sinh(x);
    }

    public static final double cosh(double x) {
        return Math.cosh(x);
    }

    public static final double tanh(double x) {
        return Math.tanh(x);
    }

    public static final double cbrt(double x) {
        return Math.cbrt(x);
    }
         
    public static final double pow(double x, double y) {
        return Math.pow(x, y);
    }

    public static final double log10(double x) {
        return Math.log10(x);
    }
}
