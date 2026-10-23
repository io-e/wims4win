#!/usr/local/bin/perl
# -*- coding: utf-8 -*-

open ('TEX', '>/tmp/chemtest.tex');
print TEX "\\documentclass{article}\n";
#print TEX "\\usepackage[frenchb]{babel}\n";
print TEX "\\usepackage{a4wide}\n";
print TEX "\\usepackage[utf8]{inputenc}\n";
#print TEX "\\usepackage {lmodern}\n";

print TEX "\\title{Chemeq, a parser for chemical equations}\n";
print TEX "\\author{\\copyright{} GPL 2000-2013, Georges Khaznadar 
\\emph{georgesk\@debian.org}}\n";
print TEX "\\begin{document}\n\\maketitle\n\n\\sloppy\n";
print TEX "\\abstract{Chemtest is a parser which analyses its standard input
and outputs useful strings accounting for informations related to chemical
equations. It can be embedded in educational applications. 
Here are listed examples of possible inputs for
chemeq. Some outputs are usefully interpreted by the LaTeX language 
in math mode. The other outputs are typeset with \\texttt{teletype fonts}.}\n\n";


$lignes_de_reponse = 6;

print TEX "Currently, when \\texttt{chemeq} is fed with a chemical equation or a
combination of chemical equations on its standard input, it gives ",$lignes_de_reponse," lines on its standard output.\n";
print TEX "\~\\\\\[3ex]\n\\hrule\n\\tableofcontents\n\~\\\\\[3ex]\n\\hrule\n";

while(chomp($essai = <STDIN>)){
    if(length($essai) != 0 && substr($essai,0,1) ne "#"){
	for ($i=0; $i < $lignes_de_reponse; $i++){
	    chomp($tmp=<STDIN>); @reponse[$i]=$tmp;
	}
	$essai_ = $essai;
	$essai_ =~ s/\'/\\\'/g;
	@args = ("echo \"$essai_\" | chemeq > /tmp/tmp.1");
	system @args;
	open (TMP,'/tmp/tmp.1');
	$essai =~ s/\^/\\\^{}/g;
	$essai =~ s/\>/\$>\$/g;
	$essai =~ s/\_/\\\_{}/g;
	$ok=1;
	for ($i=0; $i < $lignes_de_reponse; $i++){
	    chomp($tmp=<TMP>); @result[$i]=$tmp;
	    if (@result[$i] ne @reponse[$i]){
		@idem[i]="KO";
		$ok=0;
	    }
	    else {
		@idem[i]="OK";
	    }
	}
	if ($ok==1){
	    print TEX "\\section{OK : \\texttt{$essai}}\n";
	}
	else {
	    print TEX "\\section{ERROR : \\texttt{$essai}}\n";
	}
	for ($i=0; $i < $lignes_de_reponse; $i++){
	    print TEX @idem[i],"  ";
	}
	print TEX "\\\\\n";
	print TEX "Expected output :\n";
	print TEX "\\begin{enumerate} \n";
	for ($i=0; $i < $lignes_de_reponse; $i++){
	    print TEX "\\item "; 
	    if (@reponse[$i] =~ /\\/){ 
		print TEX "\$\$",@reponse[$i],"\$\$\n";
	    } 
	    else {
		$buffer = @reponse[$i];
		$buffer =~ s/\^/\\\^{}/g;
		$buffer =~ s/\>/\$>\$/g;
		$buffer =~ s/\_/\\\_{}/g;
		$buffer =~ s/\|/\$|\$/g;
		print TEX "\\texttt{", $buffer,"}\n";
	    }
	}
	print TEX "\\end{enumerate}\n";
    	if (!$ok){
	    print TEX "found output :\n";
	    print TEX "\\begin{enumerate} \n";
	    for ($i=0; $i < $lignes_de_reponse; $i++){
		print TEX "\\item \\verb|", @reponse[$i], "|\n";
	    }
	    print TEX "\\end{enumerate}\n";
	}
    }
}

print TEX "\\end{document}\n";

close TEX;

system ("(cd /tmp; echo q | latex chemtest.tex)");
system ("(cd /tmp; echo q | latex chemtest.tex)");
system ("(cd /tmp; xdvi chemtest.dvi)");
system ("(cd /tmp;rm -f chemtest* tmp.1)");
