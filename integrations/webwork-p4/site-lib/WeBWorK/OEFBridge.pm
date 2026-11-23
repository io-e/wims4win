package WeBWorK::OEFBridge;
# GROK_V1 P4 trusted module: WeBWorK (PG, inside Safe) -> OEF worker (127.0.0.1:3003) -> WIMS.
# Listed in $pg{modules} (localOverrides.conf; under webwork2 the pg_config.yml list is replaced by the course
# environment's) and in pg_config.yml (standalone PG). Exactly two public functions: oef_render, oef_check.
#
# Constraints found in A3.3 step 1 (p4_results/A33_step1_module_probe.md):
#   - inside Safe, class-name method calls at run time fail (the root stash is the compartment's): objects (JSON,
#     HTTP::Tiny) are built at load time, and HTTP::Tiny / IO::Socket::IP / IO::Socket / Socket / Errno are preloaded.
#     Client: http_client=builtin (default: HTTP/1.0 on builtins, separate connect/read deadlines, 8 MiB cap, no
#     redirect, Connection: close, JSON content type required) or http_client=tiny (HTTP::Tiny, timeout, max_size,
#     max_redirect=0, keep_alive=0). Tried first as LW_WEBWORK asked; inside Safe it dies on HTTP::Tiny::Handle->new
#     even with HTTP::Tiny, IO::Socket::IP, IO::Socket, Socket, Errno preloaded, so builtin is the default (gate P1b);
#   - no alarm(): WeBWorK uses it for its own render timeout.
# URL and token come from the site config file named by OEF_BRIDGE_CONF (read once at load, outside Safe), never
# from the problem. Only loopback URLs are accepted. The token never enters PG memory visible to problems
# (lexical in this file).
use strict;
use warnings;
use Socket qw(PF_INET SOCK_STREAM IPPROTO_TCP SOL_SOCKET SO_ERROR inet_aton pack_sockaddr_in);
use Fcntl qw(F_GETFL F_SETFL O_NONBLOCK);
use Errno qw(EINPROGRESS EWOULDBLOCK EAGAIN EINTR);
use Errno ();
use Time::HiRes ();
use JSON::PP ();
# Preloaded at trusted-module load time (outside Safe), so nothing is required lazily inside the compartment
# (HTTP::Tiny requires IO::Socket::IP at its first connect, which fails inside Safe).
use IO::Socket ();
use IO::Socket::IP ();
use HTTP::Tiny ();

my $MAX_RESPONSE = 8 * 1024 * 1024;

