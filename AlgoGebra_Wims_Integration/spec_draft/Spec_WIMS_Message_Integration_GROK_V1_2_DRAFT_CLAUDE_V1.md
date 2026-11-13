<!-- GROK_V1_2 -->
<!-- CLAUDE_V1 : draft prepared by Claude for Arnaud's approval, 2026-11-13. NOT published, NOT committed to io-e/AlgoGebra. -->
# Specification — WIMS Message Integration in AlgoGebra

| Field | Value |
|---|---|
| Document | `02-Cdc/Spec_WIMS_Message_Integration_GROK_V1_2.md` |
| Version | GROK_V1_2 (**draft**, awaiting owner approval) |
| Date | 2026-11-13 |
| Status | **Draft** — aligned with wims4win hook V2 (`feat/algogebra-wims-integration` @ `bece517`, `runtime_patch/algogebra_json_CLAUDE_V2.phtml`) and the Windows corpus review |
| Supersedes | `Spec_WIMS_Message_Integration_GROK_V1_1.md` (keep as archive) |
| Depends on | `Cdc_WIMS_Integration_AlgoGebra_GROK_V2_2.md` (WI-20..WI-24), `Decisions_WIMS_CDC_Open_Points_Owner_GROK_V1_1.md` (D-WIMS-G01), `Spec_State_Contract_CLAUDE_V1.md` (§5 submission), `Spec_Activity_Manifest_CLAUDE_V1.md` |
| Evidence | `GV_Residual_Results_GBOT_V1.md` (G-A6), wims4win `AlgoGebra_Wims_Integration/OEF_Variable_Map_GROK_V1.md`, `tests/test_algogebra_json_GROK_V1.py`, **`Review_Hook_JSON_Windows_CLAUDE_V1.md` (F1–F6)**, `tests/out_corpus_CLAUDE_V2_hook_CLAUDE_V1.json` (10 real blocks, usable as client fixtures) |

---

## 0. Changes since GROK_V1_1

Only four points change; everything else is carried over verbatim. Each change is tagged **[V1_2]** in the body.

| § | Change | Origin |
|---|---|---|
| 3.1 | `qnum` is never sent; it stays at the WIMS default (1). Series are out of scope. | Review F5 |
| 3.2 | `expected` is the text the form posts (option text for menu / radio / checkbox and the same family), never `<index>;<option list>`. The JSON block is in the **page charset** (`windows-1252` on this server), not ASCII. | Review F1, F6 |
| 5.2 | The reply POST is percent-encoded in the **charset of the `cmd=new` response**, not UTF-8. | Review F6 |
| 6 | New error `INVALID_ANSWER`: HTTP 200, no JSON block, `class="wims_msg alert"` present. `NO_JSON_BLOCK` keeps its meaning when no alert is present. No new JSON field. | Review F4, owner decision (no schema change) |

---

## 1. Scope

This spec defines the **message protocol** between AlgoGebra (desktop RCP client or web client) and a WIMS server for exercise lifecycle operations. It covers:

1. **Session creation** — `cmd=new` (get exercise with random parameters)
2. **Answer submission** — `cmd=reply` (submit student answers, get grade)
3. **JSON extraction** — parsing the `ALGOGEBRA_JSON` block from WIMS HTML responses
4. **Error handling** — WIMS error codes, timeouts, encoding
5. **`adm/raw` auxiliary calls** — class operations, score aggregation (not grading)

It does NOT cover:
- Offline evaluation (see `Spec_State_Contract_CLAUDE_V1.md` §5 + `Cdc_WIMS_Integration_AlgoGebra_CLAUDE_V2.md` WI-30..WI-32)
- OEF conversion pipeline (see WI-03..WI-06)
- Engine subprocess calls (see `Cdc_External_Engines_AlgoGebra_CLAUDE_V1_3.md`)

---

## 2. Deployment modes and endpoints

From `Cdc_WIMS_Integration_AlgoGebra_CLAUDE_V2.md` WI-01:

