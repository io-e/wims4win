oef2html - Énoncé + correction d'un exercice OEF en page HTML, via le VRAI moteur WIMS
===================================================================================

PRINCIPE
  Le script n'imite pas WIMS. Il fait exécuter l'exercice par wims.cgi 4.31b, compilé
  sans modification depuis wims_4.31b+dfsg1.orig.tar.gz :
   1. le .oef est compilé en .def par oef2wims (ou le .def est pris tel quel) ;
   2. le .def est déposé dans un module OEF local (modules/H6/test/oef2html.fr), copié
      du gabarit officiel modules/template/oef.fr ;
   3. la graine est fixée par "!setseed N" dans var.proc du module. Ce mécanisme est
      prévu par le gabarit WIMS : même graine = même tirage ;
   4. wims.cgi est lancé en ligne de commande comme un vrai CGI (REQUEST_METHOD,
      QUERY_STRING...). L'appel cmd=new produit l'énoncé ;
   5. l'appel cmd=reply envoie une réponse "test" fausse. Le moteur affiche alors
      lui-même la bonne réponse, la \solution et les \feedback. Pour les menus ou les
      choix où WIMS n'affiche pas la bonne réponse, le script fait noter chaque option
      par le moteur et garde celle qu'il déclare juste ;
   6. le script extrait ces fragments HTML (formules en MathML produites par
      wims_mathml, images flydraw/draw intégrées en base64, canvasdraw intégré) et
      remplit le gabarit.

OÙ ÇA TOURNE
  - Linux (box, serveur, VM, Docker) : OUI, testé.
  - Windows natif : NON pour l'instant. Les sources 4.31b ne compilent pas avec
    mingw-w64 : il manque sys/wait.h, sys/socket.h, et le moteur utilise fork, execv,
    waitpid, pipe, alarm/SIGALRM, setrlimit et un socket Unix (wimslogd). Les
    calculateurs (pari, maxima) et flydraw sont lancés par fork. Un portage
    demanderait de réécrire la couche processus, ce qui viole la règle « ne pas
    toucher au code ». Le portage expérimental wims4win (4.33a modifié) démarre
    sous Wine, mais il ne s'agit plus du code upstream ; il n'est pas utilisé ici.
  - Sous Windows, il faut donc lancer le moteur sur une machine Linux, ou avec Docker
    Desktop (voir Dockerfile ; non testé, Docker est absent de la box).

INSTALLATION (Linux)
  sudo apt install build-essential autoconf flex bison libgd-dev pari-gp maxima python3
  sh build_wims_runtime.sh wims_4.31b+dfsg1.orig.tar.gz wims-runtime     (~30 s)
  -> wims.cgi, wimslogd, oef2wims, wims_mathml, interfaces pari/maxima, flydraw, canvasdraw
     Le fichier log/wims.conf ne reçoit que 3 lignes de configuration : formules en
     MathML (default_texposition=2), et désactivation de l'anti-surcharge
     (threshold1/2) pour les traitements par lots.

UTILISATION
  python3 oef2html.py [options] fichier.oef|fichier.def|dossier ...
    --wims-home DIR    arbre WIMS (défaut ./wims-runtime ou variable WIMS_HOME)
    --oef2wims EXE     compilateur (défaut : celui de l'arbre WIMS)
    --template F       gabarit HTML (défaut template/modele.html)
    --css F            feuille de style (défaut : style.css à côté du gabarit)
    --seed N           graine du premier tirage (défaut 1)
    --variants K       K tirages : graines N .. N+K-1, un fichier par tirage
    --out DIR          dossier de sortie (défaut html_out)
    --mathjax on|off   charge MathJax (rendu MathML pour vieux navigateurs ; défaut on)
    --no-solution      fiche élève : énoncé seul
    --json             écrit aussi les fragments extraits (.json)
    --lang fr          langue de l'interface WIMS
  Sortie : <nom>_s<graine>.html + oef2html_rapport.json (statut par fichier et tous les
  appels wims.cgi : requête, code retour, durée, stderr).
  Exemples :
    python3 oef2html.py exo.oef
    python3 oef2html.py mes_oef/ --seed 10 --variants 5 --out tirages
    python3 oef2html.py exo.oef --no-solution --out fiches_eleves
    python3 oef2html.py exo.oef --template mon_modele.html --css mon_style.css

GABARIT (template/modele.html, UTF-8) - variables {{...}}
  {{title}}      titre de l'exercice (\title)
  {{source}}     nom du fichier
  {{seed}}       graine du tirage
  {{statement}}  énoncé tel que produit par WIMS (zones de saisie remplacées par des blancs)
  {{form}}       zone de réponse hors énoncé (étiquettes, choix QCM ○/□, menus [a | b])
  {{answers}}    tableau des bonnes réponses (nom de la réponse -> valeur donnée par WIMS)
  {{solution}}   contenu de \solution (vide si l'exercice n'en a pas)
  {{feedback}}   \feedback déclenchés par la réponse test (souvent vide)
  {{css}}        contenu de la feuille de style
  {{mathjax}}    balises MathJax (vide avec --mathjax off)
  {{date}} {{engine}} {{correction_class}} ("hidden" avec --no-solution)
  Le gabarit fourni : style sobre, imprimable (la correction commence sur une nouvelle
  page), libellés en français.

RÉSULTATS DES TESTS (box Linux, 25/10/2026)
  - 15 exemples (numeric, radio, checkbox, menu, formal, case, clickfill, dragfill,
    numexp, matrix/set, draw, canvasdraw) : 15/15 pages produites, avec les bonnes
    réponses ; dossier exemples/.
  - Corpus de 1 438 fichiers (graine 1, ~2 min 15) : 1 354 pages, 84 échecs :
      6  erreurs oef2wims (fichiers de test volontairement faux) ;
      13 "modèles" Createxo, qui ne sont pas des exercices ;
      65 erreurs signalées par le moteur WIMS lui-même : 58 too_many_gotos,
         6 bad_index, 1 for_syntax. Elles dépendent du tirage : 00347 et 00352 (issus
         de xml2oef) échouent avec la graine 1 et passent avec les graines 2 et 3.
         C'est un vrai défaut à corriger dans ces exercices (boucle \while trop longue).
  - Bonne(s) réponse(s) trouvées dans 829 pages. Les autres exercices notent par
    \condition sans "bonne réponse" affichable, ou n'ont pas de champ de réponse.

LIMITES CONNUES
  - Les feedbacks correspondent à la réponse test envoyée, pas à la bonne réponse.
  - Les images TeX (texgif) ne sont pas utilisées : les formules sont en MathML.
  - Les exercices qui dépendent d'une classe, de données externes ou de logiciels
    absents (octave, gap, povray...) peuvent échouer.
