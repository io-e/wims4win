#!/usr/bin/env python3
"""GROK_V1 P4 WeBWorK HTTP client (stdlib). Login, view a set problem, submit answers.
Adapted from LW's Submit_Answers_DEBIAN.py (port and secrets differ). Passwords are read from ../secrets, never printed."""
import html, os, re, urllib.parse, urllib.request
from pathlib import Path

P4 = Path(__file__).resolve().parents[1]
import sys as _s; _s.path.insert(0, str(Path(__file__).parent))
from p4env_GROK_V1 import WW_BASE as BASE, SECRETS


def secret(name):
    return (SECRETS / name).read_text().strip()


class WW:
    def __init__(self, course, user, pwfile):
        self.course, self.user, self.cookies = course, user, {}
        self.get(f"{BASE}/{course}", {"user": user, "passwd": secret(pwfile)})

    def get(self, url, data=None, timeout=120):
        body = urllib.parse.urlencode(data, doseq=True).encode() if data is not None else None
        req = urllib.request.Request(url, body)
        if self.cookies:
            req.add_header("Cookie", "; ".join(f"{k}={v}" for k, v in self.cookies.items()))
        try:
            resp = urllib.request.urlopen(req, timeout=timeout)
        except urllib.error.HTTPError as e:
            resp = e
        for h in resp.headers.get_all("Set-Cookie") or []:
            k, v = h.split(";", 1)[0].split("=", 1)
            self.cookies[k] = v
        self.status = resp.status if hasattr(resp, "status") else resp.code
        return resp.read().decode("utf-8", "replace")

    def url(self, set_id, prob):
        return f"{BASE}/{self.course}/{set_id}/{prob}/"

    def view(self, set_id, prob, **extra):
        u = self.url(set_id, prob)
        if extra:
            u += "?" + urllib.parse.urlencode(extra)
        return self.get(u)

    @staticmethod
    def form_of(page):
        main = page[page.find('id="problemMainForm"'):]
        main = main[: main.find("</form>")]
        hidden = {}
        for tag in re.findall(r'<input[^>]*type="hidden"[^>]*>', main):
            n = re.search(r'name="([^"]+)"', tag); v = re.search(r'value="([^"]*)"', tag)
            if n and n.group(1) not in hidden:
                hidden[n.group(1)] = html.unescape(v.group(1)) if v else ""
        names = []
        for n in re.findall(r'name="(AnSwEr\d{4}|MaTrIx_\w+)"', main):
            if n not in names:
                names.append(n)
        return {k: v for k, v in hidden.items() if not k.startswith("AnSwEr")}, names

    def submit(self, set_id, prob, answers, page=None, button="submitAnswers"):
        page = page or self.view(set_id, prob)
        form, names = self.form_of(page)
        form[button] = "Submit Answers"
        if isinstance(answers, dict):
            form.update(answers)
        else:
            for n, a in zip(names, answers):
                form[n] = a
        return self.get(self.url(set_id, prob), form)


def text_of(page):
    return html.unescape(re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", page)))