| Mode | Base URL | Auth | Phase |
|---|---|---|---|
| **Offline** | No WIMS server | N/A | MVP |
| **Local-connected** | `http://localhost:{port}/wims/wims.cgi` | Anonymous (`cmd=new/reply`) or `.connections` ident (`adm/raw`) | Phase 2 Step 1 |
| **Institutional** | `https://{server}/wims/wims.cgi` | TLS + `.connections` + optional SSO | Phase 2 Step 3 |

The `WimsEndpoint` is configured in `activity.json` via `wimsClassRef` (optional). When absent, AlgoGebra runs in offline mode.

---

## 3. Message types

### 3.1 Exercise session creation — `cmd=new`

**Purpose:** Create a new exercise session on the WIMS server. WIMS generates random parameters and returns the exercise HTML.

**Request:**

```
POST {base_url}
Content-Type: application/x-www-form-urlencoded; charset=UTF-8

module={class_module}
&cmd=new
&lang={locale}
&exo={exercise_id}
```

| Parameter | Type | Required | Description |
|---|---|---|---|
| `module` | string | Yes | WIMS class/sheet module path (e.g., `H6/test/oef2html.fr`) |
| `cmd` | string | Yes | Always `new` |
| `lang` | string | No | Language code (`fr`, `en`). Default: WIMS server locale |
| `exo` | string | Yes | Exercise identifier within the module |
| `qnum` | — | **Never sent** | **[V1_2]** WIMS defaults to 1 (one question per session). With `qnum>1`, `module_score` is undefined until the last question and the hook reports `grade: 0` on intermediate questions (review F5). Series are out of scope; a provisional grade would need a hook change. |

**Response:** `200 OK`, `Content-Type: text/html; charset=windows-1252`

**[V1_2]** The client **must record the charset** of this response (from `Content-Type`, fallback `windows-1252`): it is the charset in which the `cmd=reply` body is percent-encoded (§5.2), exactly as a browser does for the form it was served.

The response is standard WIMS exercise HTML containing:
- Exercise text with input fields (`reply1`, `reply2`, ...)
- Session identifier (in form hidden fields or cookies)
- Random parameter values (baked into HTML)

**AlgoGebra processing:**
1. Extract session identifier from response (hidden form field `wims_session` or cookie)
2. Extract exercise parameters if needed for display
3. Store session ID for subsequent `cmd=reply`
4. Do NOT render WIMS HTML — AlgoGebra has its own UI. The HTML is parsed for structure only.

### 3.2 Answer submission — `cmd=reply`

**Purpose:** Submit student answers and receive grading results.

**Request:**

```
POST {base_url}
Content-Type: application/x-www-form-urlencoded; charset=UTF-8

module={class_module}
&cmd=reply
&special_parm=algogebra_json
&session={wims_session}
&reply1={answer_1}
&reply2={answer_2}
...
```

| Parameter | Type | Required | Description |
|---|---|---|---|
| `module` | string | Yes | Same as `cmd=new` |
| `cmd` | string | Yes | Always `reply` |
| `special_parm` | string | Yes | `algogebra_json` — triggers the JSON output hook (L2-1). **Reply only** — never on `cmd=new` |
| `session` | string | Yes | WIMS session from `cmd=new` response (form field / cookie) |
| `reply1`..`replyN` | string | Yes* | Student answers for `\answer` fields (*as present in the exercise) |
| `choice1`..`choiceN` | string | Yes* | Student answers for `\choice` fields when present |

**Do not invent `ans1`.** Field names must match the OEF form (`replyN` / `choiceN`).

**Response:** `200 OK`, `Content-Type: text/html; charset=windows-1252`

Standard WIMS grading HTML **plus** an embedded JSON block:

