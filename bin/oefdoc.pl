#!/usr/bin/perl

use locale;
use warnings;
use strict;
##lancer du répertoire wims : bin/oedfdoc.pl

### for "editor"
my %name ;
$name{'explanation'}{'fr'}="Explication" ;
$name{'example'}{'fr'}="Exemple" ;
$name{'special'}{'fr'}="Méthodes spéciales<br>(énoncé)";
$name{'syntax'}{'fr'}="Syntaxe";
$name{'slib'}{'fr'}='SLIB';
$name{'anstype'}{'fr'}='Types de réponses' ;

$name{'explanation'}{'en'}="Explanation" ;
$name{'example'}{'en'}="Example" ;
$name{'special'}{'en'}="Special method<br>(statement)";
$name{'syntax'}{'en'}="Syntax";
$name{'slib'}{'en'}='SLIB';
$name{'anstype'}{'en'}='Answer types' ;

$name{'explanation'}{'cn'}="Explanation" ;
$name{'example'}{'cn'}="Example" ;
$name{'special'}{'cn'}="Special method<br>(statement)";
$name{'syntax'}{'cn'}="Syntax";
$name{'slib'}{'cn'}='SLIB';
$name{'anstype'}{'cn'}='Answer types' ;

$name{'explanation'}{'nl'}="Explanation" ;
$name{'example'}{'nl'}="Example" ;
$name{'special'}{'nl'}="Special method<br>(statement)";
$name{'syntax'}{'nl'}="Syntax";
$name{'slib'}{'nl'}='SLIB';
$name{'anstype'}{'nl'}='Answer types' ;

$name{'explanation'}{'it'}="Spiegazione" ;
$name{'example'}{'it'}="Esempio" ;
$name{'special'}{'it'}="Metodi speciali<br>(enunciato)";
$name{'syntax'}{'it'}="Sintassi";
$name{'slib'}{'it'}='SLIB';
$name{'anstype'}{'it'}='Tipi di risposte' ;

$name{'explanation'}{'es'}="Explicación" ;
$name{'example'}{'es'}="Ejemplo" ;
$name{'special'}{'es'}="Métodos especiales<br>(enunciado)";
$name{'syntax'}{'es'}="Sintaxis";
$name{'slib'}{'es'}='SLIB';
$name{'anstype'}{'es'}='Tipos de Respuestas' ;

$name{'explanation'}{'ca'}="Explicació" ;
$name{'example'}{'ca'}="Exemple" ;
$name{'special'}{'ca'}="Mètodes especials<br>(enunciat)";
$name{'syntax'}{'ca'}="Sintaxi";
$name{'slib'}{'ca'}='SLIB';
$name{'anstype'}{'ca'}='Tipus de Respostes' ;
my $DOSSIER="public_html/scripts/js/editor/scripts_1/bd_js";
my $DOSSIER_edit_area="public_html/scripts/js/edit_area/reg_syntax";
my $DOSSIER_wims="public_html/modules/help/wimsdoc.en/cmd/";
my $slibdir="public_html/scripts/slib/";
my $helpdir="public_html/scripts/help";

my @Lang=('en','fr','cn','nl','it','ca', 'es') ;

system("mkdir -p $DOSSIER") ;
my @table=('if', 'oefparm0', 'oefparm1', 'oefparm2', 'oefparm3', 'oefparm4', 'oefparm5','oefcommand') ;

### for "editor"
my (%Begin, %End) ;
$Begin{'if'}= "  " ;
$End{'if'}= "  " ;
$Begin{'oefparm0'}= '\\\\' ;
$End{'oefparm0'}= "\{  =  \}" ;
$Begin{'oefcommand'}= '\\\\' ;
$End{'oefcommand'}= "\{  \}" ;
$Begin{'anstype'}='' ;
$End{'anstype'}='' ;
##$Begin{'anstype'}='\\\\answer\{  \}\{  \}\{type=' ;
##$End{'anstype'}='\}\{option=  \}\{ weight= \}\n' ;
for my $tag ("oefparm4") {
   $Begin{$tag}= "" ;
   $End{$tag}= "\(  \)" ;
}
for my $tag ("oefparm2", "oefparm3") {
   $Begin{$tag}= " " ;
   $End{$tag}= " " ;
}
for my $tag ( "oefparm1") {
   $Begin{$tag}= "" ;
   $End{$tag}= "" ;
}

