oef_llm_fix.py — correction des fichiers OEF par un LLM local (Ollama)
=====================================================================

PRINCIPE
  1. oef2wims-v2.exe --json compile le fichier et écrit nom.json (erreurs avec ligne,
     colonne, explication et correction proposée).
  2. Le script construit un prompt en français : source numéroté + points « À CORRIGER »
     + consignes strictes (ne corriger que ces points, ne rien changer d'autre, rendre
     le fichier complet entre ```oef et ```).
  3. Le LLM répond ; le script contrôle la réponse (voir SÉCURITÉ), écrit
     nom.corrige.oef, le recompile avec oef2wims-v2.exe, et recommence avec les erreurs
     restantes (3 essais par défaut).
  Le fichier d'origine n'est JAMAIS modifié. Relisez toujours le diff avant d'utiliser
  un .corrige.oef : un LLM peut produire un fichier qui compile mais change le sens
  de l'exercice.

INSTALLATION (Windows)
  - Python 3.12 (python.org, cocher « Add to PATH »). Aucun module à installer.
  - Ollama (ollama.com), puis par exemple :  ollama pull llama3.1:8b-instruct-q5_K_M
  - Placez le dossier llm\ dans le dossier oef2wims-win (à côté de oef2wims-v2.exe,
    oef2wims.exe, oef2wims-trace.exe). Sinon indiquez --exe.

UTILISATION (PowerShell)
  cd <dossier oef2wims-win>\llm
  python .\oef_llm_fix.py D:\mes_oef\exo.oef
  python .\oef_llm_fix.py D:\mes_oef                                  # tout un dossier
  python .\oef_llm_fix.py D:\mes_oef --model qwen2.5:14b-instruct --max-iter 2
  python .\oef_llm_fix.py D:\mes_oef\exo.oef --model deepseek-r1:32b-qwen-distill-q4_K_M --num-ctx 16384 --timeout 1200
  python .\oef_llm_fix.py D:\mes_oef\exo.oef --dry-run                # affiche le prompt, sans LLM
  python .\oef_llm_fix.py D:\mes_oef --warnings                       # corrige aussi les avertissements
  python .\oef_llm_fix.py --help
  Options : --exe, --provider (ollama|openai), --model, --base-url, --num-ctx (8192),
            --temperature (0.1), --max-iter (3), --max-change (30 %), --min-lines (3),
            --warnings, --timeout (600 s), --dry-run, --runner (tests seulement).

FICHIERS PRODUITS (à côté de chaque .oef)
  nom.corrige.oef   dernière proposition acceptée (encodée en UTF-8)
  nom.corrige.log/.json/.def   résultat de la recompilation par oef2wims-v2.exe
  nom.llm.log       prompts, réponses brutes, diff de chaque itération, diff final
                    original -> corrigé, alertes, verdict final
  llm-rapport.html / llm-rapport.json  (dans le dossier traité) : fichier, erreurs
                    initiales, itérations, statut final, alertes, durée, modèle.
                    Attention : un nouveau lancement sur le même dossier remplace ce rapport.
  Statuts : OK | OK (rien à corriger) | TOUJOURS EN ERREUR (le .corrige.oef compile
  encore avec erreurs) | ÉCHEC (aucune proposition acceptée) | ÉCHEC (LLM injoignable).

SÉCURITÉ (une proposition est REJETÉE, et signalée dans le rapport, si)
  - elle ne contient pas de bloc ```oef ... ``` (texte libre, refus, réponse vide) ;
  - elle modifie plus de --max-change % des lignes (au moins --min-lines lignes sont
    toujours tolérées, pour les petits fichiers) ;
  - elle supprime une directive (\title, \statement, \answer, ...). Exception : fusion
    autorisée quand l'erreur est « deux \statement » ou « deux \steps ».
  Les blocs <think>...</think> (deepseek-r1) sont retirés avant l'analyse ; les numéros
  de ligne recopiés par erreur sont retirés ; les fins de ligne CRLF d'origine sont
  conservées.

MODÈLES CONSEILLÉS (exemples Ollama publics ; adaptez à vos modèles)
  - qwen2.5:14b-instruct : bon compromis vitesse/respect des consignes ; à essayer en premier.
  - un modèle ~32B spécialisé maths (ex. qwen2.5:32b-instruct) : plus précis sur le contenu mathématique, plus lent.
  - deepseek-r1:32b-qwen-distill-q4_K_M : raisonne longuement (<think>) ; prévoir
    --timeout 1200 et --num-ctx 16384 ; utile pour les cas difficiles.
  - llama3.1:8b-instruct-q5_K_M (défaut) : rapide, suffisant pour accolades,
    fautes de frappe de directive, \statement manquant.
  - modèles très quantifiés (Q2) : qualité incertaine pour une recopie exacte ;
    à tester avec --dry-run puis sur quelques fichiers.
  Pour un fichier long, augmentez --num-ctx (le prompt contient le fichier deux fois
  au total avec la réponse : comptez environ 2 x la taille du fichier en tokens + 1000).

AJOUTER UN AUTRE LLM
  - Serveur compatible OpenAI (LM Studio, llama.cpp server, vLLM, OpenRouter, OpenAI) :
      $env:OEF_LLM_API_KEY = "sk-..."          # facultatif en local
      python .\oef_llm_fix.py exo.oef --provider openai --base-url http://localhost:1234/v1 --model nom-du-modele
    (ou $env:OEF_LLM_BASE_URL au lieu de --base-url)
  - Autre API : dans oef_llm_fix.py, créez une classe héritant de Provider avec une
    méthode chat(self, messages) qui renvoie le texte de la réponse (messages = liste
    de {"role": "system"|"user", "content": ...}), puis ajoutez-la au dictionnaire
    PROVIDERS, par ex.  PROVIDERS["mistral"] = MistralProvider. Elle devient
    disponible avec --provider mistral.

SCHÉMA JSON LU (nom.json produit par oef2wims-v2.exe --json)
  fichier, verdict ("OK" | "OK (avertissements)" | "ERREUR"), code_retour, duree_ms,
  message_compilateur, def, erreurs, avertissements, infos,
  [validateur {lance, verdict, code}], [diff {lignes_retirees, lignes_ajoutees}],
  problemes : liste de {severite ("ERREUR"|"AVERTISSEMENT"|"INFO"), source
  ("compilateur"|"pré-contrôle"|"validateur"), code (PAREN, NOSTMT, STMT2, STEPS2,
  PROMPT, EMPTY, ...), ligne, colonne (0 = sans position), [ligne2, colonne2],
  message, explication, correction}. Détail complet en tête de oef_llm_fix.py.

TESTS (sur le serveur de préparation, sans vrai modèle)
  test\mock_ollama.py imite /api/chat. Avec 9 fichiers d'exemple : 5 corrigés (dont
  un en 2 essais et un au format deepseek <think>), 1 sans erreur, 3 rejetés comme
  prévu (réécriture complète, directive supprimée, réponse sans bloc oef) ;
  serveur injoignable et --dry-run vérifiés.