```html
<!-- normal WIMS HTML grading response -->
...
<!-- ALGOGEBRA_JSON_BEGIN
{
  "grade": 0.75,
  "seed": 12345,
  "answers": [
    {
      "id": "reply1",
      "correct": true,
      "score": 1,
      "feedback": "",
      "expected": "42",
      "given": "42"
    },
    {
      "id": "reply2",
      "correct": false,
      "score": 0.5,
      "feedback": "",
      "expected": "2*x",
      "given": "3x"
    }
  ],
  "feedback": ""
}
ALGOGEBRA_JSON_END -->
```

**Field contract (hook V2) [V1_2]:**

| Field | Contract |
|---|---|
| `grade` | JSON number, `module_score / 10`, in [0, 1]. Equals the score WIMS prints. |
| `seed` | JSON number, `wims_seed` of the session. |
| `answers[]` | One entry per **graded** `\answer` / `\choice`, in the order of the analysis rows WIMS displays. Replies with an empty good answer (free point) and replies of steps not reached yet (`\steps`) are **omitted**, as WIMS omits them. The count can therefore be smaller than the number of `replyN` fields in the form. |
| `id` | `replyN` / `choiceN`, matching the form field name. |
| `correct` | JSON boolean, `score >= 1`. |
| `score` | JSON number per reply: `0`, `0.5` (partial / precision) or `1`. |
| `expected` | **The text the form posts for the good answer**, in display form, `!singlespace`d: option text for `menu`, `radio`, `checkbox`, `click`, `mark`, `multipleclick`, `flashcard` (comma-separated when several); first row of the good answer for `clickfill`, `dragfill`, `case`, `nocase`, `atext`, `compose`, `coord`, `imgcomp`, `javacurve`, `reorder`, `textcomp`, `wlist`; the raw good answer otherwise (`numeric`, `numexp`, `algexp`, `function`, `formal`, `matrix`, …). It is never `<index>;<option list>`. Replaying `expected` as `given` yields `grade: 1.0` on the reference corpus. |
| `given` | The posted value after `!singlespace`. |
| `feedback` | Reserved, empty string in V2 (both per answer and global). |

Escaping: `\` → `\\`, `"` → `\"`, and `--` is neutralised so the HTML comment cannot be closed early.

**Charset [V1_2]:** the JSON block is **not** ASCII-safe. Strings are emitted in the page charset (`windows-1252` on wims4win / Debian default) whenever an option text or a text answer contains non-ASCII characters (e.g. `Poliomyélite`). The client decodes the whole response body with the `Content-Type` charset (§5.2) **before** locating the markers and parsing the JSON; no `\uNNNN` escaping is performed by the hook.

### 3.3 `adm/raw` auxiliary calls

**Purpose:** Class management, score aggregation, server info. NOT used for exercise grading.

**Request:**

```
POST {base_url}
Content-Type: application/x-www-form-urlencoded; charset=UTF-8

module=adm/raw
&job={job_name}
&ident={connection_ident}
&passwd={connection_passwd}
&option1=value1
```

**Response:** `200 OK`, `Content-Type: application/json; charset=iso-8859-1`

`adm/raw` returns JSON (unlike `cmd=new/reply` which returns HTML). Used for:

| Job | Purpose | Auth required |
|---|---|---|
| `getinfoserver` | Server version, available modules | No (5 jobs need no auth) |
| `getclass` | Class info | Yes (`.connections` ident) |
| `getscore` | Aggregate stored scores | Yes |
| `getsheetscores` | Per-sheet scores | Yes |

**Not used for:** OEF answer evaluation (`testexo` only compiles, `getscore*` only reads stored aggregates — see D-WIMS-G01 rationale).

---

## 4. Java client architecture

### 4.1 Interface

```java
package org.geogebraplus.wims;

public interface WimsClient {
    WimsSession createSession(WimsExerciseRef ref, Locale locale)
        throws WimsException;

    GradeResult submitAnswers(WimsSession session, Map<String, String> answers)
        throws WimsException;

    ServerInfo getServerInfo()
        throws WimsException;
}
```

### 4.2 DTOs

