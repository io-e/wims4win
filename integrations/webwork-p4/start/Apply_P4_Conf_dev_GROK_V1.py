# GROK_V1 P4 copy of LW Apply_WebWork4Win_Conf_GROK_V1.py (GROK_V1_7+DEBIAN_PORT_1+MARIADB_1).
# P4 changes: own base /workspace/mw-p4-20261123/ww, WeBWorK 3020, MariaDB 3308, random Mojolicious secret from
# ../secrets, site lib dir + WeBWorK::OEFBridge module in pg_config.yml, no OEF pointer conf.
"""Write the Windows-local WeBWorK config, the same way wims4win points CAS at tests-win.

The upstream trees under src stay unmodified in Git. Local files that WeBWorK
already gitignores (conf/*.yml, site.conf, localOverrides.conf, pg_config.yml)
receive Windows paths. Strawberry Perl is the interpreter. No Docker, no WSL.
"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

# DEBIAN: paths come from env (start_debian.sh), defaults = /workspace/ww-debian layout.
BASE = Path(os.environ.get("WW_P4", "/workspace/mw-p4-20261123/ww"))
SRC = Path(os.environ.get("WEBWORK_ROOT", BASE / "src"))
PG = SRC / "pg"
RUNTIME = BASE / "p4"
COURSES = Path(os.environ.get("WW_COURSES", BASE / "courses"))
SECRETS = BASE.parent / "secrets"
DB_PASSWORD_FILE = SECRETS / "mariadb_webworkWrite_password"
MOJO_SECRET_FILE = SECRETS / "mojolicious_secret"
SITE_LIB = BASE / "site-lib"
DB_PORT = os.environ.get("WW_DB_PORT", "3308")
# MARIADB: site-level macro dir for the CCDMD/pg_fr_qc French macros (appended to macrosPath).
FR_MACROS = Path(os.environ.get("WW_FR_MACROS", BASE / "src_pg" / "pg_fr_qc_macros"))
# MARIADB_1: site macro dir searched right after the course macros dir, before PG's own dirs, so its
# parserCustomization.pl replaces PG's empty placeholder (decimal comma). Course templates/macros still wins.
SITE_MACROS = Path(os.environ.get("WW_SITE_MACROS", BASE / "site-macros"))
# DEBIAN: no MiKTeX/ImageMagick .exe; the /usr/bin defaults of the .dist files are kept.

PORT = int(os.environ.get("WW_PORT", "3020"))
LIBRARIES = RUNTIME / "course_libraries_P4_GROK_V1.yaml"
REPARSE_POINT = 0x400


def perl_path(path: Path) -> str:
    return str(path).replace("\\", "/")


def perl_squote(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"


def perl_quote(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def parse_scalar(raw: str):
    text = raw.strip()
    if " #" in text:
        text = text.split(" #", 1)[0].rstrip()
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "'\"":
        return text[1:-1]
    if text == "true":
        return True
    if text == "false":
        return False
    return text


def load_simple_yaml(path: Path) -> tuple[dict, list[dict]]:
    """The small subset used by course_libraries and programming_folders."""
    top: dict = {}
    items: list[dict] = []
    current: dict | None = None
    for line_no, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line.startswith("  - "):
            if current is not None:
                items.append(current)
            current = {}
            body = line[4:]
            if ":" not in body:
                raise SystemExit(f"{path}:{line_no}: expected key")
            key, value = body.split(":", 1)
            current[key.strip()] = parse_scalar(value)
            continue
        if line.startswith("    ") and current is not None:
            body = line.strip()
            if ":" not in body:
                raise SystemExit(f"{path}:{line_no}: expected key")
            key, value = body.split(":", 1)
            current[key.strip()] = parse_scalar(value)
            continue
        if line.startswith(" ") or ":" not in line:
            raise SystemExit(f"{path}:{line_no}: unsupported YAML")
        if current is not None:
            items.append(current)
            current = None
        key, value = line.split(":", 1)
        top[key.strip()] = parse_scalar(value)
    if current is not None:
        items.append(current)
    return top, items


def library_catalog() -> list[dict]:
    if not LIBRARIES.is_file():
        raise SystemExit(f"Missing {LIBRARIES}")
    top, items = load_simple_yaml(LIBRARIES)
    catalog: list[dict] = []
    for item in items:
        name = str(item.get("name", "")).strip()
        kind = str(item.get("type", "")).strip()
        label = str(item.get("label", name)).strip()
        raw_path = str(item.get("path", "")).strip()
        if not name or kind not in {"opl", "contrib", "problib"} or not raw_path:
            raise SystemExit(f"Bad library entry in {LIBRARIES.name}: {item}")
        catalog.append({"name": name, "label": label, "type": kind, "path": Path(raw_path)})
    pg_from = str(top.get("pg_from", "")).strip()
    if pg_from:
        source = Path(pg_from)
        if not source.is_file():
            raise SystemExit(f"pg_from file not found: {source}")
        _, folders = load_simple_yaml(source)
        for folder in folders:
            if folder.get("lang") != "pg" or folder.get("enabled") is False:
                continue
            name = str(folder.get("id", "")).strip()
            raw_path = str(folder.get("path", "")).strip()
            if not name or not raw_path:
                raise SystemExit(f"PG folder in {source.name} has no id or path")
            catalog.append(
                {
                    "name": name,
                    "label": str(folder.get("label", name)).strip(),
                    "type": "problib",
                    "path": Path(raw_path),
                }
            )
    names = [item["name"] for item in catalog]
    if len(names) != len(set(names)):
        raise SystemExit(f"Duplicate library name in {LIBRARIES.name}")
    for kind in ("opl", "contrib"):
        found = [item for item in catalog if item["type"] == kind]
        if len(found) > 1:  # DEBIAN: 0 allowed (OPL/Contrib not installed)
            raise SystemExit(f"{LIBRARIES.name} needs exactly one type: {kind}")
    return catalog


def root_of(catalog: list[dict], kind: str):
    return next((item["path"] for item in catalog if item["type"] == kind), None)


def problib_lines(catalog: list[dict]) -> str:
    rows = [
        f"\t{item['name']} => {perl_quote(item['label'])},"
        for item in catalog
        if item["type"] == "problib"
    ]
    return "\n".join(rows)


def is_reparse(path: Path) -> bool:
    # DEBIAN: NTFS junction == symlink here.
    return path.is_symlink()


def ensure_junction(link: Path, target: Path) -> str:
    # DEBIAN: os.symlink instead of "cmd /c mklink /J" + attrib.
    if not target.is_dir():
        raise SystemExit(f"Missing library target {target}")
    if link.is_symlink():
        if os.path.realpath(link) == os.path.realpath(target):
            return "kept"
        link.unlink()
    elif link.exists():
        if link.is_dir() and not any(link.iterdir()):
            link.rmdir()
        else:
            raise SystemExit(f"Refusing to replace non-empty {link}")
    link.symlink_to(target, target_is_directory=True)
    return "created"


# Junctions created by an earlier catalog. Removed when they are no longer listed,
# so they do not reappear under Course Files.
RETIRED_LINKS = ("suma_plus", "alf_pg", "library_plus", "OpenProblemLibrary_plus", "Contribution")


def remove_retired_links(templates: Path, catalog: list[dict]) -> None:
    names = {item["name"] for item in catalog}
    for name in RETIRED_LINKS:
        if name in names:
            continue
        link = templates / name
        if not is_reparse(link):
            continue
        link.unlink()
        print(f"removed {link.parent.parent.name}/templates/{name}")


def ensure_course_links(catalog: list[dict]) -> None:
    courses = COURSES
    if not courses.is_dir():
        return
    for course in sorted(path for path in courses.iterdir() if path.is_dir()):
        if course.name == "modelCourse":
            continue
        templates = course / "templates"
        if not templates.is_dir():
            continue
        remove_retired_links(templates, catalog)
        for item in catalog:
            state = ensure_junction(templates / item["name"], item["path"])
            print(f"{state} {course.name}/templates/{item['name']} -> {item['path']}")


def write_mojo() -> None:
    text = f"""---
