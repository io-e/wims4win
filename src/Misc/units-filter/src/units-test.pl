#!/usr/bin/perl

while(chomp($essai = <STDIN>)){
    if(length($essai) != 0){
	if ($essai =~ /options=(.*)/){
	    $options = $1;
	    chomp($essai = <STDIN>);
	}
	chomp($reponse=<STDIN>);
	$essai_ = $essai;
	$essai_ =~ s/\'/\\\'/g;
	@args = ("echo $essai_ | ./units-filter $options > tmp.1");
	system @args;
	open (TMP,'tmp.1');
	chomp($result=<TMP>);
	if ($reponse eq $result){print "OK\t $essai ---> $reponse\n";}
	else{
	    print "***ERREUR***\n $essai ---> $result\n on attendait $reponse\n";
	}
    }
    
}