```java
public class WimsExerciseRef {
    String module;      // e.g. "H6/test/oef2html.fr"
    String exerciseId;  // e.g. "00072"
}

public class WimsSession {
    String sessionId;       // from WIMS response
    String module;
    String exerciseId;
    Instant createdAt;
    WimsEndpoint endpoint;  // base URL + auth
}

public class GradeResult {
    double grade;                    // 0.0 .. 1.0
    List<AnswerResult> answers;      // per-answer detail
    String overallFeedback;          // optional text

    public static class AnswerResult {
        String id;          // "reply1", "choice1", ...
        boolean correct;
        double score;       // 0.0 .. 1.0
        String feedback;    // per-answer text
    }
}

public class WimsEndpoint {
    URI baseUrl;                // e.g. http://localhost:18431/wims/wims.cgi
    String connectionIdent;     // for adm/raw, null for anonymous
    String connectionPasswd;    // for adm/raw
}

public class ServerInfo {
    String version;     // e.g. "4.31b"
    String protocol;    // e.g. "WIMS 4.31"
}
```

### 4.3 Implementation: `HttpWimsClient`

```java
@Component(service = WimsClient.class)
public class HttpWimsClient implements WimsClient {

    private final HttpClient httpClient;

    public HttpWimsClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public WimsSession createSession(WimsExerciseRef ref, Locale locale)
            throws WimsException {
        Map<String, String> params = Map.of(
            "module", ref.module,
            "cmd", "new",
            "lang", locale.getLanguage(),
            "exo", ref.exerciseId
        );
        String html = postForm(params);
        String sessionId = extractSessionId(html);
        return new WimsSession(sessionId, ref.module, ref.exerciseId,
                              Instant.now(), endpoint);
    }

    @Override
    public GradeResult submitAnswers(WimsSession session,
                                      Map<String, String> answers)
            throws WimsException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("module", session.module);
        params.put("cmd", "reply");
        params.put("special_parm", "algogebra_json");
        params.put("exercise", session.exerciseId);
        params.put("session", session.sessionId);
        answers.forEach(params::put);

        String html = postForm(params);
        return extractGradeJson(html);
    }
}
```

### 4.4 JSON extraction

```java
private static final String JSON_BEGIN = "<!-- ALGOGEBRA_JSON_BEGIN";
private static final String JSON_END = "ALGOGEBRA_JSON_END -->";

// [V1_2] WIMS redisplays the form with this class on bad_answer / empty_data,
// and answer.phtml exits before the hook on bad_exo. HTTP status is 200.
private static final Pattern WIMS_ALERT =
    Pattern.compile("<div[^>]*class=\"wims_msg alert\"[^>]*>(.*?)</div>", Pattern.DOTALL);

GradeResult extractGradeJson(String html) throws WimsException {
    int start = html.indexOf(JSON_BEGIN);
    int end = html.indexOf(JSON_END);
    if (start < 0 || end < 0 || end <= start) {
        Matcher m = WIMS_ALERT.matcher(html);                 // [V1_2]
        if (m.find()) {
            throw new WimsException(WimsError.INVALID_ANSWER,
                stripTags(m.group(1)).trim());                 // the WIMS message, shown to the user
        }
        throw new WimsException(WimsError.NO_JSON_BLOCK,
            "WIMS response did not contain ALGOGEBRA_JSON block. " +
            "Is the L2-1 JSON hook installed?");
    }
    String json = html.substring(start + JSON_BEGIN.length(), end).trim();
    return parseGradeJson(json);
}
```

`html` is already decoded with the response charset (§5.2), so `expected` / `given` strings are correct Java `String`s with no further transcoding. **[V1_2]**

---

## 5. Encoding

### 5.1 Problem

WIMS responses use `windows-1252` or `iso-8859-1` (see WI-20 evidence, WI-R8). AlgoGebra uses UTF-8 internally.

### 5.2 Design

