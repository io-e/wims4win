#! /usr/bin/python3

"""
This utility is part of chemeq's source package. It is used to
extract data from NIST atomic weight tables.

Chemeq is a basic standalone filter written in C language,
flex and bison. It inputs strings like:

`2H2 + O2 ---> 2 H2O`

and can outputs LaTeX code and messages about the equilibrium of a
chemical reaction.
----------------------------------------

Copyright (c) 2023 Georges Khaznadar
License: GPL V3+

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""

import sys, re

class data:
    """
    Data read from NIST atomic weight tables
    """

    def __init__(self, A, Z, symbol, isotope_weight, weight):
        self.A = A
        self.Z = Z
        self.symbol = symbol
        self.isotope_weight = isotope_weight
        self.weight = weight
        return

    def __str__(self):
        if self.weight is not None:
            result = "{symbol}: Z = {Z}, A = {A}, molar mass = {weight}".format(**self.__dict__)
        else:
            result = "{symbol}: Z = {Z}, A = {A}, isotope mass = {isotope_weight}".format(**self.__dict__)
        return result + " mean molar mass " + self.mean_molar_mass

    @property
    def mean_molar_mass(self):
        """
        Computes a "best mean" molar mass based on data found
        in NIST atomic weight tables
        @return a string, which can be converted to a float
        """
        if self.weight:
            # ex: 1 H 1 1.00782503223(9) 0.999885(70) [1.00784,1.00811] m
            m = re.match(r"\[(\S+),(\S+)\]", self.weight)
            if m:
                # the molar mass is the mean value of two limits
                return str(0.5 * (float(m.group(1))+ float(m.group(2))))
            # ex: 2   He  3    3.0160293201(25)   0.00000134(3)  4.002602(2)
            m = re.match(r"(\S+)\(.*\)", self.weight)
            if m:
                # get rid of the additional numbers in parenthesis
                return m.group(1)
            # ex: 89  Ac  227  227.0277523(25)                  [227]
            m = re.match(r"\[(\S+)\]", self.weight)
            if m:
                # get rid of the square brackets
                return m.group(1)
        elif self.isotope_weight:
            # ex: 99  Es  252  252.082980(54)
            m = re.match(r"([.0-9]+)\(.*\)", self.isotope_weight)
            if m:
                # get rid of everything which is not a pure number at the end
                return m.group(1)
        return "ERROR: format not recognized"
        
pattern = re.compile(r"^(?P<Z>\d+)\s+(?P<symbol>[A-Za-z]+)\s+(?P<A>\d+)\s+(?P<isotope_weight>\S+)(\s+(\S+))?(\s+(?P<weight>\S+))?.*")

def parse(infile):
    """
    Parse data from NIST  atomic weight table, available at
    https://physics.nist.gov/cgi-bin/Compositions/stand_alone.pl?ele=&all=all&ascii=ascii&isotype=some
    @param infile an open text file
    @return a dictionary Z value => `data` instance
    """
    result = {}
    # the molar weight is always written after column 46, for
    # lines which match `pattern`.
    WEIGHT_COLUMN = 46
    for l in infile.readlines():
        l = l.rstrip() # remove spaces on the right
        m = pattern.match(l)
        if m :
            if len(l) > WEIGHT_COLUMN:
                weight = re.match(r"\s*(\S+)", l[WEIGHT_COLUMN:]).group(1)
            else:
                weight = None
            d = data(
                m.group("A"), m.group("Z"), m.group("symbol"),
                m.group("isotope_weight"), weight)
            result[int(m.group("Z"))] = d
    return result

def rewrite_mendeleiev(nist_data):
    """
    Create a C++ source from file mendeleiv.cc.in, by replacing previous
    molar mass information with data provided by nist_data
    @param nist_data a dictionary Z value => `data` instance
    @return a string featuring a valid C++ source
    """
    result = ""
    struct_pattern = re.compile(r"\{\{(?P<name>N_[^,]+),\s*\"(?P<symbol>[A-Za-z]+)\",\s*\"(?P<Z>\d+)\",(?P<remainder>[^\}]*)\}\},")
    with open("mendeleiev.cc.in") as infile:
        for l in infile.readlines():
            m = struct_pattern.match(l.strip())
            if m:
                print(m.group("name"), m.group("symbol"), m.group("Z"), m.group("remainder"))
    return result

if __name__ == "__main__":
    if len(sys.argv) > 1:
        fname = sys.argv[1]
    else:
        fname = "NIST-data.txt"
    with open(fname) as infile:
        nist_data = parse(infile)
        for Z in sorted(nist_data.keys()):
            print(nist_data[Z])
        rewrite_mendeleiev(nist_data)
