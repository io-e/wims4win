# GROK_V1_2
package WeBWorK4Win::Templates;

use strict;
use warnings;

# Pandoc.pm uses say(). Loaded from Mojolicious' string eval on this Perl, that
# keyword is not enabled and the problem editor cannot be compiled. Load it
# once in this ordinary scope; later "use Pandoc" then skips compilation.
require Pandoc;

# Embedded Perl declares "my $name" only for stash keys present at the first
# compile. The editor template also uses names that are filled in just before
# render. Declare those names too. The compiled sub still reads the live stash.
my @editor_vars = qw(problemContents formsToShow actionFormTitles hardcopyLabels sampleProblemMetadata);
my $editor_paths_installed = 0;

# Upstream treats only a leading "/" as an absolute path. Drive paths such as
# D:/... then raise a false error and are joined onto the templates directory.
sub _windows_absolute {
	my ($path) = @_;
	return defined $path && $path =~ m{\A[A-Za-z]:[\\/]};
}

sub _slash {
	my ($path) = @_;
	$path =~ s{\\}{/}g if defined $path;
	return $path;
}

sub _install_editor_paths {
	return if $editor_paths_installed;
	require WeBWorK::ContentGenerator::Instructor::PGProblemEditor;

	my $pkg = 'WeBWorK::ContentGenerator::Instructor::PGProblemEditor';
	my ($temp, $orig, $is_temp);
	{
		no strict 'refs';
		$temp    = \&{"${pkg}::determineTempEditFilePath"};
		$orig    = \&{"${pkg}::determineOriginalEditFilePath"};
		$is_temp = \&{"${pkg}::isTempEditFilePath"};
	}
	my $bad = \&WeBWorK::ContentGenerator::addbadmessage;

	no warnings qw(redefine once);
	no strict 'refs';
	*{"${pkg}::isTempEditFilePath"} = sub {
		my ($c, $path) = @_;
		if (_windows_absolute($path)) {
			my $tmp = _slash($c->getTempEditFileDirectory());
			return _slash($path) =~ /^\Q$tmp\E/ ? 1 : 0;
		}
		return $is_temp->(@_);
	};
	*{"${pkg}::determineTempEditFilePath"} = sub {
		my ($c, $path) = @_;
		return $temp->(@_) unless _windows_absolute($path);
		local *WeBWorK::ContentGenerator::addbadmessage = sub {
			my ($obj, $message) = @_;
			return if defined $message && $message eq 'The path to the original file should be absolute.';
			return $bad->(@_);
		};
		return $temp->(@_);
	};
	*{"${pkg}::determineOriginalEditFilePath"} = sub {
		my ($c, $path) = @_;
		return $orig->(@_) unless _windows_absolute($path);
		my $ce      = $c->ce;
		my $newpath = $path;
		if ($c->isTempEditFilePath($newpath)) {
			my $tmp = _slash($c->getTempEditFileDirectory());
			$newpath = _slash($newpath);
			$newpath =~ s|^\Q$tmp\E/||;
			if ($newpath =~ m|blank\.[^/]*$|) {
				$newpath = $ce->{webworkFiles}{screenSnippets}{blankProblem};
			} elsif ($newpath =~ m|hardcopyHeader\.[^/]*$|) {
				$newpath = $ce->{webworkFiles}{hardcopySnippets}{setHeader};
			} elsif ($newpath =~ m|screenHeader\.[^/]*$|) {
				$newpath = $ce->{webworkFiles}{screenSnippets}{setHeader};
			} elsif ($newpath =~ m|hardcopyTheme\.([^/]*\.xml)\.[^/]*$|) {
				$newpath = "$ce->{courseDirs}{hardcopyThemes}/$1";
			} else {
				my $user = $c->param('user');
				$newpath =~ s|\.$user\.tmp$||;
			}
		}
		return $newpath;
	};
	$editor_paths_installed = 1;
	return;
}

sub import {
	# File::Find chdir fails inside the Morbo worker on this machine, and the
	# failure is silent. The sample-problem index then stays empty. Search by
	# absolute path instead. The parser reads $File::Find::name, not $_.
	require WeBWorK::PG::SampleProblemParser;
	my $parser_find = \&WeBWorK::PG::SampleProblemParser::find;
	no warnings qw(redefine once);
	*WeBWorK::PG::SampleProblemParser::find = sub {
		my $spec = shift;
		if (ref($spec) eq 'HASH') {
			$spec->{no_chdir} = 1;
			return $parser_find->($spec, @_);
		}
		return $parser_find->({ wanted => $spec, no_chdir => 1 }, @_);
	};

	require Mojo::Template;
	my $wrap = \&Mojo::Template::_wrap;
	no warnings qw(redefine once);
	*Mojo::Template::_wrap = sub {
		my ($self, $body, $vars) = @_;
		_install_editor_paths() if ($self->name // '') =~ /PGProblemEditor/;
		if ($self->vars && ref($vars) eq 'HASH') {
			my %copy = %$vars;
			for my $name (@editor_vars) {
				next if exists $copy{$name};
				next if $body !~ /(?<![\w\$:])\$$name\b/;
				$copy{$name} = undef;
			}
			return $wrap->($self, $body, \%copy);
		}
		return $wrap->(@_);
	};
	return;
}

1;
