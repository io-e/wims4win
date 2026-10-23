#!/usr/bin/perl

open(IN, "commands") ;

my $L='' ; my $t=0 ;
while (<IN>) {my $l=$_ ;
  if (/^:/) { $t=1 ; $l =~ s/^:(\w+)(\s.*)?\n/$1/g ; $L .=$l ; }
    else {
    if ($t==1) { $L .=$l ;} ; $t=0 ;
  }
} ;
$L =~ s/\s+/\n/g;
$L = "'" . join ( "', '",split ('\n', $L)) . "'";

my $Text = << "EOT"
  editAreaLoader.load_syntax["flydraw"] = {
    'QUOTEMARKS' : {1: ' " '}
	,'KEYWORD_CASE_SENSITIVE' : true
	,'KEYWORDS' : {
	'flydraw' : [
EOT
. $L
. << "EOT"
 ]
}
	,'OPERATORS' :[
		'*', '+', '-', '/', '^', '<', '=', '>','//'
	]
	,'DELIMITERS' :[
		'(', ')', '[', ']','{','}'
	]
	,'REGEXPS' :
	{
		'oefvariables' : { 'search': '()(\\\\\\\\\\\\w+)()',
			'class' : 'oefvariables',
			'modifiers' : 'g', 'execute' : 'before' }
		,'wimsvariables' : {
			'search' : '()(\\\\\$\\\\w+)()'
			,'class' : 'wimsvariables'
			,'modifiers' : 'g', 'execute' : 'before'
		}
	}
	,'STYLES' : {
		'COMMENTS': 'color: #0000CD;'
		,'QUOTESMARKS': 'color: #6381F8;'
		,'KEYWORDS' : {
			'flydraw' : 'color: #48BDDF;'
			}
		,'OPERATORS' : 'color: #FF00FF;'
		,'DELIMITERS' : 'color: #60CA00;'
		,'REGEXPS': {
			'oefvariables' : 'color: #FF3A6E;'
			,'wimsvariables' : 'color: #FF3A6E;'
		}
	}
};
EOT
;

out( 'flydraw.js', $Text) ;

sub out { my ($bloc, $text) = @_;
  open  (OUT, ">$bloc") ;
  print OUT $text ; close OUT;
}