| Direction | Encoding | Responsibility |
|---|---|---|
| AlgoGebra → WIMS | **[V1_2] Charset of the `cmd=new` response** (`windows-1252` on this server), never UTF-8 | `HttpWimsClient` URL-encodes keys and values with `session.charset`, as a browser does for the form it was served. WIMS reads form bytes in the page charset: `Poliomyélite` sent as UTF-8 arrives as `PoliomyÃ©lite` and is graded wrong (review F6, reproduced: 0.33 with UTF-8, 1.0 with `windows-1252`). Characters outside the page charset cannot be submitted; the client replaces them (`?`) and warns. |
| WIMS → AlgoGebra | `windows-1252` (HTML, **including the JSON block**) or `iso-8859-1` (`adm/raw` JSON) | `HttpWimsClient` reads `Content-Type` charset, decodes the whole body accordingly, then extracts the JSON (§4.4). **[V1_2]** |
| Internal storage | UTF-8 | All `GradeResult`, `SessionState`, `course.json` |

```java
// [V1_2] formCharset: UTF_8 for the very first cmd=new (nothing to encode but ASCII),
// then session.charset = charset of the cmd=new response for every cmd=reply.
private String postForm(Map<String, String> params, Charset formCharset)
        throws WimsException {
    String body = params.entrySet().stream()
        .map(e -> URLEncoder.encode(e.getKey(), formCharset) + "=" +
                  URLEncoder.encode(e.getValue(), formCharset))
        .collect(Collectors.joining("&"));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(endpoint.baseUrl)
        .header("Content-Type",
                "application/x-www-form-urlencoded; charset=" + formCharset.name())
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(30))
        .build();

    HttpResponse<byte[]> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

    Charset responseCharset = extractCharset(response);
    lastResponseCharset = responseCharset;      // [V1_2] stored into WimsSession by createSession()
    return new String(response.body(), responseCharset);
}

private Charset extractCharset(HttpResponse<?> response) {
    return response.headers().firstValue("Content-Type")
        .filter(ct -> ct.contains("charset="))
        .map(ct -> {
            String cs = ct.substring(ct.indexOf("charset=") + 8).trim();
            if (cs.contains(";")) cs = cs.substring(0, cs.indexOf(";"));
            return cs;
        })
        .map(cs -> {
            try { return Charset.forName(cs); }
            catch (Exception e) { return StandardCharsets.ISO_8859_1; }
        })
        .orElse(Charset.forName("windows-1252")); // [V1_2] WIMS default page charset
}
```

**[V1_2]** `WimsSession` gains a `charset` field (the `cmd=new` response charset). `submitAnswers()` calls `postForm(params, session.charset)`. `SessionState` persists it with the session id (§8.2).

---

## 6. Error handling

### 6.1 WIMS error codes

| HTTP status | WIMS meaning | AlgoGebra action |
|---|---|---|
| `200` + JSON block | Success | Parse the block (§4.4) |
| `200` + no block + `class="wims_msg alert"` | **[V1_2]** WIMS rejected the submission: `bad_answer` (answer not understood / wrong syntax), `empty_data` (nothing filled in), `bad_exo` (exercise failed to run, e.g. CAS unavailable). The form is redisplayed, the session stays `waiting`. | `WimsError.INVALID_ANSWER` — show the WIMS message, let the student correct and resubmit on the **same** session. Not a hook problem. |
| `200` + no block + no alert | Hook absent or not triggered | `WimsError.NO_JSON_BLOCK` |
| `450` | Invalid command (e.g., `cmd=getresult`) | `WimsError.INVALID_COMMAND` — programming error, not user error |
| `400` | Bad request (missing param) | `WimsError.BAD_REQUEST` |
| `403` | Authentication required/failed | `WimsError.AUTH_FAILED` |
| `500` | Server error | `WimsError.SERVER_ERROR` |
| Connection refused | WIMS not running | `WimsError.CONNECTION_REFUSED` |
| Timeout | No response in 30s | `WimsError.TIMEOUT` |

### 6.2 Error enum

