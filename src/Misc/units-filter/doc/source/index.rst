.. units-filter documentation master file, created by
   sphinx-quickstart on Tue Apr 17 19:33:22 2018.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.


Units-filter
============

Synopsis
--------

:code:`units-filter [-s]  [-o]  [-l]`

Description
-----------

:program:`units-filter` is a basic standalone parser written in C language, flex
and bison. It inputs strings like "1.5e3 nN.m.s^-1" (it could be the
time growth ratio of a torque) and outputs the value in standard SI
unit, followed by the physical dimension of this value.

Options
-------

.. program:: units-filter

.. option:: -s

           Like **S**\ignificant.

           Takes in account the number of significant digits. For example 1.0
           m contains 2 significant digits, while 0.00100 contains 3
           significant digits. It is possible to enforce the number of
           significant digits by using a special syntax : if units-filter
           parses the input "1.0m#6", it interprets it as a value with exactly
           6 significant digits, like "1.00000 m". The number following the #
           sign is the forced number of significant digits. The number of
           significant digits appears just before the last zero in the output
           of the command (this zero is a placeholder for future extensions).

.. option:: -o

           Like **O**\utput.

           Outputs a correct representation of the physical quantity with its
           physical unit in the International System notation. There may be
           some simplification with usual units. For example, a newton will be
           represented by the unit N in place of m.kg.s^-2. The value is
           expressed as a floating number with one digit before the decimal
           point, and as many digits in the mantissa as necessary to fit the
           desired number of significant digits (see an example below). It is
           possible to enforce the output unit : just add a colon and the
           desired unit at the end of the input. If this unit is homogeneous
           with the former one, it will be used to format the output.


.. option:: -l

           Like **L**\aTeX.

           Outputs a correct representation of the physical quantity with its
           physical unit in the International System notation, in LaTeX
           language.

SUPPORTED SYNTAX
----------------

The simplest syntax for a physical quantity to be passed by :code:`units-filter`
is: <float number> <physical unit>, for instance: "1e-3 m" stands for one
millimeter.

For compound units, the product of two base units is built with a dot "."
and each unit can be put at an integer power, with the symbol "^".
For instance, the velocity unit meter per second is "m.s^-1", and the
physical quantity "3e8 m.s^-1" stands for the velocity of light.

One can use the star character "*" instead of the dot "." to mean a
multiplication.

Instead of building compound units with dots and negative powers, one can
also use "/" as a separator. For instance, "1 m/s" and "1 m.s^-1",
"1 m/s^2" and "1 m.s^-2" are synonyms.

This simple syntax can be enriched by **modifiers**.

The modifier "#"
~~~~~~~~~~~~~~~~