for my $tag ("oefparm5") {
   $Begin{$tag}= '\\\\' ;
   $End{$tag}= "" ;
}

$Begin{'slib'}= "slib(" ;
$End{'slib'}= " )" ;

my @phtml=("expandlines", "imagefill", "help", "tabs2lines", "rename", "tooltip", "codeinput", "imageinput", "mathmlinput", "drawinput", "glossary") ;
for my $tag (@phtml) {
   $Begin{$tag}= "\\special\{" ;
   $End{$tag}= " \}" ;
}
### end for "editor"
### begin for "edit area"
my $EDIT_AERA_OEF_begin = "editAreaLoader.load_syntax['wimsoef'] = {
    'QUOTEMARKS' : {1: ' \" '}
	,'KEYWORD_CASE_SENSITIVE' : true
	,\'KEYWORDS\' : {" ;
my $EDIT_AERA_begin = "editAreaLoader.load_syntax['wims'] = {
    'QUOTEMARKS' : {1: ' \" '}
	,'KEYWORD_CASE_SENSITIVE' : true
	,\'KEYWORDS\' : {" ;
my $EDIT_AERA_DOC_begin = "editAreaLoader.load_syntax['wimsdoc'] = {
    'QUOTEMARKS' : {1: ' \" '}
	,'KEYWORD_CASE_SENSITIVE' : true
	,\'KEYWORDS\' : {" ;

my $EDIT_AERA_OEF_end = "}
	,'OPERATORS' : [
		'*', '+', '-', '/', '^', ':=', '<', '=', '>','..'
	]
	,'DELIMITERS' : [
		'(', ')', '[', ']','{','}','??','??'
	]
	,'REGEXPS' :
	{
		'oefvariables' : { 'search': '()(\\\\\\\\\\\\w+)()',
			'class' : 'oefvariables',
			'modifiers' : 'g', 'execute' : 'after' },
		'record' : { 'search': '(\\n)(:)()',
			'class' : 'record',
			'modifiers' : 'g', 'execute' : 'after' },
		'question' : { 'search': '(\\\\?\\\\?)([^\\\\?]+)(\\\\?\\\\?)',
			'class' : 'question',
			'modifiers' : 'g', 'execute' : 'before' }
	}
	,'STYLES' : {
		'COMMENTS': 'color: #0000CD;'
		,'QUOTESMARKS': 'color: #6381F8;'
		,'KEYWORDS' : {
			'oefcommand' : 'color: #FF9933;'
			,'oefparm0' : 'color: #FF9933;'
			,'oefparm1' : 'color: #985717;'
			,'oefparm2' : 'color: #336600;'
			,'oefparm3' : 'color: #336600;'
			,'oefparm4' : 'color: #3399FF;'
			,'oefparm5' : 'color: #330099;'
			,'iff' : 'color: #FF00FF;'
			,'slib' : 'color: #60CA33;'
			,'anstype' : 'color: #3399FF;'
			,'special' : 'color: #3399FF;'
			}
		,'OPERATORS' : 'color: #FF00FF;'
		,'DELIMITERS' : 'color: #60CA00;'
		,'REGEXPS': {
			'oefvariables' : 'color: #FF3A6E;'
			,'question' : 'color:#985717;'
		}
	}
};" ;
my $EDIT_AERA_DOC_end = "}
	,'OPERATORS' : [
		'*', '+', '-', '/', '^', ':=', '<', '=', '>','..'
	]
	,'DELIMITERS' : [
		'(', ')', '[', ']','{','}','??','??'
	]
	,'REGEXPS' :
	{
		'docvariables' : { 'search': '()(\\\\\\\\\\\\w+)()',
			'class' : 'docvariables',
			'modifiers' : 'g', 'execute' : 'after' },
		'record' : { 'search': '(\\n)(:)()',
			'class' : 'record',
			'modifiers' : 'g', 'execute' : 'after' },
		'question' : { 'search': '(\\\\?\\\\?)([^\\\\?]+)(\\\\?\\\\?)',
			'class' : 'question',
			'modifiers' : 'g', 'execute' : 'before' }
	}
	,'STYLES' : {
		'COMMENTS': 'color: #0000CD;'
		,'QUOTESMARKS': 'color: #6381F8;'
		,'KEYWORDS' : {
			'text' : 'color: #FF9933;'
			,'iff' : 'color: #FF00FF;'
			,'slib' : 'color: #60CA33;'
			}
		,'OPERATORS' : 'color: #FF00FF;'
		,'DELIMITERS' : 'color: #60CA00;'
		,'REGEXPS': {
			'docvariables' : 'color: #FF3A6E;'
			,'question' : 'color:#985717;'
		}
	}
};" ;
my $EDIT_AERA_end = "}
	,'OPERATORS' :[
		'*', '+', '-', '/', '^', ':=', '<', '=', '>','//','\$'
	]
	,'DELIMITERS' :[
		'(', ')', '[', ']','{','}','\\(', '\\)','??','??'
	],
	'REGEXPS' :
	{
		'wimsvariables' : { 'search': '(\\\\\$)([a-zA-Z0-9_]*)()',
			'class' : 'wimsvariables',
			'modifiers' : 'g', 'execute' : 'before' },
		'record' : { 'search': '(\\n)(:)()',
			'class' : 'record',
			'modifiers' : 'g', 'execute' : 'after' },
	}
	,'STYLES' : {
		'COMMENTS': 'color: #AAAAAA;'
		,'QUOTESMARKS': 'color: #6381F8;'
		,'KEYWORDS' : {
			'wimscommand' : 'color: #3399FF;'
			,'compare' : 'color: #FF00FF;'
			,'wimslogiciel' : 'color: yellow;'
			}
		,'OPERATORS' : 'color: green;'
		,'DELIMITERS' : 'color: #60CA00;',
		'REGEXPS': {
			'wimsvariables' : 'color: #FF3A6E;'
			,'tex' : 'color:red;'
		}
	}
};" ;
### end for "editarea"
##### generation

