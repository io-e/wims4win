#!/usr/bin/perl
### script for generating from the data of log/referencement
### some files which can be exploited by the search engine.
### for the moment, this is not launched by wims, only manually
use warnings;
use strict;
my $lang='fr';
my $T='';
my %hash=();
my $hash=\%hash;
my @base=("log/referencement");
my $dirout="public_html/bases/classification/src";
for my $dir (@base) {
 for my $f (glob("$dir/*/*/*/*")) {
  my @f1=split('/', $f);
  my $id="$f1[2]/$f1[3]/$f1[4]\&exo=$f1[5]";
  open (IN, "$f");
  while (<IN>) {
   my ($t0, $t1) = split ("=",$_) ;
   $t0=~ s/!set ref_//;
   my @T2= split(";", $t1);
   for my $a (@T2) {
     my @b=split(',',$a);
     my $b1=$b[0]; my $b2=$b[-1]; $b2=~ s/\n//;
     if (($b2) && $b2 >= 1) {
       if ($hash{$t0}{$id}) { $hash{$t0}{$id}.= "," . $b1 }
        else { $hash{$t0}{$id}= $b1};
     }
    }
  }
 }
}

for my $taxo (keys %hash) {
  my $T='';
  for my $id (keys %{$hash{$taxo}}) {
    $T .= "$id:" . join(',', sortuniq ( split(',',$hash{$taxo}{$id}))) . "\n"
  }
  out("$dirout/$taxo" . '_exo',$T);
}

sub out { my ($bloc, $text) = @_;
  open  (OUT, ">$bloc") ;
  print OUT $text ; close OUT;
}

sub sortuniq {
  return if !(@_) ;
  my $prev = "not $_[0]";
  grep { $_ ne $prev && ($prev = $_, 1) } sort @_;
}