# MARIADB_1 Debian MariaDB variant of the Windows overlay. Generated by Apply_WebWork4Win_Conf_GROK_V1.py.
secrets:
  - {MOJO_SECRET_FILE.read_text().strip()}
pg_dir: {perl_path(PG)}
server_user: ""
server_group: ""
# DEBIAN: Minion job queue on SQLite, as on Windows (upstream dist default is SQLite too).
MIN_HTML_ERRORS: 0
"""
    (SRC / "conf" / "webwork2.mojolicious.yml").write_text(text, encoding="utf-8", newline="\n")


def write_site() -> None:
    src = (SRC / "conf" / "site.conf.dist").read_text(encoding="utf-8")
    password = DB_PASSWORD_FILE.read_text().strip()
    tools = {
        '$server_root_url   = \'\';': f"$server_root_url   = 'http://127.0.0.1:{PORT}';",
        # MARIADB: upstream driver (MariaDB), database (webwork), user (webworkWrite) and DSN block are kept.
        # Only the port, TCP instead of socket, the password and the courses dir change.
        '$database_port="3306";': f'$database_port="{DB_PORT}";',
        "$database_password ='passwordRW';": f"$database_password ={perl_squote(password)};",
        '$webwork_courses_dir = "/opt/webwork/courses"; # a typical place to put course directories': f'$webwork_courses_dir = "{perl_path(COURSES)}";',
        "$database_use_socket_if_localhost=1;": "$database_use_socket_if_localhost=0;  # MARIADB_1 TCP 127.0.0.1",
    }
    for old, new in tools.items():
        if old not in src:
            raise SystemExit(f"site.conf.dist no longer contains: {old}")
        src = src.replace(old, new, 1)
    src = src.replace(
        "# GROK_V1",
        "# GROK_V1",
        1,
    )
    if not src.startswith("# GROK_V1"):
        src = "# GROK_V1 P4. MariaDB on 127.0.0.1:" + DB_PORT + ". Regenerated from site.conf.dist. Do not edit by hand.\n" + src
    (SRC / "conf" / "site.conf").write_text(src, encoding="utf-8", newline="\n")


def write_overrides(catalog: list[dict]) -> None:
    dest = SRC / "conf" / "localOverrides.conf"
    text = (SRC / "conf" / "localOverrides.conf.dist").read_text(encoding="utf-8")
    opl, contrib = root_of(catalog, "opl"), root_of(catalog, "contrib")
    opl_lines = "\n".join(
        ([f'$problemLibrary{{root}} = "{perl_path(opl)}";'] if opl else ["# DEBIAN: no OPL installed"])
        + ([f'$contribLibrary{{root}} = "{perl_path(contrib)}";'] if contrib else ["# DEBIAN: no Contrib installed"])
    )
    fr_line = (f'$pg{{directories}}{{macrosPath}} = [@{{$pg{{directories}}{{macrosPath}}}}, "{perl_path(FR_MACROS)}"];'
               if FR_MACROS.is_dir() else "# no French macro dir")
    oef_conf = SRC / "conf" / "oef_pointers.conf.P4-disabled"
    oef_line = 'include("conf/oef_pointers.conf");' if oef_conf.is_file() else "# no conf/oef_pointers.conf"
    site_line = (f'splice(@{{$pg{{directories}}{{macrosPath}}}}, 2, 0, "{perl_path(SITE_MACROS)}");'
                 if SITE_MACROS.is_dir() else "# no site macro dir")
    # GROK_V1 P4: under webwork2 the course environment's $pg{modules} (defaults.config) replaces the pg_config.yml
    # list (WeBWorK::PG::Environment deepCopy), so the trusted module must also be pushed here (site file only).
    mods = []
    if (SITE_LIB / "WeBWorK" / "OEFBridge.pm").is_file():
        mods.append("push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)]; # GROK_V1 P4 trusted module")
    if os.environ.get("WW_P4_PROBE") == "1" and (SITE_LIB / "WeBWorK" / "P4Probe.pm").is_file():
        mods.append("push @{$pg{modules}}, [qw(WeBWorK::P4Probe)]; # GROK_V1 P4 A3.3 step-1 probe")
    mod_lines = "\n".join(mods) or "# no P4 trusted module"
    block = f"""
