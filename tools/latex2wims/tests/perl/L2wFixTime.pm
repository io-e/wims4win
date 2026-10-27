# Fige time() pour des sorties reproductibles (date « datm » de latex2wims.pl) — tests seulement.
package L2wFixTime;
BEGIN { *CORE::GLOBAL::time = sub () { 1790000000 }; }
1;
