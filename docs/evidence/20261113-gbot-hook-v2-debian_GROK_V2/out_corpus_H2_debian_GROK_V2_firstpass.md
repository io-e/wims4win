<!-- CLAUDE_V1 -->
# AlgoGebra JSON hook — corpus review (Debian runtime, GROK_V2 body)

| exo | types | W marker | JSON | n==rows | grade==html | correct==html | given==sent | ASCII | replay 1.0 | replay ;-trim | no leak | page== | findings |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 00004 | radio | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00008 | menu | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok |  |
| 00021 | numexp | **FAIL** | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | hook marker missing on cmd=reply (answer_analysis=False) |
| 00045 | checkbox | ok | ok | ok | ok | ok | ok | **FAIL** | ok | - | ok | ok |  |
| 00055 | - | ? | ? | ? | ? | ? | ? | ? | ? | - | ? | ? | exception: RuntimeError(&#x27;no session in cmd=new: ?&#x27;) |
| 00085 | algexp,checkbox,default,formal,fset,function,matrix,set,vector | ok | ok | ok | ok | ok | ok | ok | ok | - | ok | ok | answers=2, def replycnt=12 choicecnt=0 (ungraded replies omitted?) |
