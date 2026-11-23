# GROK_V1 P4 copy of LW Import_Set_DEBIAN.pl (paths only) (not in the Windows overlay): import a set .def into a course and assign it to all users.
# Usage: perl Import_Set_DEBIAN.pl COURSE setFILE.def
use strict;
use warnings;
use FindBin;
BEGIN { $ENV{WEBWORK_ROOT} // die "set WEBWORK_ROOT\n"; $ENV{PG_ROOT} //= "$ENV{WEBWORK_ROOT}/pg"; }
use lib "$FindBin::Bin/lib", "$ENV{WEBWORK_ROOT}/lib", "$ENV{PG_ROOT}/lib";
use WeBWorK::CourseEnvironment;
use WeBWorK::DB;
use WeBWorK::File::SetDef qw(importSetsFromDef);
my ($course, $def) = @ARGV;
my $ce = WeBWorK::CourseEnvironment->new({ webwork_dir => $ENV{WEBWORK_ROOT}, courseName => $course });
my $db = WeBWorK::DB->new($ce);
my ($added, $skipped, $errors) = importSetsFromDef($ce, $db, [$def], undef, 'all');
print "added: @$added\nskipped: @$skipped\n";
print "error: ", join(' ', map { ref $_ ? "@$_" : $_ } @$_), "\n" for @$errors;
my @p = $db->listGlobalProblems($added->[0] // $skipped->[0]);
print "problems in set: ", scalar(@p), "\n";