my $debut_oef=1;
my $debut_wims=1;
my $EDIT_AERA_OEF=$EDIT_AERA_OEF_begin ;
my $EDIT_AERA=$EDIT_AERA_begin ;
my $EDIT_AERA_DOC=$EDIT_AERA_DOC_begin ;

for my $lang (@Lang) {
   print "oefdoc.pl $lang\n" ;  system("mkdir -p $DOSSIER/$lang") ;
   slib($lang) ;
   for my $t (@table) { tableau($t,$lang) ; }
   phtml("$helpdir/$lang/special",$lang,"special",@phtml) ;
   $EDIT_AERA_OEF .= "\n, \'special\' : \n[ \'\\embed\', \'\\special{" . join ("\', '\\special{", @phtml) . "\']" if ($lang =~ /en/) ;
   anstype($lang) ;

};
$EDIT_AERA_OEF .=$EDIT_AERA_OEF_end ;
$EDIT_AERA_DOC .=$EDIT_AERA_OEF_end ;
if ($debut_wims==1) {$EDIT_AERA .= "\n" ; $debut_wims=0 } else {$EDIT_AERA .= "\n," };
$EDIT_AERA .= " \'wimscommand' : \n[ \' " . join ("\', \'", wimscommand($DOSSIER_wims)) . "']";

$EDIT_AERA .=$EDIT_AERA_end ;
out( "$DOSSIER_edit_area/wimsoef.js",  $EDIT_AERA_OEF) ;
out( "$DOSSIER_edit_area/wims.js",  $EDIT_AERA) ;
out( "$DOSSIER_edit_area/wimsdoc.js",  $EDIT_AERA_DOC) ;

