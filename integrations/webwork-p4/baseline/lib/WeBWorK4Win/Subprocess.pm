# GROK_V1_6
package WeBWorK4Win::Subprocess;

use strict;
use warnings;
use Config ();
use POSIX  ();

# Mojo::IOLoop::Subprocess refuses Perl's Windows pseudofork, so PG problems
# never render. Run that callback on the event loop instead. A bad problem can
# take down this one Morbo process; that is acceptable for the loopback pilot.
sub import {
	return unless $Config::Config{d_pseudofork};
	require Mojo::IOLoop::Subprocess;
	require Mojo::Promise;
	no warnings qw(redefine once);
	*Mojo::IOLoop::Subprocess::run_p = sub {
		my ($self, $child) = @_;
		my $promise = Mojo::Promise->new;
		$self->ioloop->next_tick(sub {
			_numeric_locale_c();
			_accept_comma_decimals();
			_refresh_macro_inits();
			my $results = eval { [$self->$child] };
			my $error   = $@;
			if ($error) {
				$promise->reject($error);
			} else {
				$promise->resolve(@$results);
			}
		});
		return $promise;
	};
	# Locale only. Loading Value here also loads the PG translator before
	# Morbo sets MOJO_MODE, and the translator then has no safe cache.
	_numeric_locale_c();
	_arm_form_flatten();
	return;
}

# Mojolicious turns a repeated form name into an array. A problem then sees
# displayMode as an array, and a repeated answer can be lost. Keep the last
# value, which is what a scalar param() returns.
my $form_flattened = 0;

sub _arm_form_flatten {
	return if $form_flattened;
	unshift @INC, sub {
		my (undef, $file) = @_;
		return if $file ne 'WeBWorK/Utils/Rendering.pm';
		for my $inc (@INC) {
			next if ref $inc;
			my $path = "$inc/$file";
			next unless -f $path;
			open my $in, '<:raw', $path or next;
			my $src = do { local $/; <$in> };
			close $in;
			$src =~ s/\n1;\s*\z/\nWeBWorK4Win::Subprocess::_flatten_renderPG();\n1;\n/;
			open my $out, '<', \$src;
			return $out;
		}
		return;
	};
	return;
}

sub _flatten_renderPG {
	return if $form_flattened;
	$form_flattened = 1;
	my $render = \&WeBWorK::Utils::Rendering::renderPG;
	no warnings 'redefine';
	*WeBWorK::Utils::Rendering::renderPG = sub {
		my $fields = $_[5];
		if (ref $fields eq 'HASH') {
			for my $key (keys %$fields) {
				my $value = $fields->{$key};
				$fields->{$key} = $value->[-1] if ref $value eq 'ARRAY' && @$value;
			}
		}
		return $render->(@_);
	};
	return;
}

# The PG safe is built for one problem per process. This Morbo worker renders
# every problem in-process, so a macro init that copied the answer hash keeps
# the first request's hash and later answers look blank. Run those inits again.
my $macros_refreshed = 0;
my %macro_init_running;

sub _macro_init_name {
	my ($file_name) = @_;
	return undef unless defined $file_name && $file_name =~ /\.(?:pl|pg)\z/ && $file_name !~ /\APG\.pl\z/;
	my $name = $file_name;
	$name =~ s/\.(?:pl|pg)\z//;
	$name =~ s/[^a-zA-Z0-9_]/_/g;
	return "_${name}_init";
}

sub _refresh_macro_inits {
	return if $macros_refreshed;
	$macros_refreshed = 1;
	require PGloadfiles;
	my $load = \&PGloadfiles::loadMacros;
	no warnings 'redefine';
	*PGloadfiles::loadMacros = sub {
		my ($self, @files) = @_;
		my @again;
		for my $file (@files) {
			my $name = _macro_init_name($file);
			next if !$name || $macro_init_running{$name};
			my $init = eval { \&{ 'main::' . $name } };
			next if !$init || !defined(&$init);
			push @again, [ $name, $init ];
		}
		my $result = $load->($self, @files);
		for my $pair (@again) {
			my ($name, $init) = @$pair;
			next if $macro_init_running{$name};
			local $macro_init_running{$name} = 1;
			$init->();
		}
		return $result;
	};
	return;
}

# A French locale can cache "2,718..." as the string form of a number. PG then
# refuses that text. Rebuild those values as plain numbers with a dot.
my $numbers_fixed = 0;
my $thread_locale;
my $per_thread_locale;
my $crt_setlocale;

