#!/usr/bin/perl -w

use POSIX qw(strftime);
#use strict ;
#use warnings;
###execute in wims directory

my $mirror='wimsedu';
my $addr='http://downloadcenter.wimsedu.info/publish/modules' ;
my $directory='public_html/modules';
my $verwims=`cat version` ; ## la version doit être écrite avec deux décimales !
my $vawims ;
($vawims = $verwims) =~ s/[0-9\.]//g;
$verwims =~ s/[a-z]//g;
my $list='';
my %data;
my $today = strftime "%Y%m%d", localtime;

open(IN, "wget -O - '$addr/index.txt' |");

while (<IN>){
  my $l = $_;
  my ($wa, $junk2, $version, $publisher, $wims, $module)=split(' ', $l) ;
  my $module1=$module ; $module=~ s,/,\~,g ;
  $wa=~ s/[0-9\.:]//g if ($wa) ;
  ## test sur la version de wims
  next if (($wims > $verwims) || ($wims == $verwims && ($wa gt $vawims) ));
  ##il y a une version plus récente déjà
  next if (($data{$module}) && (($data{$module}) > $wims));
  $list = "$today: $module1 $version\n" ;
  $data{$module}=$wims ;
  wget($module,$wims) ;
  outcat ("public_html/modules/Changelog.new",$list)
}

close IN;

sub wget { my ($mod, $vers) = @_ ;
  my @adr=split('\~', $mod);
  system("mkdir -p $directory/$adr[0] ;
    mkdir -p $directory/$adr[0]/$adr[1] ;
    cd  $directory/$adr[0]/$adr[1];
    wget -O $mod.tgz '$addr/$vers/$mod.tgz';
    tar xzf $mod.tgz ; rm $mod.tgz") ;
}

sub out { my ($bloc, $text) = @_ ;
  open  (OUT, ">$bloc") || warn "peut pas créer $bloc";
  print OUT $text ; close OUT ;
}

sub outcat { my ($bloc, $text) = @_ ;
  open  (OUT, ">>$bloc") || warn "peut pas créer $bloc";
  print OUT $text ; close OUT ;
}
