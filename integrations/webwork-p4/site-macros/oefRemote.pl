################################################################################
# oefRemote.pl  (GROK_V1_1, P4)  WIMS OEF exercises inside WeBWorK.
#   V1_1: worker down -> "Service de correction indisponible / grading service unavailable" on the page and in
#   every answer message (score 0, error_flag OEF_WORKER; the P4 sets have unlimited attempts, gate P5b);
#   hardcopy uses the worker's statement_tex (WIMS MathML converted to TeX), so formulas print as math.
#   WeBWorK keeps the only gradebook; WIMS renders and grades through the OEF worker, reached only through the
#   trusted module WeBWorK::OEFBridge (site config URL + token, never from the problem).
#
#   loadMacros('PGstandard.pl', 'oefRemote.pl');
#   $oef = OEF('H3/analysis/oeflinf.fr', 'antecedant');
#   TEXT($oef->statement);     # statement with the answer blanks (embedded or listed after it)
#   $oef->ANS;                 # one evaluator per blank (per-blank grading, one cached check per submission)
#
# Seed: $problemSeed. Blanks AnSwEr0001.. map in order to the WIMS fields (reply1.., choice1..).
# No correct answer is ever known on this side: correct_ans is always empty, "show correct answers" shows nothing.
# Worker down: each evaluator returns score 0 with an error message and error_flag OEF_WORKER, so it can be told
# apart from a wrong answer.
################################################################################
sub _oefRemote_init { }

sub OEF { my ($module, $exo, %opt) = @_; return OEFRemote->new($module, $exo, %opt) }

package OEFRemote;

our $UNAVAILABLE = 'Service de correction indisponible / grading service unavailable.';