```java
public enum WimsError {
    CONNECTION_REFUSED,     // WIMS server not reachable
    TIMEOUT,                // No response within deadline
    AUTH_FAILED,            // .connections ident/passwd rejected
    INVALID_COMMAND,        // cmd= not in WIMS's closed list
    BAD_REQUEST,            // Missing required parameter
    SERVER_ERROR,           // WIMS 500
    NO_JSON_BLOCK,          // L2-1 hook not installed or not triggered (200, no block, no alert)
    INVALID_ANSWER,         // [V1_2] 200, no block, wims_msg alert: WIMS rejected the answer, retry
    MALFORMED_JSON,         // JSON block present but unparseable
    SESSION_EXPIRED,        // WIMS session no longer valid
    ENCODING_ERROR          // Response charset unreadable
}
```

### 6.3 User-facing messages

| Error | User sees |
|---|---|
| `CONNECTION_REFUSED` | "Cannot connect to WIMS server at {url}. Check that WIMS is running." |
| `TIMEOUT` | "WIMS server did not respond in time. Try again." |
| `AUTH_FAILED` | "Authentication failed. Check your WIMS connection settings." |
| `NO_JSON_BLOCK` | "WIMS returned a grade but AlgoGebra could not read it. The JSON hook may not be installed on this server." |
| `INVALID_ANSWER` | **[V1_2]** "WIMS did not accept this answer: {wims message}. Please correct it and submit again." (the WIMS message, tags stripped, in the exercise language) |
| `SESSION_EXPIRED` | "Your exercise session has expired. Starting a new one." |

---

## 7. Message flow diagrams

### 7.1 Connected mode — exercise lifecycle

```
Student                 AlgoGebra                   WIMS server
  │                        │                            │
  │  opens exercise        │                            │
  │───────────────────────>│                            │
  │                        │  POST cmd=new              │
  │                        │───────────────────────────>│
  │                        │  200 HTML (exercise)       │
  │                        │<───────────────────────────│
  │                        │  extract session ID        │
  │  sees exercise in UI   │                            │
  │<───────────────────────│                            │
  │                        │                            │
  │  submits answers       │                            │
  │───────────────────────>│                            │
  │                        │  POST cmd=reply            │
  │                        │  +special_parm=            │
  │                        │   algogebra_json           │
  │                        │───────────────────────────>│
  │                        │  200 HTML + JSON block     │
  │                        │<───────────────────────────│
  │                        │  extract ALGOGEBRA_JSON    │
  │                        │  parse GradeResult         │
  │  sees grade + feedback │                            │
  │<───────────────────────│                            │
  │                        │  update SessionState       │
  │                        │  (local save)              │
```

### 7.2 Offline mode — no WIMS messages

```
Student                 AlgoGebra
  │                        │
  │  opens exercise        │
  │───────────────────────>│
  │                        │  load course.json
  │                        │  pick random variant
  │  sees exercise in UI   │
  │<───────────────────────│
  │                        │
  │  submits answers       │
  │───────────────────────>│
  │                        │  local check (WI-31 types)
  │                        │  compare vs frozen expected
  │  sees grade + feedback │
  │<───────────────────────│
```

---

## 8. Session management

### 8.1 Session lifecycle

| Event | AlgoGebra action |
|---|---|
| Activity load (connected mode) | `WimsClient.createSession()` for each exercise in the course |
| Student navigates to exercise | Use existing session (or create if not yet created) |
| Student submits answer | `WimsClient.submitAnswers()` — one call per check attempt |
| Session expired (WIMS returns error) | Auto-recreate session with `cmd=new`, inform student "New random parameters" |
| Activity close | No WIMS call needed (sessions expire server-side) |

### 8.2 Session storage

Connected-mode sessions are stored in `SessionState.wimsSession`:

```java
public class SessionState {
    // ... existing fields from Spec_State_Contract ...

    // Connected mode (null in offline)
    WimsSession wimsSession;
    Map<String, WimsSession> exerciseSessions; // exerciseId → session
}
```