This modifier allows one to specify the number of significant digits
which apply to a physical quantity. For instance, "1e-3m #2" stands
for 1.0 mm, i.e. one millimeter with two significant digits. The same can
be achieved with the input "1.0 mm" or "0.0010 m". This modifier is taken
in account only if one calls :code:`units-filter -s` to activate the
"Significant" option. It is also honoured when an output in ordinary text
format or in LaTeX source format is requested (when one runs
:code:`units-filter -o` or :code:`units-filter -l`.

The output of :code:`units-filter -s` provides the effective number of
significant digits, as the ninth field, hence permitting to make some scripting
around. All strings "1e-3m #2", "1.0 mm" or "0.0010 m" will yield the
same output when parsed by :code:`units-filter -s`.

Please notice that this modifier allows one to artificially reduce the
number of significant digits, hence forcing some rounding. For instance,
the string "299792458m.s^-1#1", when processed by :code:`units-filter -o`
will yied :code:`3e8 m.s^-1`, which is a less precise value of the speed
of light.

Much effort has been put in the rounding mechanism, in order to overcome
limitations due to float numbers in base two. The calculations are internally
carried as decimal numbers, so the general rule to round <integer>+0.5 to the
integer immediately above is honoured. However, beware: if the value to be
rounded *comes from an external program* which uses calculation with
ordinary float numbers, a value 3/2 processed by an external program
and later processed by :code:`units-filter`, with one single significative
number, will not always be rounded to 2 as expected.

The modifier "~"
~~~~~~~~~~~~~~~~

This modifier allows one to express a given precision, as a percentage.
For example, "1kV~2" stands for one kilovolt plus or minus two percent.
This option is honoured wen one runs :code:`units-filter -o` or
:code:`units-filter -l`, that is, when one requests an output in ordinary
text format or in LaTeX source format.

The modifier ":"
~~~~~~~~~~~~~~~~

This modifier allows one to specify a non-standard unit for the text or
LaTeX output. For instance, "1800C:A.h" will be rendered as half of an
ampere.hour; This modifier is honoured when one runs :code:`units-filter -o` or
:code:`units-filter -l`, that is, when one requests an output in ordinary
text format or in LaTeX source format.

OUTPUT
------

When :code:`units-filter` is run with no option switch, it parses its
standard input, and if the syntax is recognized, it prints one float number
and nine integers. The meaning of this output is:

float value
~~~~~~~~~~~
The physical quantity's value, given in the basic International System unit.

integers #1 to #7
~~~~~~~~~~~~~~~~~
Those integers express the powers of the base units of International System used
for the physical quantity's unit.

integer #8
~~~~~~~~~~
This integer is non-zero only when one calls :code:`units-filter` with the
:code:`-s` option, to take in account significative numbers of the value,
either given implicitly or given explicitly after a modifier (see
`The modifier "#"`_ above).

integer #9
~~~~~~~~~~
Not used currently. May be used by future versions of :code:`units-filter`.

When :code:`units-filter` is run with the switch :code:`-s`, the output
is the same as when :code:`units-filter` is run with no option, except
for the eighth integer, which is always positive and means the number of
significant digits (hence the precision) of the value.

When :code:`units-filter` is run with the switch :code:`-o`, the output is
a physical quantity, written in a canonical format. One can notice that
:code:`units-filter -o` is *reentrant*, i.e. when called twice in a pipe,
the output is the same than when it is called only once.

When :code:`units-filter` is run with the switch :code:`-l`, the output is
a physical quantity, written as LaTeX source code. In order to get
something useful, one must embed the output inside a well-shaped LaTeX
document and run a LaTeX compiler.

Compound units
~~~~~~~~~~~~~~
:code:`units-filter` can parse physical quantities like "1 h 1 min 1 s", which
yields the same as "3661 s". This syntax is honored if and only if the units
are compatible together, and given in order of decreasing importance.
For example "1m 1mm" makes sense, whereas "1 C 1A.h" would trigger an
error, and "1A.h 1C" would be accepted.


ERROR OUTPUT
------------
When the syntax of the input cannot be parsed properly by :code:`units-filter`,
it sends a message like :code:`ERROR at xx : syntax error`, where *xx* is a
column number, to the standard error stream. The number *xx* allows one to
know how many characters of the input could be parsed successfully before
the failure. Additionally, the return code of the program is non-zero.

LIST OF PHYSICAL UNITS
----------------------

Here is the list of physical units which are automatically recognized
by :code:`units-filter`:

  .. include:: ./all_units.rst
	       
EXAMPLES
--------
       Establish the SI value and unit exponent of a quantity in the mksa
       system:

       :code:`~$ echo 1.5e3 nN.m.s^-1 | units-filter`

       :code:`1.5e-6 2 1 -3 0 0 0 0`

       which means : 1.5e-6 (SI unit) m^2.kg.s^-3

       Compare different physical quantities:

       :code:`~$ e1=$(echo "1.2e-3 V" | units-filter)`
	     
       :code:`~$ e2=$(echo "1200e3 nWb/s"| units-filter`
	     
       :code:`~$ if [ "$e1" = "$e2" ]; then echo ok; else echo ko; fi`

       :code:`ok`

       ... which emphasizes that webers by time unit are the same as volts.

       Playing with the number of significant digits:

       :code:`~$ echo "0.00100m" | src/units-filter -s`

       :code:`0.001 1 0 0 0 0 0 0 3 0`

       :code:`~$ echo "0.00100m #2" | src/units-filter -s`

       :code:`0.001 1 0 0 0 0 0 0 2 0`

       Giving a value for the relative precision:

       :code:`~$ echo "1kV~2" | units-filter -o`

       :code:`1e+03 V +-2%`

       Turning on the LaTeX output:

       :code:`~$ echo "1kohm+-2%" | units-filter -l`

       :code:`1\times 10^{+03}\, \Omega \pm 2\,\%`

       Turning on the output of a canonical physical notation:

       :code:`~$ echo "1.0 m.kg.s^-2 #7" | units-filter -o`

       :code:`1.000000e+00N`

       Choosing a non-standard unit for the output:

       :code:`~$ echo 1800C:A.h| units-filter -o`

       :code:`5.000e-01 A.h`
	     because 3600 coulomb is one ampere.hour

       :code:`~$ echo 1 g:uma | units-filter -o`

       :code:`6e23 uma`
	     This last result reminds that Avogadro's constant is near 6e23
	     mol^-1.

KNOWN BUGS
----------

       Few units out of the mksa system are successfully parsed.

       Compound units yield inconsistent results, when parsed by
       :code:`units-filter -o`, because this process will take the
       first mentioned unit as the unit wanted for the result, and consider
       that the first value gives the number of wanted significant digits.
       
