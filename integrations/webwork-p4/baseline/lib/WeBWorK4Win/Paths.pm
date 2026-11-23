# GROK_V1_2
package WeBWorK4Win::Paths;

use strict;
use warnings;

require WeBWorK::Utils::Files;
require WeBWorK::PG::IO;

# WeBWorK rejects any path that does not start with "/". A Windows path
# such as D:/... therefore never passes, and the static routes return 404.
sub _absolute {
	my ($path) = @_;
	return defined $path && $path =~ m{\A(?:[A-Za-z]:[\\/]|[\\/])};
}

sub _normalized {
	my ($path) = @_;
	$path =~ s{\\}{/}g;
	my $drive = '';
	$drive = lc($1) . ':' if $path =~ s{\A([A-Za-z]):}{};
	my @parts;
	for my $part (split m{/}, $path, -1) {
		next if $part eq '' || $part eq '.';
		if ($part eq '..') {
			return unless @parts;
			pop @parts;
			next;
		}
		push @parts, $part;
	}
	return lc($drive) . '/' . join('/', @parts) . '/';
}

{
	no warnings qw(redefine once);
	my $path_is_subdir = sub {
		my ($path, $dir, $allow_relative) = @_;
		return 0 unless defined $dir && $dir ne '';
		if (!_absolute($path)) {
			return 0 unless $allow_relative;
			$path = "$dir/$path";
		}
		my $file_path = _normalized($path);
		my $root_path = _normalized($dir);
		return 0 unless defined $file_path && defined $root_path;
		return index($file_path, $root_path) == 0 ? 1 : 0;
	};
	*WeBWorK::Utils::Files::path_is_subdir = $path_is_subdir;
	*WeBWorK::PG::IO::path_is_subdir        = $path_is_subdir;
}

1;
