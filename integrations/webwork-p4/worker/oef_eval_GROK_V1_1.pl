#!/usr/bin/env perl
# GROK_V1_1 - P4 OEF worker: renders and grades WIMS OEF exercises for WeBWorK (WeBWorK keeps the only gradebook).
# Shaped like the Lumaplus PG worker pg_eval_CLAUDE_V1_3.pl (same token header, Host/Origin checks, JSON only,
# 64 KiB bodies, no CORS, recycling counter).
#
# Endpoints (JSON only):
#   GET  /oef/ping                                  -> {ok, engine, served, max_requests, pid}
#   POST /oef/render {module, exo, seed, lang}      -> {ok, seed, statement_html, fields:[{id,type,embedded,choices?}], step, steps, ms}
#   POST /oef/check  {module, exo, seed, lang, answers:{reply1:..., choice1:...} | answers_ordered:[...]}
#        -> {ok, seed, score, per_field:[{id,correct,score,feedback}], done, step, steps, error, view, ms}
#        WIMS: cmd=new;seed=S then cmd=reply&special_parm=p4_json (once per \steps step, while answers are given
#        for the fields of the step reached). "view" = statement of the last WIMS page reached and every field met
#        (earlier steps first), so WeBWorK needs one call per submission. A passed step whose next fields have no
#        answer yet gives done=false.
#
# Never leaves the worker: the expected answer (WIMS's p4_json hook does not send it; the output is still built
# from an allowlist of keys), the given answer, the WIMS session id (removed from HTML), WIMS URLs (images are
# fetched by the worker and inlined as data: URIs, so the browser never reaches WIMS or the worker).
#
# V1_1: the p4render call (secret) and every reply (answers) are multipart/form-data POSTs to wims.cgi?form-data<unique>
# (not in a query string, so not in access logs; cmd=new stays a GET, see wims_new); sessions are opened with the authenticated job=p4render; the module
# must be in the site allowlist OEF_MODULES and an OEF exercise module (F1): classified path ^[A-Z][0-9]+/..., not adm/home/classes/devel, with def/
# and src/ folders, and the exo must have def/<exo>.def.
#
# Security: loopback listen only (refuses to start otherwise); X-LumaPlus-Token compared in constant time;
# Host must equal the listen address; Origin, when present, must equal OEF_ALLOWED_ORIGIN; POST bodies must be
# application/json and at most 64 KiB; module resolved inside OEF_MODULE_ROOTS (no "..", no absolute path,
# must be a directory with a def/ folder); exo ^[A-Za-z0-9_]+$; seed integer 0..10^9; answer values at most
# 512 bytes, at most 40 fields; rate limit per token (OEF_RATE per minute); per-call WIMS timeout.
#
# Environment:
#   OEF_TOKEN or OEF_TOKEN_FILE   shared secret, >= 32 chars (file: gitignored secrets/oef_token)
#   OEF_WIMS_TOKEN_FILE           V1_1: WIMS-side P4 secret (64 hex, gitignored secrets/wims_p4_token), sent to
#                                 adm/raw job=p4render in a multipart POST field (never in a URL or a log)
#   OEF_LISTEN                    default 127.0.0.1:3003 (loopback only)
#   OEF_ALLOWED_ORIGIN            default http://127.0.0.1:3020
#   OEF_WIMS_BASE                 default http://127.0.0.1:8892/wims/wims.cgi (loopback only)
#   OEF_MODULE_ROOTS              WIMS modules dirs separated by "|" (required)
#   OEF_MODULES                   V1_1 (F1, required): EXACT allowlist of the OEF modules the site permits, separated
#                                 by spaces, commas or "|" (e.g. "H3/analysis/oeflinf.fr U2/arithmetic/oefrational.fr").
#                                 A .pg author can only pick a module from it; anything else is refused.
#   OEF_TIMEOUT                   seconds per WIMS call, default 10
#   OEF_MAX_REQUESTS              connections accepted by one worker process before it is replaced (prefork), default 2000
#   OEF_WORKERS                   worker processes of the pre-forking server, default 1 (1..8)
#   OEF_PREFORK_PIDFILE           manager pid file, default <tmpdir>/oef_eval_prefork_<host_port>.pid
#   OEF_RATE                      requests per minute per token, default 600
#   OEF_LOG_ANSWERS               never; answers and statements are not logged (only module/exo/seed/status/ms)
use strict;
use warnings;
use utf8;
use Mojolicious::Lite -signatures;
use Mojo::UserAgent;
use Mojo::DOM;
use Mojo::URL;
use Mojo::Util qw(b64_encode secure_compare url_escape);
use Time::HiRes qw(time);
use Encode ();
use File::Spec;