my $JSON = JSON::PP->new->utf8->canonical;
my ($HOST, $PORT, $TOKEN, $CONNECT_T, $READ_T, $CONF_ERR) = ('127.0.0.1', 3003, '', 2, 15, '');
my $CLIENT = 'builtin';    # http_client = builtin (default, works inside Safe) | tiny (HTTP::Tiny; fails inside Safe, P1b)
{
	my $file = $ENV{OEF_BRIDGE_CONF} // '';
	if (open my $fh, '<', $file) {
		my %c;
		while (<$fh>) { next if /^\s*(#|$)/; chomp; my ($k, $v) = split /\s*=\s*/, $_, 2; $c{$k} = $v if defined $v }
		close $fh;
		if (($c{url} // '') =~ m{^http://(127\.0\.0\.1):(\d{2,5})/?$}) { ($HOST, $PORT) = ($1, $2) }
		else { $CONF_ERR = 'bad url in OEF bridge config (loopback only)' }
		if (defined $c{token_file} && open my $tf, '<', $c{token_file}) { $TOKEN = <$tf> // ''; chomp $TOKEN; close $tf }
		$CONNECT_T = $c{connect_timeout} if ($c{connect_timeout} // '') =~ /^\d+(\.\d+)?$/;
		$READ_T    = $c{read_timeout}    if ($c{read_timeout} // '')    =~ /^\d+(\.\d+)?$/;
		$CLIENT    = $c{http_client}     if ($c{http_client} // '')     =~ /^(tiny|builtin)$/;
	} else {
		$CONF_ERR = 'OEF bridge config not found';
	}
	$CONF_ERR ||= 'OEF token missing' if length($TOKEN) < 32;
}
# HTTP::Tiny object built here, outside Safe (a class-name call such as HTTP::Tiny->new inside the compartment
# would resolve against the compartment's root stash). One timeout (HTTP::Tiny applies it to connect and to each
# read), response size cap, no redirect, no keep-alive, no proxy from the environment.
my $HT = HTTP::Tiny->new(timeout => $READ_T, max_size => $MAX_RESPONSE, max_redirect => 0, keep_alive => 0,
	proxy => undef, http_proxy => undef, https_proxy => undef, no_proxy => [], verify_SSL => 0,
	agent => 'WeBWorK-OEFBridge/GROK_V1');
my %CACHE;    # per PG run (each render is a forked child): one check per submission

sub _post {
	my ($path, $body) = @_;
	return { ok => 0, error => $CONF_ERR, transport => 1 } if $CONF_ERR;
	# Connection refused is retried for up to ~2 s: the worker recycles itself every OEF_MAX_REQUESTS calls and its
	# supervisor needs ~0.8 s to restart it (gate P10b). Timeouts are never retried.
	my $r;
	for my $try (1 .. 6) {
		$r = $CLIENT eq 'builtin' ? _post_builtin($path, $body) : _post_tiny($path, $body);
		last unless $r->{refused};
		select(undef, undef, undef, 0.4) if $try < 6;
	}
	delete $r->{refused};
	return $r;
}

sub last_client { $CLIENT }

sub _post_tiny {
	my ($path, $body) = @_;
	my $payload = $JSON->encode($body);
	my $r = eval {
		$HT->request('POST', "http://$HOST:$PORT$path", { content => $payload,
			headers => { 'Content-Type' => 'application/json', 'X-LumaPlus-Token' => $TOKEN, 'Connection' => 'close' } });
	};
	return { ok => 0, error => 'OEF bridge HTTP client failure', transport => 1 } unless $r;
	# HTTP::Tiny reports transport errors (refused, timeout, size cap) as status 599
	# Under webwork2 this path ends here: HTTP::Tiny calls HTTP::Tiny::Handle->new (a class-name call) at request
	# time, which inside Safe dies even with everything preloaded (evidence: p4_results/P1b_http_client.md).
	return { ok => 0, error => 'OEF bridge HTTP::Tiny: ' . substr($r->{content} // '', 0, 120), transport => 1 }
		if $r->{status} == 599 && ($r->{content} // '') =~ /Can't locate object method/;
	return { ok => 0, error => ($r->{content} // '') =~ /timed? ?out/i ? 'OEF worker timeout' : 'OEF worker unreachable',
		transport => 1, refused => (($r->{content} // '') =~ /refused/i ? 1 : 0) } if $r->{status} == 599 && ($r->{content} // '') !~ /size/i;
	return { ok => 0, error => 'OEF worker response too large', transport => 1 } if $r->{status} == 599;
	return { ok => 0, error => "OEF worker redirect refused", transport => 1 } if $r->{status} =~ /^3/;
	my $ct = $r->{headers}{'content-type'} // '';
	return { ok => 0, error => "OEF worker HTTP $r->{status} (not JSON)", transport => 1 } unless $ct =~ m{^application/json}i;
	my $d = eval { $JSON->decode($r->{content} // '') };
	return { ok => 0, error => "OEF worker HTTP $r->{status} (malformed JSON)", transport => 1 } unless ref $d eq 'HASH';
	return _plain($d);
}

sub _post_builtin {
	my ($path, $body) = @_;
	my $payload = $JSON->encode($body);
	my $req = "POST $path HTTP/1.0\r\nHost: $HOST:$PORT\r\nX-LumaPlus-Token: $TOKEN\r\n"
		. "Content-Type: application/json\r\nContent-Length: " . length($payload) . "\r\nConnection: close\r\n\r\n$payload";
	my $sock;
	socket($sock, PF_INET, SOCK_STREAM, IPPROTO_TCP) or return { ok => 0, error => "socket: $!", transport => 1 };
	my $flags = fcntl($sock, F_GETFL, 0);
	fcntl($sock, F_SETFL, $flags | O_NONBLOCK);
	my $addr = pack_sockaddr_in($PORT, inet_aton($HOST));
	if (!connect($sock, $addr)) {
		if ($! != EINPROGRESS && $! != EWOULDBLOCK) {
			my $refused = ($! == Errno::ECONNREFUSED()) ? 1 : 0;
			close $sock; return { ok => 0, error => 'OEF worker unreachable', transport => 1, refused => $refused } }
		my $w = ''; vec($w, fileno($sock), 1) = 1;
		my $n = select(undef, my $wout = $w, undef, $CONNECT_T);
		my $err = $n > 0 ? unpack('i', getsockopt($sock, SOL_SOCKET, SO_ERROR) // pack('i', 1)) : -1;
		if ($n <= 0 || $err) { close $sock; return { ok => 0, error => 'OEF worker unreachable', transport => 1,
			refused => ($n > 0 && $err == Errno::ECONNREFUSED()) ? 1 : 0 } }
	}
	my $deadline = Time::HiRes::time() + $READ_T;
	my $off = 0;
	while ($off < length $req) {
		my $w = ''; vec($w, fileno($sock), 1) = 1;
		my $left = $deadline - Time::HiRes::time();
		last if $left <= 0;
		next unless select(undef, my $wout = $w, undef, $left) > 0;
		my $n = syswrite($sock, $req, length($req) - $off, $off);
		if (!defined $n) { next if $! == EAGAIN || $! == EINTR; last }
		$off += $n;
	}
	my $buf = '';
	while (1) {
		my $left = $deadline - Time::HiRes::time();
		if ($left <= 0) { close $sock; return { ok => 0, error => 'OEF worker timeout', transport => 1 } }
		my $r = ''; vec($r, fileno($sock), 1) = 1;
		my $n = select(my $rout = $r, undef, undef, $left);
		next if $n < 0 && $! == EINTR;
		next unless $n > 0;
		my $got = sysread($sock, my $chunk, 65536);
		if (!defined $got) { next if $! == EAGAIN || $! == EINTR; last }
		last if $got == 0;
		$buf .= $chunk;
		if (length($buf) > $MAX_RESPONSE) { close $sock; return { ok => 0, error => 'OEF worker response too large', transport => 1 } }
	}
	close $sock;
	my ($head, $content) = split /\r\n\r\n/, $buf, 2;
	my ($status) = ($head // '') =~ m{^HTTP/\d\.\d (\d{3})};
	return { ok => 0, error => 'OEF worker: no HTTP answer', transport => 1 } unless $status;
	return { ok => 0, error => 'OEF worker redirect refused', transport => 1 } if $status =~ /^3/;
	return { ok => 0, error => "OEF worker HTTP $status (not JSON)", transport => 1 } unless ($head =~ m{^Content-Type:\s*application/json}im);
	my $d = eval { $JSON->decode($content // '') };
	return { ok => 0, error => "OEF worker HTTP $status (not JSON)", transport => 1 } unless ref $d eq 'HASH';
	return _plain($d);
}

# JSON::PP booleans -> 0/1, so no blessed object reaches the Safe compartment.
sub _plain {
	my $x = shift;
	return [ map { _plain($_) } @$x ] if ref $x eq 'ARRAY';
	return { map { ($_ => _plain($x->{$_})) } keys %$x } if ref $x eq 'HASH';
	return ($x ? 1 : 0) if JSON::PP::is_bool($x);
	return $x;
}

sub oef_render {
	my ($module, $exo, $seed, $lang) = @_;
	return _post('/oef/render', { module => "$module", exo => "$exo", seed => 0 + ($seed // 0), lang => $lang // 'fr' });
}

sub oef_check {
	# $answers: arrayref in WeBWorK blank order (preferred: one call, no render needed), or hashref by WIMS id.
	my ($module, $exo, $seed, $answers, $lang) = @_;
	my %body = (module => "$module", exo => "$exo", seed => 0 + ($seed // 0), lang => $lang // 'fr');
	if (ref $answers eq 'ARRAY') { $body{answers_ordered} = [ map { '' . ($_ // '') } @$answers ] }
	else { $body{answers} = { map { ($_ => '' . ($answers->{$_} // '')) } grep {/^(reply|choice)\d+$/} keys %{ $answers // {} } } }
	my $key = $JSON->encode(\%body);
	return $CACHE{$key} //= _post('/oef/check', \%body);
}

1;