sub anstype { my ($lang)=@_ ;
  my %HASH ;
  my %HELP ;
  open (IN, "$helpdir/$lang/reply.phtml") ;
  while (<IN>) {my $line = $_; my @t=();
    next if ($line =~ /^\!set/) ;
    next if !($line) ;
    if ($line =~ /\\\n/) {
      my @L= split( ',', $line) ;
      my $t1 = $L[0] ; $t1=~ s/\|/,/ ;
      my @ta=split(',',$t1) ;
      if ($ta[1]) {@t=split(' ',$ta[1]) ; } else {@t=($L[0])};
      for my $tag (@t) { $HASH{$tag} = cleanup3($L[1] . ' ' . (($L[2]) ? $L[2]: '')); $HELP{$tag}=$ta[0] ;}
    }
  }
  close IN ;
  my $text ='';
  my $Text = "var anstypename='$name{'anstype'}{$lang}';\n" ;
  my $var=join ("$End{'anstype'}\',\'$Begin{'anstype'}", sort keys(%HASH)) ;
  $Text .="var anstype = [ '$Begin{'anstype'}$var$End{'anstype'}' ];\n" ;
  for my $tag (keys(%HASH)){
    $text .= begin_js("$Begin{'anstype'}$tag$End{'anstype'}")
          . title_js($tag,'title')
          . middle_js($HASH{$tag},'out', $lang)
          . end_js("<a target=\"wims_help\" href=\"wims.cgi?lang=$lang&module=adm%2Fcreatexo&modtoolhelp=yes&+special_parm=reply,$HELP{$tag}\">++>></a>") ;
  }
  $Text .= function_js($text,'anstype') ;
  $EDIT_AERA_OEF .= "\n, \'anstype\' : \n[ \' type=" . join ("\', 'type=", sort keys(%HASH)) . "\']"  if $lang=~/en/ ;
  out ("$DOSSIER/$lang/anstype" . "_bd\.js", $Text) ;
}

