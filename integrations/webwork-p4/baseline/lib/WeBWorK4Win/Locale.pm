# GROK_V1_1
package WeBWorK4Win::Locale;

use strict;
use warnings;

# A French Windows locale stringifies 2.718 as "2,718" inside "use locale".
# PG then refuses that text as a formula constant. Keep LC_NUMERIC at C even
# when another library resets the process locale.
use POSIX qw(locale_h);

my $setlocale = \&POSIX::setlocale;
{
	no warnings 'redefine';
	*POSIX::setlocale = sub {
		my $category = $_[0];
		my $result   = $setlocale->(@_);
		if (@_ > 1 && defined $_[1] && $_[1] ne '' && defined $category && $category == LC_ALL()) {
			$setlocale->(LC_NUMERIC(), 'C');
		} elsif (@_ > 1 && defined $_[1] && $_[1] ne '' && defined $category && $category == LC_NUMERIC()) {
			$result = $setlocale->(LC_NUMERIC(), 'C') if $_[1] ne 'C';
		}
		return $result;
	};
}

setlocale(LC_NUMERIC, 'C');

1;
