#!/usr/bin/env python3
"""Optional Playwright end-to-end test: student login → exercise → reply →
teacher Notes (userscore). Credentials from --creds or WIMS_CREDS_FILE / env.
Never hardcodes secrets. Requires: pip install playwright && playwright install chromium.
"""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urljoin, urlsplit, urlunsplit


def load_creds(path: str | None) -> dict[str, str]:
    data: dict[str, str] = {}
    if path:
        p = Path(path)
        if not p.is_file():
            raise SystemExit("playwright: cannot read creds file")
        for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            data[k.strip()] = v.strip()

    def pick(*names: str, default: str = "") -> str:
        for n in names:
            ev = os.environ.get(n)
            if ev:
                return ev
            key = n.lower().removeprefix("wims_")
            if key in data and data[key]:
                return data[key]
        return default

    c = {
        "class": pick("WIMS_CLASS", default=""),
        "supervisor_login": pick("WIMS_SUPERVISOR_LOGIN"),
        "supervisor_password": pick("WIMS_SUPERVISOR_PASSWORD"),
        "participant_login": pick("WIMS_PARTICIPANT_LOGIN"),
        "participant_password": pick("WIMS_PARTICIPANT_PASSWORD"),
        "exercise_module": pick("WIMS_EXERCISE_MODULE", default="classes/fr"),
        "exercise_exo": pick("WIMS_EXERCISE_EXO", default="exi1"),
        "exercise_sheet": pick("WIMS_EXERCISE_SHEET", default="1"),
    }
    # also accept file keys already in data via pick fallback
    if not c["class"]:
        c["class"] = data.get("class", "")
    if not c["supervisor_login"]:
        c["supervisor_login"] = data.get("supervisor_login", "")
    if not c["supervisor_password"]:
        c["supervisor_password"] = data.get("supervisor_password", "")
    if not c["participant_login"]:
        c["participant_login"] = data.get("participant_login", "")
    if not c["participant_password"]:
        c["participant_password"] = data.get("participant_password", "")
    if not all(
        [
            c["class"],
            c["supervisor_login"],
            c["supervisor_password"],
            c["participant_login"],
            c["participant_password"],
        ]
    ):
        raise SystemExit("playwright: missing class or login fields")
    return c


def cgi_url(base: str, params: dict[str, str]) -> str:
    parts = urlsplit(base)
    return urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(params), ""))


def session_of(url: str, page=None) -> str:
    q = parse_qs(urlsplit(url).query)
    sid = (q.get("session") or [""])[0].split(".")[0]
    if sid and sid.lower() not in ("new", "intro", "reply", "done"):
        return sid
    if page is not None:
        try:
            hidden = page.locator('input[name="session"]').first
            if hidden.count():
                val = (hidden.input_value() or "").split(".")[0]
                if val and val.lower() not in ("new", "intro", "reply", "done"):
                    return val
        except Exception:
            pass
        try:
            html = page_content(page)
            m = re.search(r"session=([A-Za-z0-9]{8,})", html)
            if m:
                return m.group(1).split(".")[0]
        except Exception:
            pass
    return ""


def page_content(page) -> str:
    for _ in range(5):
        try:
            page.wait_for_load_state("load", timeout=15000)
            return page.content()
        except Exception:
            page.wait_for_timeout(300)
    return page.content()


def submit_form(page) -> None:
    form = page.locator("form").first
    submit = form.locator('input[type="submit"], button[type="submit"], button[name="cmd"]').first
    with page.expect_navigation(timeout=30000):
        if submit.count():
            submit.click()
        else:
            form.evaluate("form => form.submit()")
    page.wait_for_load_state("load", timeout=30000)



