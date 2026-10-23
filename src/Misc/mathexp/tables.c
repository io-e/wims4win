/*    Copyright (C) 2002-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

#include "mathexp.h"

char *typenames[]={
    "number", "variable",
      "parentheses","matrix","set","function",
      "exponentiation","term","cupcap","addition",
      "equation","inequality","not","and","or","implication",
      "comma","quantifier"
};

op oppunct[]={
    {"(",	exp_paren},
    {"[",	exp_matrix},
    {"{",	exp_set},
    {"^",	exp_exp},
    {"**", 	exp_exp},
    {"*", 	exp_muldiv},
    {"/",	exp_muldiv},
    {"-",	exp_add},
    {"+",	exp_add},
    {"!=",	exp_eq},
    {"<>",	exp_eq},
    {"<==>",	exp_imply},
    {"<==",	exp_imply},
    {"<=>",	exp_imply},
    {"=>",	exp_imply},
    {"==>",	exp_imply},
    {"<=",	exp_ineq},
    {">=",	exp_ineq},
    {"<",	exp_ineq},
    {">",	exp_ineq},
    {"==",	exp_eq},
    {"=",	exp_eq},
    {",",	exp_comma},
    {";",	exp_comma},
};
int oppunctno=(sizeof(oppunct)/sizeof(oppunct[0]));

op opalpha[]={
    {"cup",	exp_cupcap},
    {"cap",	exp_cupcap},
    {"and",	exp_and},
    {"or",	exp_or},
    {"not",	exp_not},
    {"in",	exp_quantifier},
    {"forall",	exp_quantifier},
    {"exist",	exp_quantifier},
    {"exists",	exp_quantifier},
};
int opalphano=(sizeof(opalpha)/sizeof(opalpha[0]));

ex exptype[]={
	{"addition",	exp_add, exp_add},
	{"algexp",	0, exp_add},
        {"and",		exp_and, exp_and},
        {"andor",	exp_and, exp_or},
	{"equation",	exp_eq, exp_eq},
	{"equations",	exp_eq, exp_eq},
	{"function",	exp_fn, exp_fn},
	{"functions",	exp_fn, exp_fn},
	{"inequalities",exp_ineq, exp_ineq},
	{"inequality",	exp_ineq, exp_ineq},
	{"inequation",	exp_eq, exp_ineq},
	{"inequations",	exp_eq, exp_ineq},
        {"logic",	exp_not, exp_or},
        {"matrix",	exp_matrix, exp_matrix},
        {"mult",	exp_muldiv, exp_muldiv},
        {"multiplication", exp_muldiv, exp_muldiv},
        {"or",		exp_or, exp_or},
        {"parentheses",	exp_paren, exp_paren},
        {"parenthesis",	exp_paren, exp_paren},
        {"predicate",	exp_eq, exp_quantifier},
        {"set",		exp_set, exp_set},
        {"term",	0, exp_muldiv},
        {"variable",	exp_variable, exp_variable},
        {"variables",	exp_variable, exp_variable},
};

int exptypeno=(sizeof(exptype)/sizeof(exptype[0]));