sub slib {my ($lang)= @_ ;
  my $Text = "var slibname='$name{'slib'}{$lang}';\n" ;
  my @list_keyword=();
  my %HASH ;
  my $slibdirhelp="$helpdir/$lang/slib";
  for my $file (glob("$slibdir/*/*")) {
  #for example file=$slibdir/function/integrate
  $file =~s/$slibdir\///;
  my $text=''; my $tag='';
  my $filehelp="$slibdirhelp/$file" ;
  if (!open(IN, "$filehelp")) { $filehelp="$slibdirhelp/en/$file"; }
  if (!open(IN, "$filehelp")) { $filehelp="$slibdir/$file"; }
  for my $fff ("$slibdir/$file",$filehelp) {
    open (IN, ("$fff"));
    while (<IN>) {my $line=$_;
      if  ($line=~/^ *!exit/) { last ;}
      if ($line=~ s/^ *slib_(\w+) *=//){
        $tag=$1;
        $line=cleanup($line) ;
        if ($tag=~/parms/) {
          $line=~s/\\//;
          $HASH{nbparm}{$file}=$line;
        } else
        {
          $HASH{$tag}{$file}=$line ;
        }
      } else {
        next if !($tag) ;
        if ($tag=~/parms/) {
          chomp $line ;
          next if ($line) ;
          my @parm=split(",", $line) ;
          if (($parm[1]) && ($parm[1] =~ s/\\//)) {
            $HASH{$tag}{$file}.= cleanup3($parm[1]) . ",";
          }
          else {
            $HASH{$tag}{$file}= (($HASH{$tag}{$file})? $HASH{$tag}{$file}:' ') . (($parm[1]) ? cleanup3($parm[1]):'') ;
          }
          # $HASH{$tag}{$file} .= "\[$parm[0]\]" if ($parm[0]);
        }
        else {
          $HASH{$tag}{$file} .= cleanup($line);
          $tag='' if !($line=~/\\$/) ;
        }
      }
    }
  close IN;
  }
  push @list_keyword, $file if ($HASH{'title'}{$file});
  $tag='';
}
 my $var=join ("$End{'slib'}\',\'$Begin{'slib'}", @list_keyword) ;
 $Text .="var slib = [ '$Begin{'slib'}$var$End{'slib'}' ];\n" ;
 my $text='' ;
 for my $file (@list_keyword) {
  next if !($HASH{'title'}{$file}) ;
  my @examples=split("\\\\\\\\",$HASH{'example'}{$file}) if ($HASH{'example'}{$file});
  my $example='';
  for my $ex (@examples) {
    next if !($ex) ;
    $example .="<div class=\"title\">$name{'example'}{$lang}</div><code><font color=\"red\">slib($file</font> $ex <font color=\"red\">)</font></code>" ;
  }

  $text.=begin_js("$Begin{'slib'}$file$End{'slib'}")
           . title_js($HASH{'title'}{$file},'title')
           . syntax_js("<font color=\"red\">slib($file</font> " . ($HASH{'parms'}{$file}  ? $HASH{'parms'}{$file} : '') . " <font color=\"red\">)</font>",$lang)
           . middle_js($HASH{'out'}{$file},'out',$lang)
           . middle1_js (cleanup($example),'',$lang)
           . end_js("<a target=\"wims_help\" href=\"wims.cgi?lang=$lang&module=adm%2Fcreatexo&modtoolhelp=yes&special_parm=slib&+slibdet=$file#slib\">++>></a>") ;
 $text =cleanup2($text) ;
 }
 $Text .= function_js($text,'slib') ;
  out ("$DOSSIER/$lang/slib" . "_bd\.js",$Text) ;
  if ($lang=~/en/) {
    if ($debut_oef==1) { $EDIT_AERA_OEF .= "\n" ; $debut_oef=0 } else {$EDIT_AERA_OEF .= "\n," };
  $EDIT_AERA_OEF .= "\'slib\' : \n[ \'" . join ("\', '", @list_keyword) . "\']"   ;
  }
}
##special methode
sub phtml {my ($dir,$lang,$f,@file)=@_ ;
 my $cities=$f;
 my %HASH = (
  example       => {},
  signification => {},
  syntaxe => {},
  ) ;
 my $text='';
 my $tag='';
 my $Text="var specialname= '$name{'special'}{$lang}';\n";
 for my $meth (@file) {
  #print $meth . "\n";
    open (IN, "$dir/$meth.phtml");
    while (<IN>) {my $line=$_;
    next if !($line) ;
    if ($line =~ /^:/) {
    chomp $line;
       $tag=$line; $tag=~ s/://;
    }
    else
    {
     $HASH{$tag}{$meth} .= cleanup($line) . "\\n" ;
    }
  }
}
 my $var=join (" }\', \'\\\\special{", @phtml) ;
 ### cas particulier
 my $meth='embed';
 my $embd='\\\\embed{r  }' ;
   open (IN, "$helpdir/$lang/embedans.phtml");
    while (<IN>) {my $line=$_;
    next if !($line) ;
    if ($line =~ /^:/) {
    chomp $line;
       $tag=$line; $tag=~ s/://;
    }
    else
    {
     $HASH{$tag}{$meth} .= cleanup($line) . "\\n" ;
    }
  }
  close IN ;
  $Text .= "var special= [ '$embd', '\\\\special{$var }'];\n" ;
  $text = begin_js("$embd")
        . syntax_js("\\\\embed{r i, option }",$lang)
        . (($HASH{'signification'}{$meth}) ? middle_js($HASH{'signification'}{$meth},'title',$lang) : '')
        . (($HASH{'example'}{$meth}) ? middle1_js ($HASH{'example'}{$meth},'title',$lang) : '')
        . end_js("") ;
  ## fin cas particulier embed
  for my $meth (@phtml) {
   $text .= begin_js("\\\\special{$meth }")
           . syntax_js("\\\\special{$meth }",$lang)
           . (($HASH{'signification'}{$meth}) ? middle_js($HASH{'signification'}{$meth},'title',$lang ) : '')
           . (($HASH{'example'}{$meth}) ? middle1_js ($HASH{'example'}{$meth},'title',$lang) : '')
           . end_js("") ;

  }
  $Text .= function_js($text,'special') ;
  out ("$DOSSIER/$lang/$f" . "_bd\.js",$Text) ;

}
##file : nom du fichier, n nombre de lignes dans chaque record
sub tableau { my ($file, $lang) = @_ ;
  ### mettre $file à la place ensuite
  my $file1="$helpdir/$lang/$file";
  my $cities=$file ;
  if ($file =~ /if/){ $cities .= 'f' } ;
  my @list_keyword;
  my @List_keyword;
  my $text='';
  ##bug s'il n'y a pas de documentation
  open (IN, $file1);
  my $cnt=0 ;my $nl;
  my $Text = '' ;
  while (<IN>) { my $line=$_;
    chomp $line ;
    next if !($line) ;
    next if ($line=~ /^(!!)/);
    if ($line =~ /!changeto/){ die "$file1 should be translated"};
    if ($line =~ s /^://) {
      $cnt ++ ;
      if ($cnt == 1 ) { $Text = "var $cities" . "name= '" . cleanup3($line) . "';\n ";  }
      next if $cnt < 3 ;
      $line =~ s/:// ;
      if($text) { $text .= end_js("") ;}
      if ($text) {$text .= begin_js($Begin{$file} . $line . $End{$file}) ;}
      else
      { $text = begin_js($Begin{$file} . $line . $End{$file}) ; };
      push @list_keyword, $line;
      $line =~ s/(\(|\)|\.|,|\[|\]| )//g ;
      push @List_keyword, $line;
      $nl=0;
    }
    else {
      next if $cnt<3 ; if ($line =~ /^(\d)/) { my $arg=$1 ; next }
      $nl ++ ;
      $line = cleanup($line); if ($line =~ /help=/) { $line = '' ;}
      if ($nl==1 && !($file=~/oefcommand/)){
        if ($line) { $text .= syntax_js($line,$lang) ; }
      }
      else {
        if ($line) { $text .= middle_js($line,1,$lang) ;  }
	    }
	  }
  }
  close IN ;
  $text .= end_js("") ;
  my $var=join ("$End{$file}\', \'$Begin{$file}", @list_keyword) ;
  $Text .="var $cities= [ \'$Begin{$file}$var$End{$file}\' ];\n"
    . function_js($text,$cities) ;
  out ("$DOSSIER/$lang/$cities" . "_bd\.js",$Text) ;
  if ($lang=~/en/){
    if ($debut_oef==1) { $EDIT_AERA_OEF .= "\n" ; $debut_oef=0 } else {$EDIT_AERA_OEF .= "\n," };
    $EDIT_AERA_OEF .= " \'$cities\' : \n[ \'" . join ("\', '", @List_keyword) . "\']" ;
    if ($cities=~ /(iff|oefparm4)/) {
      if ($debut_wims==1) {$EDIT_AERA .= "\n" ; $debut_wims=0 } else {$EDIT_AERA .= "\n," };
      push @List_keyword, ('or', 'and') ;
      $EDIT_AERA .= " \'compare\' : \n[ \'" . join ("\', '", @List_keyword) . "\']"  ;
    }
  }
}

sub wimscommand { my ($d) = @_;
  my @wimslist = () ;
  for my $file (glob("$d/*")) {
    $file =~ s,$d/,, ;
    $file =~ s,.phtml,, ;
    push @wimslist, $file ;
  }
  push @wimslist, ("endif","next","else") ;
  @wimslist ;
}
### subroutine for "editor"
sub begin_js {my ($t)= @_ ;
  "\n  case \'$t\' \:\n    chaine_aide="
}
sub syntax_js {my ($line,$lang)=@_ ;
  "\'<div class=\"syntax\"><div class=\"title\">$name{'syntax'}{$lang}</div><code>$line</code></div>\'+\n"
}
sub middle_js {my ($line,$tag,$lang)=@_ ;
  if(!$line) {$line='' ;}
  if ($tag) { $line="<div class=\"title\">$name{'explanation'}{$lang}</div>$line" }
  "\'<div class=\"explication\">$line</div>\'+\n";
}

sub middle1_js {my ($line,$tag,$lang)=@_ ;
  if ($tag) { $line="<div class=\"title\">$name{'example'}{$lang}</div>$line" }
  "\'<div class=\"exemple\">$line</div>\'+\n";
}

sub title_js {my ($line,$tag)=@_ ;
  "\'<div class=\"title\">$line</div>\'+\n";
}

sub end_js {my ($text)=@_ ;
  "\'$text\' ; return chaine_aide;
	   break;
    ////********************"
}
sub function_js {my ($text,$tag)=@_ ;
 "function $tag" . "fun(instruction){
  switch(instruction){$text\n  }\n}"
}

