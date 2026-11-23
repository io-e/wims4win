#!/usr/bin/perl
# GROK_V1 P1b: call WeBWorK::OEFBridge::oef_render once, outside Safe or (SAFE=1) from inside a Safe compartment
# (the module loaded outside first, as webwork2 does). Prints {result, ms} as JSON.
use strict; use warnings;
use FindBin;
use lib ($ENV{P4_SITE_LIB} || (-d "$FindBin::Bin/../../site-lib" ? "$FindBin::Bin/../../site-lib" : "$FindBin::Bin/../../ww/site-lib"));
use Time::HiRes (); use JSON::PP ();
use WeBWorK::OEFBridge;
my $t0 = Time::HiRes::time();
my $r;
if ($ENV{SAFE}) {
	require Safe;
	my $c = Safe->new('PG_priv');
	$c->share_from('main', [ '*WeBWorK::OEFBridge::' ]) if 0;
	no strict 'refs';
	*{'PG_priv::WeBWorK::OEFBridge::oef_render'} = \&WeBWorK::OEFBridge::oef_render;
	$r = $c->reval(q{ WeBWorK::OEFBridge::oef_render('H3/analysis/oeflinf.fr', 'antecedant', 1, 'fr') });
	$r = { safe_error => "$@" } if $@;
} else {
	$r = WeBWorK::OEFBridge::oef_render('H3/analysis/oeflinf.fr', 'antecedant', 1, 'fr');
}
print JSON::PP->new->canonical->encode({ result => $r, ms => int(1000 * (Time::HiRes::time() - $t0)) }), "\n";