def fill_login(page, user, password) -> None:
    page.evaluate(
        """({user, password}) => {
          const u = document.querySelector('input[name="auth_user"]');
          const p = document.querySelector('input[name="auth_password"]');
          const g = document.querySelector('input[name="agreecgu"]');
          if (u) { u.value = user; }
          if (p) { p.value = password; }
          if (g) { g.value = 'yes'; g.checked = true; }
          const form = document.querySelector('form');
          if (form) {
            let c = form.querySelector('input[name="cmd"]');
            if (!c) { c = document.createElement('input'); c.type='hidden'; c.name='cmd'; form.appendChild(c); }
            c.value = 'reply';
          }
        }""",
        {"user": user, "password": password},
    )

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default=os.environ.get("WIMS_BASE_URL", ""),
                    help="CGI URL, e.g. http://127.0.0.1:8765/wims/wims.cgi")
    ap.add_argument("--creds", default=os.environ.get("WIMS_CREDS_FILE", ""))
    ap.add_argument("--headed", action="store_true")
    args = ap.parse_args()
    if not args.base_url:
        print("playwright: --base-url / WIMS_BASE_URL required", file=sys.stderr)
        return 2
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print(
            "PLAYWRIGHT SKIP: install optional deps with "
            "`pip install playwright` and `playwright install chromium`",
            file=sys.stderr,
        )
        return 2

    creds = load_creds(args.creds or None)
    failures: list[str] = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=not args.headed)
        context = browser.new_context()
        page = context.new_page()

        # --- student login ---
        page.goto(
            cgi_url(
                args.base_url,
                {
                    "module": "adm/class/classes",
                    "cmd": "new",
                    "lang": "fr",
                    "type": "authparticipant",
                    "class": creds["class"],
                },
            ),
            wait_until="load",
            timeout=30000,
        )
        # Prefer named fields; WIMS login forms use auth_user / auth_password.
        if page.locator('input[name="auth_user"]').count() == 0:
            failures.append("student: login form has no auth_user")
        else:
            fill_login(page, creds["participant_login"], creds["participant_password"])
            submit_form(page)

        stu_url = page.url
        stu_sess = session_of(stu_url, page)
        body = page_content(page)
        student_ok = bool(stu_sess) and ("bad_password" not in body.lower())
        # A remaining visible password field usually means still on the form.
        if page.locator('input[name="auth_password"]:visible').count():
            student_ok = False
        print("STUDENT url_keys=" + ",".join(parse_qs(urlsplit(stu_url).query).keys()))
        print(f"STUDENT login={'PASS' if student_ok else 'FAIL'} session={stu_sess or 'none'}")
        if not student_ok:
            failures.append("student login")

        # --- open exercise: click the worksheet link on the class home ---
        if stu_sess:
            exo = creds["exercise_exo"]
            link = page.locator(f'a[href*="exo={exo}"]').first
            if link.count():
                href = link.get_attribute("href")
                page.goto(urljoin(page.url, href), wait_until="load", timeout=30000)
            else:
                # fallback: constructed URL (plus-prefixed params, %2B encoded)
                page.goto(
                    cgi_url(
                        args.base_url,
                        {
                            "session": stu_sess,
                            "+cmd": "new",
                            "+worksheet": creds["exercise_sheet"],
                            "+module": creds["exercise_module"],
                            "+exo": exo,
                            "qnum": "1",
                        },
                    ),
                    wait_until="load",
                    timeout=30000,
                )
            html = page_content(page)
            exo_ok = bool(html) and ("groupement d" not in html.lower()) and ("sans pr" not in html.lower())
            has_form = page.locator("form").count() > 0
            print(
                f"EXERCISE open={'PASS' if exo_ok else 'FAIL'} form={has_form} "
                f"module={creds['exercise_module']} exo={exo}"
            )
            if not exo_ok:
                failures.append("open exercise")

            # --- cmd=reply ---
            form = page.locator("form").first
            if form.count():
                # fill empty answer-like fields with a dummy value (wrong answers are fine)
                page.evaluate(
                    """() => {
                      const form = document.querySelector('form');
                      if (!form) return;
                      let cmd = form.querySelector('input[name="cmd"]');
                      if (!cmd) {
                        cmd = document.createElement('input');
                        cmd.type = 'hidden'; cmd.name = 'cmd'; form.appendChild(cmd);
                      }
                      cmd.value = 'reply';
                      for (const el of form.querySelectorAll('input, textarea, select')) {
                        const n = (el.name || '').toLowerCase();
                        if (!n || /password|passwd/.test(n)) continue;
                        if (el.type === 'hidden' || el.type === 'submit') continue;
                        if ((el.value || '') === '' &&
                            /reply|answer|rep|oef|choice|^q\\d|^r\\d/.test(n)) {
                          if (el.tagName === 'SELECT' && el.options.length) {
                            el.selectedIndex = 1;
                          } else if (el.type === 'radio' || el.type === 'checkbox') {
                            el.checked = true;
                          } else {
                            el.value = '1';
                          }
                        }
                      }
                    }"""
                )
                submit_form(page)
                print(f"REPLY status=submitted bytes={len(page_content(page))}")
            else:
                print("REPLY FAIL: no form on exercise page")
                failures.append("reply form")

        # --- supervisor Notes (userscore) ---
        context2 = browser.new_context()
        page2 = context2.new_page()
        page2.goto(
            cgi_url(
                args.base_url,
                {
                    "module": "adm/class/classes",
                    "cmd": "new",
                    "lang": "fr",
                    "type": "authsupervisor",
                    "class": creds["class"],
                },
            ),
            wait_until="load",
            timeout=30000,
        )
        if page2.locator('input[name="auth_user"]').count():
            fill_login(page2, creds["supervisor_login"], creds["supervisor_password"])
            submit_form(page2)
        sup_sess = session_of(page2.url, page2)
        print(f"SUPERVISOR login={'PASS' if sup_sess else 'FAIL'} session={sup_sess or 'none'}")
        if not sup_sess:
            failures.append("supervisor login")
        else:
            page2.goto(
                cgi_url(
                    args.base_url,
                    {
                        "module": "adm/class/userscore",
                        "cmd": "new",
                        "lang": "fr",
                        "session": sup_sess,
                        "class": creds["class"],
                    },
                ),
                wait_until="load",
                timeout=30000,
            )
            text = page_content(page2)
            notes_ok = bool(
                re.search(r"(?i)notes|feuille|sheet|score|utilisateur|userscore", text)
            )
            print(f"NOTES open={'PASS' if notes_ok else 'FAIL'} bytes={len(text)}")
            if not notes_ok:
                failures.append("teacher Notes")

        context2.close()
        browser.close()

    if failures:
        print("PLAYWRIGHT FAIL: " + ", ".join(failures))
        return 1
    print("PLAYWRIGHT PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
