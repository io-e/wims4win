<!-- CLAUDE_V1 -->
# AlgoGebra JSON hook — corpus review (Windows runtime)

| exo | types | W marker | JSON | n==def | grade==html | correct==html | given==sent | ASCII | replay 1.0 | replay ;-trim | no leak | page== | findings |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 00002 | default | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | **FAIL** | page differs outside the JSON block: line 461:
  W: 
  N: &lt;div class=&quot;wims_widget_box clearall&quot;&gt; |
| 00004 | radio | ok | ok | ok | ok | ok | ok | ok | **FAIL** | **FAIL** | ok | **FAIL** | replaying expected: engine rejected the answer, no hook block: La réponse
  2;blu e verde,blu e arancio,blu e nero,verde e arancio,verde e nero,arancio e nero que vous avez donnée
  n&#x27;est pas compréhensible. Veuillez corrig<br>after cutting expected at &#x27;;&#x27;: still not 1.0: La réponse
  2 que vous avez donnée
  n&#x27;est pas compréhensible. Veuillez corriger l&#x27;erreur et soumettre de nouveau la réponse.<br>page differs outside the JSON block: line 449:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00008 | menu | ok | ok | ok | ok | ok | ok | ok | **FAIL** | **FAIL** | ok | **FAIL** | replaying expected: engine rejected the answer, no hook block: La réponse
  2;vera,falsa que vous avez donnée
  n&#x27;est pas compréhensible. Veuillez corriger l&#x27;erreur et soumettre de nouveau la réponse.<br>after cutting expected at &#x27;;&#x27;: still not 1.0: La réponse
  2 que vous avez donnée
  n&#x27;est pas compréhensible. Veuillez corriger l&#x27;erreur et soumettre de nouveau la réponse.<br>page differs outside the JSON block: line 441:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00014 | clickfill | ok | ok | ok | ok | ? | ok | ok | **FAIL** | ok | ok | **FAIL** | replaying expected does not grade 1.0 (grade=0.5): [(&#x27;reply1&#x27;, &#x27;a,n,g,l,e;a,b,c,d,e&#x27;, &#x27;a,n,g,l,e;a,b,c,d,e&#x27;)]<br>after cutting expected at &#x27;;&#x27;: grades 1.0<br>page differs outside the JSON block: line 498:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00021 | numexp | ok | ok | ok | ok | ? | ok | ok | **FAIL** | - | ok | **FAIL** | html analysis rows=4 vs json answers=5: the hook reports answers the engine did not grade (step not reached / empty replygood)<br>replaying expected does not grade 1.0 (grade=1): [(&#x27;reply5&#x27;, &#x27;&#x27;, &#x27;&#x27;)]<br>page differs outside the JSON block: line 493:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00027 | - | ? | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | exception: RuntimeError(&#x27;no session in cmd=new: ?&#x27;) |
| 00045 | checkbox | ok | ok | ok | ok | ok | ok | **FAIL** | **FAIL** | **FAIL** | ok | **FAIL** | replaying expected does not grade 1.0 (grade=0): [(&#x27;reply1&#x27;, &#x27;2,5,6;Toxoplasmose,Grippe,Tuberculose,Lèpre,Poliomyélite,Hépatite,ces maladies ne sont pas dues à des virus&#x27;, &#x27;2,5,6;Toxoplasmose,Grippe,Tuberculose,LÃ¨pre,PoliomyÃ©lite,HÃ©patite,ces maladies ne sont pas dues Ã\xa0 des virus&#x27;)]<br>after cutting expected at &#x27;;&#x27;: still not 1.0: La réponse
  2,5,6 que vous avez donnée
  n&#x27;est pas compréhensible. Veuillez corriger l&#x27;erreur et soumettre de nouveau la réponse.<br>page differs outside the JSON block: line 528:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00047 | case | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | **FAIL** | page differs outside the JSON block: line 498:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00055 | function | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00057 | dragfill | ok | ok | ok | ok | ? | ok | ok | ok | - | ok | **FAIL** | page differs outside the JSON block: line 478:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00058 | formal | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00072 | function | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00085 | algexp,checkbox,default,formal,fset,function,matrix,set,vector | ok | ok | ok | ok | ? | ok | ok | **FAIL** | **FAIL** | ok | **FAIL** | html analysis rows=2 vs json answers=12: the hook reports answers the engine did not grade (step not reached / empty replygood)<br>replaying expected does not grade 1.0 (grade=0.5): [(&#x27;reply1&#x27;, &#x27;2,3;1,2,3,4&#x27;, &#x27;2,3;1,2,3,4&#x27;), (&#x27;reply2&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply4&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply5&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply6&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply7&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply8&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply9&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply10&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply11&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply12&#x27;, &#x27;2&#x27;, &#x27;2&#x27;)]<br>after cutting expected at &#x27;;&#x27;: still not 1.0: [(&#x27;reply2&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply4&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply5&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply6&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply7&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply8&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply9&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply10&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply11&#x27;, &#x27;2&#x27;, &#x27;2&#x27;), (&#x27;reply12&#x27;, &#x27;2&#x27;, &#x27;2&#x27;)]<br>page differs outside the JSON block: line 500:
  W: 
  N: &lt;br class=&quot;spacer clearall&quot;&gt; |
| 00352 | - | ? | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | exception: RuntimeError(&#x27;no session in cmd=new: ?&#x27;) |
| somme_demo | numeric | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | **FAIL** | page differs outside the JSON block: line 443:
  W: 
  N: &lt;div class=&quot;wims_widget_box clearall&quot;&gt; |
