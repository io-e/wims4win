<!-- CLAUDE_V1 -->
# AlgoGebra JSON hook — corpus review (Windows runtime)

| exo | types | W marker | JSON | n==rows | grade==html | correct==html | given==sent | ASCII | replay 1.0 | replay ;-trim | no leak | page== | findings |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 00002 | default | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00004 | radio | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00008 | menu | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00014 | clickfill | ok | ok | ok | ok | ? | ok | ok | ok | - | ok | ok |  |
| 00021 | numexp | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok | answers=4, def replycnt=5 choicecnt=0 (ungraded replies omitted?) |
| 00027 | - | ? | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | exception: RuntimeError(&#x27;no session in cmd=new: ?&#x27;) |
| 00045 | checkbox | ok | ok | ok | ok | ok | ok | **FAIL** | ok | - | ok | ok |  |
| 00047 | case | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00055 | function | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00057 | dragfill | ok | ok | ok | ok | ? | ok | ok | ok | - | ok | ok |  |
| 00058 | formal | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00072 | function | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00085 | algexp,checkbox,default,formal,fset,function,matrix,set,vector | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok | answers=2, def replycnt=12 choicecnt=0 (ungraded replies omitted?) |
| 00352 | - | ? | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | exception: RuntimeError(&#x27;no session in cmd=new: ?&#x27;) |
| somme_demo | numeric | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