# GROK_V1_7 library roots from course_libraries_GROK_V1.yaml.
# Loopback pilot: no authenticator app, so the two-factor page stays off.
$twoFA{{enabled}} = 0;
# Interface language. The Open Problem Library exercises stay in English.
$language = "fr";
{opl_lines}
$problemLibrary{{version}} = "2.5";
# MARIADB_1: CCDMD/pg_fr_qc French macros (*Fr.pl), site level, searched after the PG dirs.
{fr_line}
# MARIADB_1: site macros (parserCustomization.pl = decimal comma) after "." and the course macros dir.
{site_line}
# Buttons appear only when the course templates directory has the junction.
$courseFiles{{problibs}} = {{
{problib_lines(catalog)}
}};
# OEF_1 (after problibs, which it extends): études PG -> OEF (WIMS). Generated by oef-pointer/oef_etudes_GROK_V1.py conf.
{oef_line}
{mod_lines}

"""
    marker = "1; #final line of the file to reassure perl that it was read properly."
    if marker not in text:
        raise SystemExit("localOverrides.conf.dist lost its final true value")
    text = text.replace(marker, block + marker, 1)
    if not text.startswith("# GROK_V1"):
        text = "# GROK_V1 copied from localOverrides.conf.dist plus the library roots.\n" + text
    dest.write_text(text, encoding="utf-8", newline="\n")


def write_pg(catalog: list[dict]) -> None:
    text = (PG / "conf" / "pg_config.dist.yml").read_text(encoding="utf-8")
    tmp = perl_path(RUNTIME / "tmp")
    opl, contrib = root_of(catalog, "opl"), root_of(catalog, "contrib")
    repl = {"  tmp: /tmp": f"  tmp: {tmp}"}
    # MARIADB_1: same French macro dir for headless PG renders (no course environment).
    if SITE_MACROS.is_dir():
        repl["    - .\n    - $pg_root/macros\n"] = f"    - .\n    - {perl_path(SITE_MACROS)}\n    - $pg_root/macros\n"
    if FR_MACROS.is_dir():
        repl["    - $pg_root/macros/deprecated\n"] = f"    - $pg_root/macros/deprecated\n    - {perl_path(FR_MACROS)}\n"
    # DEBIAN: OPL/Contrib only if listed; all tool paths keep the /usr/bin defaults.
    if opl:
        repl["  OPL: /opt/webwork/libraries/webwork-open-problem-library/OpenProblemLibrary"] = f"  OPL: {perl_path(opl)}"
    if contrib:
        repl["  Contrib: /opt/webwork/libraries/webwork-open-problem-library/Contrib"] = f"  Contrib: {perl_path(contrib)}"
    for old, new in repl.items():
        if old not in text:
            raise SystemExit(f"pg_config.dist.yml no longer contains: {old}")
        text = text.replace(old, new, 1)
    # GROK_V1 P4: trusted module WeBWorK::OEFBridge (site lib dir on PERL5LIB), loaded like the upstream modules.
    if (SITE_LIB / "WeBWorK" / "OEFBridge.pm").is_file():
        anchor = "modules:\n"
        if anchor not in text:
            raise SystemExit("pg_config.dist.yml lost its modules: list")
        text = text.replace(anchor, anchor + "  - ['WeBWorK::OEFBridge'] # GROK_V1 P4 trusted module\n", 1)
    text = "# GROK_V1 P4. Regenerated from pg_config.dist.yml.\n" + text
    (PG / "conf" / "pg_config.yml").write_text(text, encoding="utf-8", newline="\n")


def write_shim() -> None:
    # DEBIAN: real /usr/bin/pdf2svg, no .cmd shim. Only the tmp dir is needed.
    (RUNTIME / "tmp").mkdir(parents=True, exist_ok=True)
    COURSES.mkdir(parents=True, exist_ok=True)


def main() -> None:
    catalog = library_catalog()
    links_only = "--links" in sys.argv
    ensure_course_links(catalog)
    if links_only:
        return
    write_shim()
    write_mojo()
    write_site()
    write_overrides(catalog)
    write_pg(catalog)
    print("webwork4win config written")


if __name__ == "__main__":
    main()