sub _numeric_locale_c {
	if (!defined $thread_locale) {
		$thread_locale = eval { require Win32::API; Win32::API->new('kernel32', 'SetThreadLocale', 'N', 'N') };
		$thread_locale = 0 if !$thread_locale;
		# 1 is _ENABLE_PER_THREAD_LOCALE. Without it the UCRT keeps the French
		# user locale for numeric conversion, so "1.5" becomes 1.
		$per_thread_locale = eval { Win32::API->new('ucrtbase', '_configthreadlocale', 'I', 'I') };
		$per_thread_locale = 0 if !$per_thread_locale;
		# POSIX::setlocale can report C while sprintf still uses the French CRT locale.
		$crt_setlocale = eval { Win32::API->new('ucrtbase', 'setlocale', 'IP', 'P') };
		$crt_setlocale = 0 if !$crt_setlocale;
	}
	$per_thread_locale->Call(1) if $per_thread_locale;
	# 0x0409 is English (United States). Category 4 is LC_NUMERIC.
	$thread_locale->Call(0x0409) if $thread_locale;
	$crt_setlocale->Call(4, 'C') if $crt_setlocale;
	POSIX::setlocale(POSIX::LC_NUMERIC(), 'C');
	return;
}

sub _comma_decimal_to_number {
	my ($shown) = @_;
	return undef unless defined $shown && !ref($shown);
	my $text = "$shown";
	return undef unless $text =~ /\A\s*[-+]?\d+,\d+(?:[Ee][-+]?\d+)?\s*\z/;
	my $dotted = $text;
	$dotted =~ tr/,/./;
	$dotted =~ s/\A\s+|\s+\z//g;
	# Do not numify $dotted: under a French locale "1.5" + 0 is 1.
	return undef unless $dotted =~ /\A([-+]?)(\d+)\.(\d+)(?:[Ee]([-+]?\d+))?\z/i;
	my $parsed = $2 + ($3 / (10**length($3)));
	$parsed = -$parsed if $1 eq '-';
	$parsed *= 10**$4 if defined $4;
	# A cached numeric 1.5 stringifies as "1,5" but keeps the value 1.5.
	# A typed "1,5" numifies to 1. Only the cached value is rewritten here.
	# Typed decimals are rewritten in get_response, before either checker runs.
	no warnings 'numeric';
	return undef if abs(($shown + 0) - $parsed) > (1e-6 * (abs($parsed) + 1));
	return $dotted;
}

# A whole answer that is only a decimal with a comma. "1, 2" is a list and
# is left alone. "1,5" is one and a half.
# A semicolon separates elements of a set or a list: "{34;25;10}" and "1; 2".
# Inside those elements a comma between digits is a decimal: "{1,5;2,5}".
# Problem source is not rewritten, so Compute("3*a,3*b") stays a list.
sub _student_french_answer {
	my ($shown) = @_;
	return undef unless defined $shown && !ref($shown);
	my $text = "$shown";
	if ($text =~ /\A\s*[-+]?\d+,\d+(?:[Ee][-+]?\d+)?\s*\z/) {
		my $dotted = $text;
		$dotted =~ tr/,/./;
		$dotted =~ s/\A\s+|\s+\z//g;
		return $dotted;
	}
	return undef unless $text =~ /;/;
	my $out = $text;
	$out =~ s/(\d),(\d)/$1.$2/g;
	$out =~ s/\s*;\s*/,/g;
	return $out;
}

sub _rewrite_comma_args {
	foreach my $arg (@_) {
		next if ref $arg;
		my $number = _comma_decimal_to_number($arg);
		$arg = $number if defined $number;
	}
	return;
}

sub _accept_comma_decimals {
	_refresh_macro_inits();
	return if $numbers_fixed;
	$numbers_fixed = 1;
	require Value;
	require Value::Formula;
	my $get_type       = \&Value::getType;
	my $get_value_type = \&Value::getValueType;
	my $formula_new    = \&Value::Formula::new;
	my $match_number   = \&Value::matchNumber;
	no warnings 'redefine';
	# Fraction promotion calls matchNumber before Formula->new. A cached
	# "1,5" must still count as the real number 1.5, or LimitedFraction
	# rejects the rewritten decimal text.
	*Value::matchNumber = sub {
		return 1 if $match_number->(@_);
		return defined _comma_decimal_to_number($_[0]) ? 1 : 0;
	};
	# Formula->new($context, "1,5") puts the text in $_[2], not $_[1].
	*Value::Formula::new = sub {
		_rewrite_comma_args(@_);
		return $formula_new->(@_);
	};
	*Value::getType = sub {
		_rewrite_comma_args(@_);
		return $get_type->(@_);
	};
	*Value::getValueType = sub {
		_rewrite_comma_args(@_);
		return $get_value_type->(@_);
	};
	# Both MathObjects and strict_num_cmp read the blank through this method.
	# Rewriting here accepts 1,5, 1.5, and a set written {34;25;10}.
	require PGresponsegroup;
	my $get_response = \&PGresponsegroup::get_response;
	*PGresponsegroup::get_response = sub {
		my $value = $get_response->(@_);
		return $value if ref $value;
		my $rewritten = _student_french_answer($value);
		return defined $rewritten ? $rewritten : $value;
	};
	return;
}

1;