### 8.3 Retry policy

From `Spec_State_Contract_CLAUDE_V1.md` §5.3:

- Maximum 3 retries with exponential backoff (1s, 2s, 4s)
- Same `(sessionId, submissionSequence)` for idempotency
- After 3 failures: "Submission failed — your answers are saved locally"

---

## 9. Security

### 9.1 Input sanitization

Student answers are URL-encoded before sending. No raw string concatenation into the POST body.

```java
// SAFE: URL-encoded
URLEncoder.encode(studentAnswer, UTF_8)

// NEVER: string concatenation
"reply1=" + studentAnswer  // injection risk
```

### 9.2 Response parsing

The JSON block is extracted by exact string markers (`ALGOGEBRA_JSON_BEGIN` / `ALGOGEBRA_JSON_END`). The HTML outside these markers is never evaluated, rendered, or passed to any interpreter.

### 9.3 TLS (institutional mode)

Institutional connections require HTTPS. The `WimsEndpoint` validates:
- `baseUrl.getScheme().equals("https")` for institutional mode
- Certificate chain is valid (standard Java TLS)
- No downgrade to HTTP

### 9.4 `.connections` credentials

Stored in the RCP product's secure preferences (Eclipse `ISecurePreferences`). Never logged, never included in error messages, never sent to non-WIMS endpoints.

---

## 10. OSGi service registration

`HttpWimsClient` is registered as an OSGi DS component:

```java
@Component(
    service = WimsClient.class,
    configurationPid = "org.geogebraplus.wims.client",
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
public class HttpWimsClient implements WimsClient {

    @Activate
    void activate(Map<String, Object> config) {
        String url = (String) config.getOrDefault(
            "wims.endpoint.url", "http://localhost:18431/wims/wims.cgi");
        this.endpoint = new WimsEndpoint(URI.create(url), ...);
    }
}
```

Configuration via OSGi ConfigAdmin or `activity.json` `wimsClassRef`.

---

## 11. Scenario mapping

| Scenario | Requirement | This spec |
|---|---|---|
| **S-01** | WIMS schema/capability registration | §3.3 `adm/raw` for class setup |
| **S-02** | Submission → grade → display | §3.2 `cmd=reply` + JSON extraction |
| **S-03** | Idempotency (retry same key) | §8.3 retry with same submission sequence |
| **S-04** | Session save/resume | §8.2 `wimsSession` in `SessionState` |
| **X-06** | Malformed input error handling | §6 error codes and user messages |
| **W-04** | XSS, command injection | §9 input sanitization, response parsing |

---

## 12. Dependencies on L2-1 JSON hook

**WA-8 status (2026-11-13) [V1_2]:** hook vendored by GBot in wims4win `tools/algogebra-hook/` (PR #10, installed by `build-runtime-tree.sh`, `ALGOGEBRA_HOOK=0` opts out). Body **V2** (`runtime_patch/algogebra_json_CLAUDE_V2.phtml`, same schema) fixes `expected` for the menu family, omits free-point replies and unreached steps; Windows corpus review: 10/10 graded exercises, `expected` replays to 1.0, pages identical outside the block. PR #10 is to adopt V2 before merge. `function` / `formal` types remain to be replayed on a runtime with Maxima (Debian or Laragon sibling). Without the hook on a given server:

- `cmd=reply` returns HTML only — no `ALGOGEBRA_JSON` block
- `HttpWimsClient.submitAnswers()` throws `WimsError.NO_JSON_BLOCK`
- **Fallback:** AlgoGebra could parse WIMS HTML for the grade string (fragile, locale-dependent, not recommended)

The hook is a minimal modification under `scripts/oef/` (`answer.phtml` injection). Residual: Debian packaging / `build-runtime-tree.sh`. Client rebase is **WA-9** (unblocked).

---

*End of `Spec_WIMS_Message_Integration_GROK_V1_2.md` (draft).*