sub read_token {
	return $ENV{OEF_TOKEN} if defined $ENV{OEF_TOKEN} && length $ENV{OEF_TOKEN};
	my $f = $ENV{OEF_TOKEN_FILE} // '';
	open my $fh, '<', $f or die "OEF_TOKEN_FILE not readable\n";
	my $t = <$fh>; close $fh; chomp $t; return $t;
}
my $TOKEN = read_token();
my $WIMS_TOKEN = do {
	my $f = $ENV{OEF_WIMS_TOKEN_FILE} // '';
	open my $fh, '<', $f or die "OEF_WIMS_TOKEN_FILE not readable\n";
	my $t = <$fh> // ''; close $fh; chomp $t; $t;
};
die "WIMS P4 token must be 64 lowercase hex\n" unless $WIMS_TOKEN =~ /^[0-9a-f]{64}$/;
die "OEF token shorter than 32 characters\n" unless length($TOKEN) >= 32;
my $LISTEN = $ENV{OEF_LISTEN} || '127.0.0.1:3003';
$LISTEN =~ s{^http://}{};
my ($LHOST, $LPORT) = $LISTEN =~ /^(127\.0\.0\.1|localhost|\[::1\]):(\d{2,5})$/
	or die "OEF_LISTEN must be a loopback address (127.0.0.1:PORT), got '$LISTEN'\n";
my $HOSTPORT = "$LHOST:$LPORT";
my $ORIGIN = $ENV{OEF_ALLOWED_ORIGIN} || 'http://127.0.0.1:3020';
my $WIMS = $ENV{OEF_WIMS_BASE} || 'http://127.0.0.1:8892/wims/wims.cgi';
die "OEF_WIMS_BASE must be a loopback http URL\n" unless $WIMS =~ m{^http://(127\.0\.0\.1|localhost):\d+/[\w./-]*$};
my @ROOTS = grep { length && -d } split /\|/, ($ENV{OEF_MODULE_ROOTS} // '');
die "OEF_MODULE_ROOTS has no existing directory\n" unless @ROOTS;
my $MODULE_RE = qr{^[A-Z][0-9]{1,2}(?:/[A-Za-z0-9_][A-Za-z0-9_.-]*){1,3}$};
my %ALLOW;
for my $m (grep { length } split /[\s,|]+/, ($ENV{OEF_MODULES} // '')) {
	die "OEF_MODULES: bad module name $m\n" unless $m =~ $MODULE_RE && $m !~ m{\.\.};
	$ALLOW{$m} = 1;
}
die "OEF_MODULES (exact allowlist of permitted OEF modules) is required\n" unless %ALLOW;
my $TIMEOUT = $ENV{OEF_TIMEOUT} || 10;
my $MAX_REQUESTS = $ENV{OEF_MAX_REQUESTS} || 2000;
my $RATE = $ENV{OEF_RATE} || 600;
my $MAX_FIELDS = 40;
my $MAX_ANSWER = 512;

my $ua = Mojo::UserAgent->new(max_redirects => 3, connect_timeout => 3, request_timeout => $TIMEOUT,
	inactivity_timeout => $TIMEOUT);
$ua->transactor->name('P4-OEF-GROK');

# ---- validation ----
# F1 (V1_1): an OEF exercise module only. WIMS classifies public exercise modules as <Letter><digit>/<area>/<name>
# (H3/analysis/oeflinf.fr, U2/arithmetic/oefrational.fr); adm/, home, classes/, devel/, local/... never match.
sub resolve_module ($m) {
	die "module missing\n" unless defined $m && !ref $m && length $m;
	die "absolute module path refused\n" if File::Spec->file_name_is_absolute($m);
	die "bad module name\n" unless $m =~ $MODULE_RE && $m !~ m{\.\.};
	die "module not in the site allowlist\n" unless $ALLOW{$m};
	for my $r (@ROOTS) {
		next unless -d "$r/$m";
		die "not an OEF exercise module\n" unless -d "$r/$m/def" && -d "$r/$m/src" && !-l "$r/$m";
		return $m;
	}
	die "module not found in the allowed roots\n";
}
sub check_exo ($e) { die "bad exo name\n" unless defined $e && !ref $e && $e =~ /^[A-Za-z0-9_]+$/; $e }
sub check_exo_in ($m, $e) {
	check_exo($e);
	for my $r (@ROOTS) { return $e if -f "$r/$m/def/$e.def" }
	die "exercise not found in the module\n";
}
sub check_seed ($s) { die "bad seed\n" unless defined $s && !ref $s && $s =~ /^\d{1,9}$/; 0 + $s }
sub check_lang ($l) { $l //= 'fr'; die "bad lang\n" unless $l =~ /^[a-z]{2}$/; $l }
sub check_answers ($a) {
	$a //= {};
	die "answers must be an object\n" unless ref $a eq 'HASH';
	die "too many answers\n" if keys %$a > $MAX_FIELDS;
	my %out;
	for my $k (keys %$a) {
		die "bad answer key\n" unless $k =~ /^(?:reply|choice)\d{1,2}$/;
		my $v = $a->{$k};
		$v = join(',', @$v) if ref $v eq 'ARRAY';
		die "bad answer value\n" if ref $v;
		$v //= '';
		die "answer too long\n" if length($v) > $MAX_ANSWER;
		$out{$k} = $v;
	}
	return \%out;
}

# ---- WIMS session pool (one anonymous lightpopup session per module; id never leaves the worker) ----
my %SESS;    # module -> {id, uses, t}
my $SESSION_USES = 500;
my $wims_calls = 0;

sub decode_body ($res) {
	my $raw = $res->body // '';
	my $ct = $res->headers->content_type // '';
	return Encode::decode('UTF-8', $raw) if $ct =~ /utf-8/i;
	my $t = eval { Encode::decode('UTF-8', $raw, Encode::FB_CROAK()) };
	return defined $t ? $t : Encode::decode('latin1', $raw);
}
# V1_1: multipart/form-data POST to wims.cgi?form-data (WIMS reads multipart fields only with that query string;
# a urlencoded POST body would be copied into QUERY_STRING and logged by user_error.log / post.log).
my $post_seq = 0;
sub wims_post (@kv) {
	$wims_calls++;
	# unique query string per POST: WIMS (src/variables.c check_session) answers a request whose QUERY_STRING equals
	# the previous one of the session with the previous page (reload protection), so two "?form-data" POSTs in a row
	# (\steps step 2) would get step 1's page again. WIMS's own forms do the same (exec.c: ?form-data<random>).
	my $url = Mojo::URL->new($WIMS)->query('form-data' . $$ . '_' . (++$post_seq) . '_' . int(rand(1e6)));
	my $tx = $ua->post($url => { 'Content-Type' => 'multipart/form-data' } => form => { @kv });
	my $res = $tx->result;    # dies on connection error / timeout; a 302 of p4render is followed with GET
	die "WIMS HTTP " . $res->code . "\n" unless $res->is_success;
	return decode_body($res);
}
sub wims_get (%q) {
	$wims_calls++;
	my $tx = $ua->get(Mojo::URL->new($WIMS)->query(%q));
	my $res = $tx->result;
	die "WIMS HTTP " . $res->code . "\n" unless $res->is_success;
	return decode_body($res);
}
sub page_sid ($html) {
	my ($sid) = $html =~ /name="session" value="([A-Z0-9]+)(?:\.\d+)?"/;
	($sid) = $html =~ /session=([A-Z0-9]{6,})/ unless $sid;
	return $sid;
}
sub new_session ($module, $exo, $seed, $lang) {
	my $html = wims_post(module => 'adm/raw', job => 'p4render', p4_auth => $WIMS_TOKEN, emod => $module, lang => $lang,
		parm => "cmd=new;exo=$exo;qnum=1;seed=$seed;useropts=32");
	die "WIMS refused the P4 secret\n" if $html =~ /p4_auth_refused|p4_bad_module/;
	my $sid = page_sid($html);
	die "WIMS did not open a session\n" unless $sid;
	$SESS{$module} = { id => $sid, uses => 0, t => time };
	return $html;
}
sub wims_new ($module, $exo, $seed, $lang) {
	my $s = $SESS{$module};
	if (!$s || $s->{uses} >= $SESSION_USES) { return new_session($module, $exo, $seed, $lang) }
	$s->{uses}++;
	# cmd=new stays a GET: WIMS applies exo/qnum/seed of a new exercise from the query string only (a multipart
	# cmd=new ignores the seed). Nothing secret in it (session id, module, exo, seed); answers are POSTed.
	my $html = eval { wims_get(session => $s->{id}, module => $module, cmd => 'new', exo => $exo, qnum => 1,
		seed => $seed, lang => $lang, useropts => '32') };
	# same session still alive (WIMS's cleaner removes idle popup sessions): otherwise open a new authenticated one
	return $html if defined $html && $html =~ /oefstatement|name="replyform"/ && (page_sid($html) // '') eq $s->{id};
	return new_session($module, $exo, $seed, $lang);
}
sub wims_reply ($module, $lang, $answers) {
	my $s = $SESS{$module} or die "no WIMS session\n";
	$s->{uses}++;
	return wims_post(session => $s->{id}, module => $module, cmd => 'reply', special_parm => 'p4_json', lang => $lang,
		%$answers);
}

# ---- HTML extraction ----
# Images: plain HTTP/1.0 read until close. The dev CGI server sends two Content-Length headers for WIMS images
# (CGI's own + its own), which Mojo::UserAgent and HTTP::Tiny reject; Apache does not, this works for both.
use IO::Socket::INET;
sub raw_get ($url) {
	my $s = IO::Socket::INET->new(PeerAddr => $url->host, PeerPort => $url->port, Proto => 'tcp', Timeout => $TIMEOUT)
		or return (0, '', '');
	$s->timeout($TIMEOUT);
	my $pq = $url->path_query;
	print $s "GET $pq HTTP/1.0\r\nHost: " . $url->host_port . "\r\nUser-Agent: P4-OEF-GROK\r\nConnection: close\r\n\r\n";
	my $buf = '';
	local $SIG{ALRM} = sub { die "image timeout\n" };
	alarm $TIMEOUT;
	while (1) { my $n = sysread($s, my $chunk, 65536); last unless $n; $buf .= $chunk; last if length($buf) > 300 * 1024 }
	alarm 0;
	close $s;
	my ($head, $body) = split /\r\n\r\n/, $buf, 2;
	my ($code) = ($head // '') =~ m{^HTTP/\d\.\d (\d{3})};
	my ($ct) = ($head // '') =~ m{^Content-type:\s*([^\r\n;]+)}mi;
	$wims_calls++;
	return ($code // 0, lc($ct // ''), $body // '');
}
sub inline_images ($dom, $sid) {
	$dom->find('img[src]')->each(sub ($img, $n) {
		my $src = $img->attr('src');
		return if $src =~ /^data:image\//;    # already inlined (statement inside the form)
		my $url = Mojo::URL->new($src)->to_abs(Mojo::URL->new($WIMS));
		# wims.gif is the WIMS CGI under another name (Apache alias); the dev CGI server only knows wims.cgi
		$url->path(Mojo::URL->new($WIMS)->path->to_string) if $url->path->to_string =~ m{/wims\.gif$};
		if ($url->host && $url->host =~ /^(127\.0\.0\.1|localhost)$/ && $url->port == Mojo::URL->new($WIMS)->port) {
			my ($code, $ct, $body) = raw_get($url);
			# static files come back as application/octet-stream from the dev server: trust the magic bytes only
			$ct = $body =~ /^GIF8[79]a/ ? 'image/gif' : $body =~ /^\x89PNG/ ? 'image/png' : $body =~ /^\xFF\xD8\xFF/ ? 'image/jpeg'
				: $ct =~ m{^image/svg\+xml} ? $ct : '';
			if ($code == 200 && $ct =~ m{^image/(png|gif|jpeg|svg\+xml)} && length($body) && length($body) <= 256 * 1024) {
				$img->attr(src => "data:$ct;base64," . b64_encode($body, ''));
				return;
			}
		}
		app->log->info(sprintf 'image not inlined: host=%s path=%s', $url->host // '-', $url->path) if $ENV{OEF_DEBUG_IMG};
		$img->attr(src => '');    # never leak a WIMS URL
		$img->attr(alt => '[image]');
	});
}
sub field_type ($el) {
	my $tag = $el->tag;
	return 'menu' if $tag eq 'select' || lc($el->attr('type') // '') eq 'p4menu';
	return lc($el->attr('type') // 'text') eq 'radio' ? 'radio' : lc($el->attr('type') // 'text') eq 'checkbox' ? 'checkbox' : 'text';
}
sub label_html ($form, $el) {
	my $id = $el->attr('id') // return '';
	my $lab = $form->at(qq{label[for="$id"]}) // return '';
	my $h = $lab->content; $h =~ s/^\s+|\s+$//g; return $h;
}
sub extract ($html, $sid) {
	# V1_1 (P13): Mojo::DOM mis-nests <select> (the options and the text after the menu were lost). Each select of a
	# reply/choice field becomes an <input type="p4menu"> placeholder before parsing; its options are read here.
	my %SEL;
	$html =~ s{<select\b([^>]*)>(.*?)</select\s*>}{
		my ($a, $inner) = ($1, $2);
		my ($nm) = $a =~ /\bname="((?:reply|choice)\d+)"/;
		if ($nm) {
			my @opts;
			while ($inner =~ m{<option\b[^>]*\bvalue="([^"]*)"[^>]*>(.*?)(?=<option\b|\z)}sg) {
				my ($v, $l) = ($1, $2); $l =~ s/<[^>]+>//g; $l =~ s/^\s+|\s+$//g;
				push @opts, { value => $v, label => $l } unless $v =~ /^\?+$/;
			}
			$SEL{$nm} = \@opts;
			qq{<input type="p4menu" name="$nm" id="$nm">};
		} else { '' }
	}gsei;
	my $dom = Mojo::DOM->new($html);
	my $form = $dom->at('form#a_oefform') // $dom->at('form[name="replyform"]');
	my $st = $dom->at('div.oefstatement');
	die "no OEF statement in the WIMS page\n" unless $st;
	inline_images($st, $sid);
	inline_images($form, $sid) if $form;
	my (%seen, @fields);
	my @scopes = ($form ? $form : $st);
	for my $el ($scopes[0]->find('input[name], select[name], textarea[name]')->each) {
		my $name = $el->attr('name');
		next unless $name =~ /^(reply|choice)(\d+)$/;
		my $type = field_type($el);
		my $embedded = $st->find(qq{[name="$name"]})->size ? \1 : \0;
		if (!$seen{$name}) {
			$seen{$name} = { id => $name, type => $type, embedded => $embedded };
			push @fields, $seen{$name};
		}
		if ($type eq 'radio' || $type eq 'checkbox') {
			my $v = $el->attr('value') // '';
			next if ($el->attr('id') // '') =~ /_noidea$/;
			push @{ $seen{$name}{choices} }, { value => "$v", label => label_html($form // $st, $el) };
		} elsif ($type eq 'menu') {
			$seen{$name}{choices} = $SEL{$name} // [];
		} else {
			$seen{$name}{size} = 0 + ($el->attr('size') // 10);
		}
	}
	# Embedded fields become placeholders; the WeBWorK macro puts its own answer blanks there.
	for my $f (@fields) {
		my $nodes = $st->find(qq{[name="$f->{id}"]});
		next unless $nodes->size;
		my $first = 1;
		$nodes->each(sub ($n, $i) {
			if ($first) { $n->replace("{{P4:$f->{id}}}"); $first = 0 }
			else        { $n->remove }
		});
		$st->find(qq{label[for^="$f->{id}_"]})->each(sub ($l, $i) { $l->remove });
	}
	$st->find('script')->each(sub ($s, $i) { $s->remove });
	$st->find('input[type="hidden"]')->each(sub ($s, $i) { $s->remove });
	my $out = $st->content;
	$out =~ s/\b[A-Z0-9]{10}\.\d+\b/X/g;           # any WIMS session id left
	$out =~ s{https?://(?:127\.0\.0\.1|localhost)(?::\d+)?/wims[^"'\s>]*}{}g;    # any WIMS URL left
	$out =~ s/\n\s*\n+/\n/g;
	return ($out, \@fields);
}
sub hook_json ($html) {
	my ($j) = $html =~ /P4_JSON_BEGIN(.*?)P4_JSON_END/s;
	return undef unless defined $j;
	$j =~ s/^\s+|\s+$//g;
	my $d = eval { Mojo::JSON::decode_json(Encode::encode('UTF-8', $j)) };
	die "bad p4_json from WIMS\n" unless ref $d eq 'HASH';
	die "WIMS session not authorised\n" if ($d->{error} // '') eq 'auth';
	return $d;
}
sub steps_of ($html) {
	my $d = hook_json($html);
	return $d ? ($d->{step} // 0, $d->{steps} // 0) : (undef, undef);
}


# ---- V1_1: hardcopy text. P4 sessions ask WIMS for MathML (useropts=32: math as <math>, no GIF), so the formulas
# are text; statement_tex is the statement for WeBWorK's TeX hardcopy with MathML converted to TeX math. Images
# that are still present become [figure] (the V1_1 pilot has none). Only the statement data is converted.
my %MO = ("\x{2212}" => '-', "\x{00D7}" => '\times ', "\x{00F7}" => '\div ', "\x{2264}" => '\le ', "\x{2265}" => '\ge ',
	"\x{2260}" => '\neq ', "\x{00B1}" => '\pm ', "\x{22C5}" => '\cdot ', "\x{00B7}" => '\cdot ', "\x{2192}" => '\to ',
	"\x{221E}" => '\infty ', "\x{2026}" => '\ldots ', "\x{2208}" => '\in ', "\x{2248}" => '\approx ', '{' => '\{', '}' => '\}',
	'%' => '\%', '#' => '\#', '&' => '\&', '$' => '\$', '_' => '\_', '^' => '\hat{}', '~' => '\sim ', "\x{2061}" => '',
	"\x{2062}" => '', "\x{00A0}" => '~', "\x{2009}" => '\,', "\x{2223}" => '\mid ', "\x{2261}" => '\equiv ',
	"\x{21D2}" => '\Rightarrow ', "\x{21D4}" => '\Leftrightarrow ', "\x{222A}" => '\cup ', "\x{2229}" => '\cap ',
	"\x{2200}" => '\forall ', "\x{2203}" => '\exists ', "\x{2211}" => '\sum ', "\x{220F}" => '\prod ', "\x{222B}" => '\int ',
	"\x{2218}" => '\circ ', "\x{00B0}" => '^\circ ', "\x{2282}" => '\subset ', "\x{2209}" => '\notin ', "\x{2205}" => '\emptyset ',
	"\x{2032}" => "'", "\x{2227}" => '\wedge ', "\x{2228}" => '\vee ', "\x{00AC}" => '\neg ', "\x{2016}" => '\|',
	"\x{221A}" => '\surd ', "\x{2245}" => '\cong ', "\x{2225}" => '\parallel ', "\x{22A5}" => '\perp ', "\x{2220}" => '\angle ',
	(map { chr(0x3B1 + $_->[0]) => "\\$_->[1] " } [0,'alpha'],[1,'beta'],[2,'gamma'],[3,'delta'],[4,'epsilon'],[6,'eta'],
		[7,'theta'],[8,'iota'],[9,'kappa'],[10,'lambda'],[11,'mu'],[12,'nu'],[13,'xi'],[15,'pi'],[16,'rho'],[18,'sigma'],
		[19,'tau'],[20,'upsilon'],[21,'phi'],[22,'chi'],[23,'psi'],[24,'omega'],[5,'zeta']),
	"\x{0394}" => '\Delta ', "\x{03A3}" => '\Sigma ', "\x{03A9}" => '\Omega ', "\x{03A0}" => '\Pi ', "\x{0393}" => '\Gamma ',
	"\x{211D}" => '\mathbb{R}', "\x{2115}" => '\mathbb{N}', "\x{2124}" => '\mathbb{Z}', "\x{211A}" => '\mathbb{Q}', "\x{2102}" => '\mathbb{C}');
sub math_chars ($t) { join '', map { $MO{$_} // (ord($_) > 127 ? "\\text{$_}" : $_) } split //, $t }
sub tex_text ($t) { $t =~ s/([\\#\$%&_{}~^])/$1 eq '\\' ? '\textbackslash{}' : "\\$1"/ge; return $t }
sub ml_tex ($n) {
	return '' unless defined $n;
	if ($n->type eq 'text' || $n->type eq 'raw') { my $t = $n->content // ''; $t =~ s/^\s+|\s+$//g; return tex_text($t) }
	return '' unless $n->type eq 'tag';
	my $tag = lc $n->tag;
	my @k = grep { $_->type eq 'tag' } @{ $n->child_nodes };
	my $kids = sub { join '', map { ml_tex($_) } @{ $n->child_nodes } };
	my $arg = sub ($i) { '{' . ml_tex($k[$i]) . '}' };
	if ($tag eq 'mn') { my $t = $n->all_text; $t =~ s/\s+//g; return math_chars($t) }
	if ($tag eq 'mi') { my $t = $n->all_text; $t =~ s/^\s+|\s+$//g;
		return length($t) > 1 && $t =~ /^[A-Za-z]+$/ ? '\mathrm{' . $t . '}' : math_chars($t) }
	if ($tag eq 'mtext' || $tag eq 'ms') { my $t = $n->all_text; return '\text{' . tex_text($t) . '}' }
	if ($tag eq 'mo') { my $t = $n->all_text; $t =~ s/^\s+|\s+$//g; return math_chars($t) }
	if ($tag eq 'mspace') { return '\ ' }
	if ($tag eq 'mfrac') { return '\frac' . $arg->(0) . $arg->(1) }
	if ($tag eq 'msup') { return '{' . ml_tex($k[0]) . '}^' . $arg->(1) }
	if ($tag eq 'msub') { return '{' . ml_tex($k[0]) . '}_' . $arg->(1) }
	if ($tag eq 'msubsup') { return '{' . ml_tex($k[0]) . '}_' . $arg->(1) . '^' . $arg->(2) }
	if ($tag eq 'msqrt') { return '\sqrt{' . $kids->() . '}' }
	if ($tag eq 'mroot') { return '\sqrt[' . ml_tex($k[1]) . ']' . $arg->(0) }
	if ($tag eq 'mover') { return '\overset' . $arg->(1) . $arg->(0) }
	if ($tag eq 'munder') { return '\underset' . $arg->(1) . $arg->(0) }
	if ($tag eq 'mfenced') { my ($o, $c) = ($n->attr('open') // '(', $n->attr('close') // ')');
		return '\left' . ($MO{$o} // $o) . join(',', map { ml_tex($_) } @k) . '\right' . ($MO{$c} // $c) }
	if ($tag eq 'mtable') { my @rows = map { join(' & ', map { ml_tex($_) } grep { $_->type eq 'tag' } @{ $_->child_nodes }) }
			grep { $_->type eq 'tag' } @{ $n->child_nodes };
		my $cols = 1; for (@rows) { my $c = 1 + scalar(() = /(?<!\\)&/g); $cols = $c if $c > $cols }
		return '\begin{array}{' . ('c' x $cols) . '}' . join(' \\\\ ', @rows) . '\end{array}' }
	if ($tag eq 'semantics') { return @k ? ml_tex($k[0]) : '' }
	if ($tag eq 'annotation' || $tag eq 'annotation-xml') { return '' }
	return $kids->();    # math, mstyle, mrow, mpadded, mphantom, menclose, ...
}
sub statement_tex ($html) {
	my $dom = Mojo::DOM->new($html);
	$dom->find('math')->each(sub ($m, $i) {
		my $t = ml_tex($m); $t =~ s/\s+/ /g;
		$m->replace('{{P4TEX:' . b64_encode(Encode::encode('UTF-8', $t), '') . '}}');
	});
	$dom->find('img')->each(sub ($m, $i) { $m->replace('{{P4FIG}}') });
	$dom->find('br')->each(sub ($m, $i) { $m->replace("\n\n") });
	$dom->find('p, div, li, tr')->each(sub ($m, $i) { $m->append("\n\n") });
	$dom->find('td, th')->each(sub ($m, $i) { $m->append('  ') });
	my $t = $dom->all_text;
	$t =~ s/\{\{P4:\w+\}\}/ /g;    # no inputs in hardcopy
	$t = tex_text($t);
	$t =~ s/\\\{\\\{P4FIG\\\}\\\}/[figure]/g;
	$t =~ s/\\\{\\\{P4TEX:([A-Za-z0-9+\/=]+)\\\}\\\}/'\\(' . Encode::decode('UTF-8', Mojo::Util::b64_decode($1)) . '\\)'/ge;
	$t =~ s/[ \t]+\n/\n/g; $t =~ s/\n{3,}/\n\n/g; $t =~ s/^\s+|\s+$//g;
	return $t;
}

# V1_1 (LW review item 2): the web statement carries the formulas as TeX too, \(...\) for WeBWorK's MathJax
# (same conversion as the hardcopy; the MathML is not sent).
sub html_mathjax ($html) {
	my $dom = Mojo::DOM->new($html);
	$dom->find('math')->each(sub ($m, $i) {
		my $t = ml_tex($m); $t =~ s/\s+/ /g;
		$m->replace(Mojo::Util::xml_escape('\(' . $t . '\)'));
	});
	return "$dom";
}

# ---- operations (output built from allowlists only) ----
sub op_render ($req) {
	my $module = resolve_module($req->{module});
	my $exo = check_exo_in($module, $req->{exo});
	my $seed = check_seed($req->{seed});
	my $lang = check_lang($req->{lang});
	my $html = wims_new($module, $exo, $seed, $lang);
	my ($st, $fields) = extract($html, $SESS{$module}{id});
	return { ok => \1, seed => $seed, statement_html => html_mathjax($st), statement_tex => statement_tex($st), fields => clean_fields($fields),
		step => 1 };
}
sub clean_fields ($fields) {
	return [ map {
		my $f = $_;
		my %o = (id => $f->{id}, type => $f->{type}, embedded => $f->{embedded});
		$o{size} = $f->{size} if defined $f->{size};
		$o{choices} = [ map { +{ value => $_->{value}, label => $_->{label} } } @{ $f->{choices} } ] if $f->{choices};
		\%o;
	} @$fields ];
}
sub clean_per_field ($pf) {
	return [ map { { id => "$_->{id}", correct => ($_->{correct} ? \1 : \0), score => 0 + ($_->{score} // 0),
		feedback => '' } } grep { ref $_ eq 'HASH' && ($_->{id} // '') =~ /^(reply|choice)\d+$/ } @{ $pf // [] } ];
}
# ---- P6: decimal comma ----
# WIMS numexp/default/algexp fields reject "6,22" (kind=input). French students type a decimal comma, so for those
# answer types only, a comma between two digits becomes a dot. Types where a comma separates items (set, fset, aset,
# list, range, menu, clickfill, correspond, nocase, case, ...) are never touched. The type of replyK is the type of
# the K-th \answer of the exercise source (no type = OEF default type).
my %NUMERIC_TYPE = map { $_ => 1 } qw(default numeric numexp algexp formal litexp);
my %TYPES_CACHE;
sub answer_types ($module, $exo) {
	return $TYPES_CACHE{"$module/$exo"} if $TYPES_CACHE{"$module/$exo"};
	my @types;
	for my $r (@ROOTS) {
		my $f = "$r/$module/src/$exo.oef";
		next unless -f $f;
		open(my $fh, '<:raw', $f) or last;
		local $/; my $src = <$fh>; close $fh;
		$src =~ s/^\s*%%.*$//mg;
		while ($src =~ /\\answer\s*\{/g) {
			my $pos = pos($src) - 1;
			my @g;
			for (1 .. 3) {
				last unless substr($src, $pos, 1) eq '{';
				my ($depth, $i) = (0, $pos);
				for (; $i < length $src; $i++) {
					my $ch = substr($src, $i, 1);
					$depth++ if $ch eq '{';
					$depth-- if $ch eq '}';
					last if $depth == 0;
				}
				push @g, substr($src, $pos + 1, $i - $pos - 1);
				$pos = $i + 1;
				$pos++ while substr($src, $pos, 1) =~ /[ \t]/;
			}
			my $t = (@g >= 3 && $g[2] =~ /type\s*=\s*([a-z]+)/i) ? lc $1 : 'default';
			push @types, $t;
		}
		last;
	}
	return $TYPES_CACHE{"$module/$exo"} = \@types;
}
sub normalize_comma ($module, $exo, $id, $v) {
	return $v unless defined $v && $id =~ /^reply(\d+)$/;
	my $k = $1;
	return $v unless $v =~ /\d,\d/;
	my $t = answer_types($module, $exo)->[$k - 1] // 'default';
	return $v unless $NUMERIC_TYPE{$t};
	return $v if $t =~ /^(algexp|formal|litexp)$/ && $v =~ /[()]/;    # f(1,2): leave function arguments alone
	(my $n = $v) =~ s/(?<=\d),(?=\d)/./g;
	return $n;
}

sub op_check ($req) {
	my $module = resolve_module($req->{module});
	my $exo = check_exo_in($module, $req->{exo});
	my $seed = check_seed($req->{seed});
	my $lang = check_lang($req->{lang});
	# answers: {reply1:..} by WIMS id, or answers_ordered: [..] by position (WeBWorK blank order AnSwEr0001..),
	# mapped to the fields in the order they are met (earlier \steps first).
	my $ordered = $req->{answers_ordered};
	die "answers_ordered must be an array\n" if defined $ordered && ref $ordered ne 'ARRAY';
	die "too many answers\n" if $ordered && @$ordered > $MAX_FIELDS;
	my $answers = check_answers($ordered ? {} : $req->{answers});
	my $html = wims_new($module, $exo, $seed, $lang);
	my ($cur_st, $fields) = extract($html, $SESS{$module}{id});
	my @all = @$fields;    # every field met so far, in order (earlier steps first)
	my $assign = sub {
		return unless $ordered;
		for my $i (0 .. $#all) {
			my $v = $ordered->[$i] // '';
			die "bad answer value\n" if ref $v;
			die "answer too long\n" if length($v) > $MAX_ANSWER;
			$answers->{ $all[$i]{id} } //= $v;
		}
	};
	$assign->();
	for my $round (1 .. 20) {
		my %send = map { $_->{id} => normalize_comma($module, $exo, $_->{id}, $answers->{ $_->{id} } // '') } @$fields;
		my $out = wims_reply($module, $lang, \%send);
		my $d = hook_json($out) or die "WIMS returned no p4_json block\n";
		my %res = (ok => \1, seed => $seed, step => 0 + ($d->{step} // 0), steps => 0 + ($d->{steps} // 0));
		my $kind = $d->{kind} // '';
		if ($kind eq 'final') {
			return { %res, done => \1, score => 0 + ($d->{grade} // 0), per_field => clean_per_field($d->{per_field}),
				error => undef, view => { statement_html => html_mathjax($cur_st), statement_tex => statement_tex($cur_st), fields => clean_fields(\@all) } };
		}
		if ($kind eq 'input') {
			return { %res, done => \0, score => 0, per_field => [], error => 'input',
				view => { statement_html => html_mathjax($cur_st), statement_tex => statement_tex($cur_st), fields => clean_fields(\@all) } };
		}
		# intermediate \steps step passed
		my ($st, $nf) = extract($out, $SESS{$module}{id});
		my %have = map { $_->{id} => 1 } @all;
		my @new = grep { !$have{ $_->{id} } } @$nf;
		push @all, @new;
		$assign->();
		$cur_st = $st;
		if (!@new || !grep { length($answers->{ $_->{id} } // '') } @new) {
			return { %res, done => \0, score => 0, per_field => [], error => undef,
				view => { statement_html => html_mathjax($cur_st), statement_tex => statement_tex($cur_st), fields => clean_fields(\@all) } };
		}
		$fields = \@new;
	}
	die "too many steps\n";
}

# ---- HTTP ----
my $served = 0;
my $recycling = 0;
my @hits;
app->max_request_size(64 * 1024);
app->log->level('info');
app->secrets([ Mojo::Util::sha1_sum($TOKEN . $$ . time) ]);

hook before_dispatch => sub ($c) {
	my $h = $c->req->headers;
	my $deny = sub ($status, $msg) {
		app->log->info(sprintf 'deny %s %s %d %s', $c->req->method, $c->req->url->path, $status, $msg);
		$c->render(json => { ok => \0, error => $msg }, status => $status);
	};
	return $deny->(403, 'bad Host') unless lc($h->host // '') eq lc $HOSTPORT;
	return $deny->(403, 'bad Origin') if defined $h->origin && $h->origin ne $ORIGIN;
	return $deny->(403, 'missing or wrong X-LumaPlus-Token') unless secure_compare($h->header('X-LumaPlus-Token') // '', $TOKEN);
	return $deny->(413, 'body larger than 64 KiB') if $c->req->is_limit_exceeded;
	if ($c->req->method eq 'POST') {
		return $deny->(415, 'Content-Type must be application/json')
			unless ($h->content_type // '') =~ m{^application/json\s*(;|\z)}i;
	}
	my $now = time;
	@hits = grep { $_ > $now - 60 } @hits;
	return $deny->(429, 'rate limit') if @hits >= $RATE;
	push @hits, $now;
};

get '/oef/ping' => sub ($c) {
	$c->render(json => { ok => \1, engine => 'WIMS OEF via p4_json', pid => $$, served => $served,
		max_requests => 0 + $MAX_REQUESTS, wims_calls => $wims_calls, sessions => scalar(keys %SESS) });
};

for my $route ([ '/oef/render', \&op_render ], [ '/oef/check', \&op_check ]) {
	my ($path, $op) = @$route;
	post $path => sub ($c) {
		++$served;
		my $t0 = time;
		my $req = $c->req->json;
		return $c->render(json => { ok => \0, error => 'body must be a JSON object' }, status => 400)
			unless ref $req eq 'HASH';
		my $res = eval { $op->($req) };
		if (!$res && ($@ // '') =~ /session not authorised/) {    # pooled session lost its mark: one retry, new session
			delete $SESS{ $req->{module} // '' };
			$res = eval { $op->($req) };
		}
		my $ms = int(1000 * (time - $t0));
		my $tag = sprintf '%s module=%s exo=%s seed=%s', $path, map { my $v = $req->{$_} // ''; $v =~ s/[^\w\/.-]//g; substr($v, 0, 80) } qw(module exo seed);
		if (my $err = $@) {
			$err =~ s/\s+\z//;
			$err = 'WIMS unreachable or timed out' if $err =~ /connect|timeout|Premature|Connection refused/i;
			app->log->info("$tag status=400 ms=$ms err=$err");
			return $c->render(json => { ok => \0, error => $err }, status => 400);
		}
		$res->{ms} = $ms;
		app->log->info("$tag status=200 ms=$ms" . (exists $res->{score} ? " done=" . (${ $res->{done} } ? 1 : 0) : ''));
		$c->render(json => $res);
	};
}

any '/*whatever' => { whatever => '' } => sub ($c) { $c->render(json => { ok => \0, error => 'not found' }, status => 404) };

# Pre-forking server: the manager keeps the listening socket; each worker process accepts OEF_MAX_REQUESTS
# connections (one request per connection: clients send Connection: close) and is then replaced gracefully, so the
# recycling leaves no gap (no refused or reset connection; gate P10b). OEF_WORKERS processes (default 1).
my $WORKERS = ($ENV{OEF_WORKERS} // '') =~ /^([1-8])$/ ? $1 : 1;
my $PIDFILE = $ENV{OEF_PREFORK_PIDFILE} || File::Spec->catfile(File::Spec->tmpdir, "oef_eval_prefork_" . ($HOSTPORT =~ s/\W/_/gr) . ".pid");
app->start('prefork', '-l', "http://$HOSTPORT", '-w', $WORKERS, '-a', 0 + $MAX_REQUESTS, '-P', $PIDFILE);