sub new {
	my ($class, $module, $exo, %opt) = @_;
	my $self = bless { module => $module, exo => $exo, seed => $main::problemSeed, lang => $opt{lang} // 'fr',
		names => [], fields => [] }, $class;
	# Answers already typed for this problem (sticky inputs). Names are AnSwEr0001.. in creation order.
	my %typed;
	for my $i (1 .. 40) {
		my $n = sprintf('AnSwEr%04d', $i);
		$typed{$i} = $main::inputs_ref->{$n} if defined $main::inputs_ref->{$n} && $main::inputs_ref->{$n} ne '';
	}
	$self->{typed} = \%typed;
	my $view;
	if (%typed) {
		# One cached check per submission, answers in blank order (the worker maps them to the WIMS fields).
		my $n = 0;
		for my $k (keys %typed) { $n = $k if $k > $n }    # (sort is masked in Safe)
		my $c = WeBWorK::OEFBridge::oef_check($module, $exo, $self->{seed}, [ map { $typed{$_} // '' } 1 .. $n ],
			$self->{lang});
		return $self->_fail($c) unless $c->{ok};
		$self->{check} = $c;
		$view = $c->{view};
	} else {
		$view = WeBWorK::OEFBridge::oef_render($module, $exo, $self->{seed}, $self->{lang});
		return $self->_fail($view) unless $view->{ok};
	}
	$self->{statement_html} = $view->{statement_html} // '';
	$self->{statement_tex} = $view->{statement_tex};
	$self->{fields} = $view->{fields} // [];
	return $self;
}

sub _fail {
	my ($self, $r) = @_;
	$self->{error} = $r->{error} // 'OEF error';
	$self->{transport} = $r->{transport} ? 1 : 0;
	# Answers were typed: keep one blank per typed answer so the student's input is kept and each blank gets the
	# error message (score 0, error_flag OEF_WORKER) instead of disappearing.
	my $n = 0;
	for my $k (keys %{ $self->{typed} // {} }) { $n = $k if $k > $n }
	$self->{fields} = [ map { +{ id => "blank$_", type => 'text', size => 12 } } 1 .. $n ];
	return $self;
}

sub _tex_escape {
	my $t = shift;
	$t =~ s/<img[^>]*>/[figure]/gi;
	$t =~ s/\{\{P4:\w+\}\}/\\underline{\\hspace{2cm}}/g if 0;
	$t =~ s/\{\{P4:\w+\}\}/ /g;            # hardcopy: no inputs
	$t =~ s/<br\s*\/?>|<\/p>|<\/div>|<\/li>/\n\n/gi;
	$t =~ s/<[^>]+>//g;
	$t =~ s/&nbsp;/ /g; $t =~ s/&#39;/'/g; $t =~ s/&quot;/"/g; $t =~ s/&lt;/</g; $t =~ s/&gt;/>/g; $t =~ s/&amp;/&/g;
	$t =~ s/([\\#\$%&_{}~^])/\\$1/g;
	$t =~ s/\n{3,}/\n\n/g;
	return $t;
}

# Statement with blanks. HTML: embedded blanks replace their {{P4:id}} placeholder, the others follow the text.
sub statement {
	my $self = shift;
	if ($self->{error}) {
		my $m = $self->{transport} ? $UNAVAILABLE : "Erreur OEF : $self->{error}";
		my $alert = qq{<div class="alert alert-danger" data-oef-error="1">$m</div>};
		return main::MODES(HTML => $alert, TeX => "[OEF: $m]") unless @{ $self->{fields} };
		return main::MODES(HTML => $alert . join('', map {
			my $name = main::NEW_ANS_NAME(); main::RECORD_IMPLICIT_ANS_NAME($name); push @{ $self->{names} }, $name;
			qq{<div class="oef-blank" style="margin:.4em 0">} . main::NAMED_ANS_RULE($name, 12) . '</div>' } @{ $self->{fields} }),
			TeX => "[OEF: $m]");
	}
	if ($main::displayMode eq 'TeX') {
		# no inputs in hardcopy; statement_tex: formulas as TeX math (worker V1_1), else the V1 HTML strip
		return $self->{statement_tex} if defined $self->{statement_tex} && length $self->{statement_tex};
		return _tex_escape($self->{statement_html});
	}
	my $html = $self->{statement_html};
	my @after;
	my $i = 0;
	for my $f (@{ $self->{fields} }) {
		$i++;
		my $name = main::NEW_ANS_NAME();
		main::RECORD_IMPLICIT_ANS_NAME($name);
		push @{ $self->{names} }, $name;
		my $rule;
		if (($f->{type} // '') eq 'radio' || ($f->{type} // '') eq 'menu') {
			my @c = map { ($_->{value} => $_->{label}) } @{ $f->{choices} // [] };
			my @buttons = main::NAMED_ANS_RADIO_BUTTONS($name, @c);
			$rule = qq{<div class="radio-buttons-container" data-feedback-insert-element="$name" }
				. qq{data-feedback-insert-method="append_content">} . join(' ', @buttons) . '</div>';
		} else {
			$rule = main::NAMED_ANS_RULE($name, $f->{size} || 12);
		}
		if ($html =~ /\{\{P4:\Q$f->{id}\E\}\}/) { $html =~ s/\{\{P4:\Q$f->{id}\E\}\}/$rule/ }
		else { push @after, qq{<div class="oef-blank" style="margin:.4em 0">$rule</div>} }
	}
	$html =~ s/\{\{P4:\w+\}\}//g;
	my $c = $self->{check};
	my $note = '';
	if ($c && !$c->{done}) {
		$note = ($c->{error} // '') eq 'input'
			? '<div class="alert alert-warning">Réponse incomplète ou non reconnue par WIMS : corrigez puis soumettez.</div>'
			: "<div class=\"alert alert-info\">Étape $c->{step}/$c->{steps} : l'étape précédente est validée, continuez.</div>";
	}
	return qq{<div class="oef-statement">$html</div>} . join('', @after) . $note;
}

sub _evaluator {
	my ($self, $idx) = @_;
	return sub {
		my $student = shift // '';
		my $c = $self->{check};
		my %h = (score => 0, correct_ans => '', student_ans => $student, original_student_ans => $student,
			preview_text_string => $student, preview_latex_string => '', ans_message => '', type => 'OEF');
		my $flag = '';
		if ($self->{error}) {
			$h{ans_message} = $self->{transport} ? "$UNAVAILABLE Réponse non corrigée, aucune note enregistrée / answer not graded, no score recorded." : "Erreur OEF : $self->{error}";
			$flag = 'OEF_WORKER';
		} elsif (!$c) {
			# view without answers: nothing to grade
		} elsif (!$c->{done}) {
			$h{ans_message} = ($c->{error} // '') eq 'input' ? 'Réponse non reconnue par WIMS.' : 'Étape validée : continuez.';
		} else {
			my $id = $self->{fields}[$idx]{id};
			my ($pf) = grep { $_->{id} eq $id } @{ $c->{per_field} // [] };
			$h{score} = $pf ? 0 + $pf->{score} : 0;
			$h{score} = 1 if $h{score} > 1;
			$h{score} = 0 if $h{score} < 0;
		}
		my $rh = AnswerHash->new(%h);
		$rh->{error_flag} = $flag if $flag;
		return $rh;
	};
}

# Register one evaluator per blank, in blank order.
sub ANS {
	my $self = shift;
	return if $main::displayMode eq 'TeX';
	my @ev = map { $self->_evaluator($_) } 0 .. $#{ $self->{names} };
	main::ANS(@ev) if @ev;
	return;
}

package main;
1;
