# GROK_V1 P4 copy (P9): copied read-only from LW_WEBWORK's webwork4win-mariadb; only the default paths changed to
# /workspace/mw-p4-20261123/ww (own instance). LW's original is untouched.
# MARIADB_1 grading-baseline worker. One stdin line: course<TAB>relative-path<TAB>seed.
# Pass 1 renders with processAnswers and empty inputs to read each answer's correct_ans.
# Pass 2 submits those correct answers; pass 3 submits a perturbed copy. Prints one JSON line.
# GRADE_DROP_SITE_MACROS=1 removes the site-macros dir (decimal-comma customization) from macrosPath.
# Upstream WeBWorK::PG only; no overlay module unless WW4WIN_MODULES is set.
use strict;
use warnings;
use FindBin;
BEGIN {
	$ENV{WEBWORK_ROOT} //= '/workspace/mw-p4-20261123/ww/src';
	$ENV{PG_ROOT}      //= "$ENV{WEBWORK_ROOT}/pg";
}
use lib "$FindBin::Bin/lib", "$ENV{WEBWORK_ROOT}/lib", "$ENV{PG_ROOT}/lib";
use JSON::PP ();
my @ww4win = split ' ', ($ENV{WW4WIN_MODULES} // '');
for my $m (@ww4win) { next if $m eq 'Templates'; eval "use WeBWorK4Win::$m; 1" or die $@; }
use WeBWorK::PG;
$| = 1;
my $json = JSON::PP->new->ascii->canonical;

sub run_pg {
	my ($course, $relative, $seed, $inputs) = @_;
	my @mp;
	if ($ENV{GRADE_DROP_SITE_MACROS}) {  # stock grading without the site parserCustomization.pl, same config otherwise
		require WeBWorK::PG::Environment;
		@mp = (macrosPath => [ grep { !m{/site-macros/?$} } @{ WeBWorK::PG::Environment->new($course)->{directories}{macrosPath} } ]);
	}
	my $templates = ($ENV{WW_COURSES} // '/workspace/mw-p4-20261123/ww/courses') . "/$course/templates/";
	my $pg = WeBWorK::PG->new(
		courseName => $course, sourceFilePath => $relative, templateDirectory => $templates,
		tempDirectory => "$FindBin::Bin/tmp/pg-render", displayMode => 'MathJax',
		processAnswers => 1, problemSeed => $seed, showHints => 0, showSolutions => 0,
		inputs_ref => $inputs, @mp, showFeedback => 1, showAttemptResults => 1, forceShowAttemptResults => 1,
	);
	my $err = $pg->{errors} // '';
	$err = join("\n", @$err) if ref $err eq 'ARRAY';
	my %out = (errors => "$err", flag => ($pg->{flags}{error_flag} // 0), score => $pg->{result}{score},
		order => [ @{ $pg->{flags}{ANSWER_ENTRY_ORDER} // [] } ], answers => {});
	for my $name (@{ $out{order} }) {
		my $ah = $pg->{answers}{$name} or next;
		$out{answers}{$name} = { correct_ans => $ah->{correct_ans}, score => $ah->{score} };
	}
	$pg->free if $pg->can('free');
	return \%out;
}

sub perturb {
	my ($v) = @_;
	return 'zzzwrong' if !defined $v || $v eq '';
	# Change the first number: n -> 2n+1 (never equal, also outside relative tolerance).
	return $v =~ s/(\d+(?:\.\d+)?)/2*$1+1/er if $v =~ /\d/;
	return 'zzzwrong';
}

while (my $line = <STDIN>) {
	$line =~ s/\r?\n\z//;
	my ($course, $relative, $seed) = split /\t/, $line, 3;
	my %r = (path => $relative, seed => $seed);
	eval {
		my $p1 = run_pg($course, $relative, $seed, {});
		my $e = $p1->{errors}; $e =~ s/\s+/ /g;
		if ($p1->{flag} || $e =~ /\S/) { %r = (%r, status => 'RENDER_ERR', detail => substr($e, 0, 200)); die "skip\n"; }
		my @names = grep { exists $p1->{answers}{$_} } @{ $p1->{order} };
		my %good = map { $_ => ($p1->{answers}{$_}{correct_ans} // '') } @names;
		my %bad  = map { $_ => perturb($good{$_}) } @names;
		my $p2 = run_pg($course, $relative, $seed, \%good);
		my $p3 = run_pg($course, $relative, $seed, \%bad);
		# Decimal-comma probe: correct answers that are one decimal number, written with a comma.
		my @dec = grep { $good{$_} =~ /^\s*[-+]?\d+\.\d+\s*$/ } @names;
		my $s4 = '';
		if (@dec) {
			my %comma = %good;
			$comma{$_} =~ s/\./,/ for @dec;
			my $p4 = run_pg($course, $relative, $seed, \%comma);
			$s4 = defined $p4->{score} ? 0 + $p4->{score} : -1;
		}
		my ($s2, $s3) = map { defined $_->{score} ? 0 + $_->{score} : -1 } $p2, $p3;
		my $status = !@names ? 'NO_ANSWERS' : $s2 < 0.9999 ? 'SELF_FAIL' : $s3 >= 0.9999 ? 'PERTURB_FAIL' : 'PASS';
		%r = (%r, status => $status, n_answers => scalar @names, score_correct => $s2, score_perturbed => $s3, score_comma => $s4, n_decimal_blanks => scalar @dec,
			correct => join(' | ', map { $good{$_} } @names), perturbed => join(' | ', map { $bad{$_} } @names),
			failing_blanks => join(' ', grep { ($p2->{answers}{$_}{score} // 0) < 0.9999 } @names));
	};
	if ($@ && $@ ne "skip\n") { my $m = "$@"; $m =~ s/\s+/ /g; %r = (%r, status => 'WORKER_ERR', detail => substr($m, 0, 200)); }
	print $json->encode(\%r), "\n";
}