sub cleanup { my ($txt)=@_ ;
  return $txt if !($txt) ;
  $txt=~ s/\\\b/&#92;/g ;
  $txt=~ s/\\\(/&#92;(/g ;
  $txt=~ s/\\\)/&#92;)/g ;
  $txt=~ s,\\,\\\\,g ;
  $txt=~ s/'/\\'/g ;
  $txt=~ s/\n/ /g ;
  return $txt ;
}
sub cleanup2 {my ($txt)=@_ ;
  $txt=~ s/\\\\ / /g if ($txt) ;
  return $txt;
}
sub cleanup3 {my ($txt)=@_ ;
  $txt=~ s/'/\\'/g if ($txt);
  return $txt;
}


sub htmltex { my ($TEXT) = @_ ;
$TEXT =~ s/<tt>\\/\\ttb{/g;
$TEXT =~ s/_/\\_/g;
$TEXT =~ s/\^/\\^{}/g;
$TEXT =~ s/\&/\\\&/g;
$TEXT =~ s/<(tt)>/\\text$1\{/g;
$TEXT =~ s/<(em|i)>/\\textit\{/g;
$TEXT =~ s/<b>/\\textbf\{/g;
$TEXT =~ s'</(b|tt|em|i)>'}'g;
$TEXT =~ s/<br\s*\/?>/\\par /g;
$TEXT =~ s/<p>/\\par /g;
$TEXT =~ s/<\/p>/\\par /g;
$TEXT =~ s/<ul>/\\begin{itemize}\n /g;
$TEXT =~ s/<\/ul>/\\end{itemize}\n /g;
$TEXT =~ s/<ol>/\\begin{enumerate}\n /g;
$TEXT =~ s/<\/ol>/\\end{enumerate}\n /g;
$TEXT =~ s/<li>/\\item /g;
$TEXT =~ s/<\/li>/\n /g;
$TEXT =~ s/<\/center>/\\end{center}\n /g;
$TEXT =~ s/<center>/\\begin{center}\n /g;
$TEXT =~ s/<\/center>/\n /g;
$TEXT =~ s/<center>/\n /g;
$TEXT =~ s/<table[^>]*>/\\begin{table}\\begin{tabular}{*{1}{p{30mm}}|*{1}{p{50mm}|}|*{1}{p{50mm}|}}\\hline /g;
$TEXT =~ s/<\/table>/\\\\ \\hline \\end{tabular} \\end{table}/g;
$TEXT =~ s/\$table_tr/\\\\\\hline /g;
$TEXT =~ s/\$table_hdtr/\\\\ \\hline/g;
$TEXT =~ s/\$table_header/\\begin{table}\\begin{tabular}{*{1}{p{30mm}}|*{1}{p{50mm}|}|*{1}{p{50mm}|}}\\hline/g ;
$TEXT =~ s/\$table_end/\\end{tabular}\\end{table}/g;
#$TEXT =~ s/<caption>/\\caption\{ /g;
#$TEXT =~ s/<\/caption>/\}\n /g;
$TEXT =~ s/<tr[^>]*>/\\\\/g;
$TEXT =~ s/<\/tr>/\\\\/g;
$TEXT =~ s/<(td|th) colspan=(\d)( nowrap)?>/\\multicolumn{$2}{|c|}{/g;
$TEXT =~ s/<\/(td|th)>/&/g;
$TEXT =~ s/<(td|th)[^>]*>/&/g;
$TEXT =~ s/&&/&/g;
$TEXT =~ s/\_/_/g;
$TEXT =~ s/&amp;/\\&/g;
$TEXT =~ s'\\\\&'\\\\'g;
$TEXT =~ s/&nbsp;/ /g;
$TEXT =~ s/&eacute;/é/g;
$TEXT =~ s/&egrave;/è/g;
$TEXT =~ s/&agrave;/à/g;
$TEXT =~ s/&ecirc;/ê/g;
$TEXT =~ s/&lt;/</g;
$TEXT =~ s/<(H|h)3>/\\subsection{/g;
$TEXT =~ s/<\/(H|h)3>/}/g;
$TEXT =~ s/<(H|h)4>/\\subsubsection{/g;
$TEXT =~ s/<\/(H|h)4>/}/g;
$TEXT =~ s/<(H|h)2>/\\section{/g;
$TEXT =~ s/<\/(H|h)2>/}/g;
$TEXT =~ s/&gt;/>/g;
$TEXT =~ s/\#//g;
$TEXT =~ s/<hr>/\\hrule/g;
$TEXT =~ s/<pre>/\\begin{verbatim}/g;
$TEXT =~ s/<\/pre>/\\end{verbatim}/g;
$TEXT =~ s/\n{3,}/\n\n/g;
$TEXT =~ s/<a[^>]+>([^<]+)<\/a>/\\url{$1}/g;

$TEXT ;
}

sub out { my ($bloc, $text) = @_ ;
  open  (OUT, ">$bloc") || die "peut pas créer $bloc";
  print OUT "/* Automatically generated by bin/oefdoc.pl */\n" . $text ; close OUT ;
}

sub sortuniq {
  my $prev = "not $_[0]";
  grep { $_ ne $prev && ($prev = $_, 1) } sort @_;
}
