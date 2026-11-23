#!/usr/bin/env python3
"""GROK_V1 P4: create course p4pilot (prof p4prof, students stud1..stud3) in a WeBWorK 2.21 instance, copy the 10
generated OEF wrappers into templates/OEF/, import set P4pilot and assign it to every user.
Passwords: random, written chmod 600 into SECRETS_DIR (ww_prof_password, ww_student_password) if missing; never
printed. Crypt with perl crypt() (SHA-512).
GROK_V1_1: with --menu-wrappers BUILD/wrappers/generated_menu also copies the drop-down test wrapper (gate P13,
outside the pilot) and imports set P4menu. Needs WEBWORK_ROOT, PG_ROOT, WW_COURSES(optional) and a running DB.
Usage: create_p4pilot_course_GROK_V1.py --secrets DIR --wrappers BUILD/wrappers/generated
         [--menu-wrappers BUILD/wrappers/generated_menu] [--course p4pilot]"""
import argparse, os, secrets, shutil, subprocess, sys, tempfile
from pathlib import Path


def pw(sd, name):
    f = sd / name
    if not f.exists():
        fd = os.open(f, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600); os.write(fd, secrets.token_hex(12).encode()); os.close(fd)
    return f.read_text().strip()


def crypt(p):
    salt = secrets.token_hex(8)
    return subprocess.run(["perl", "-e", 'print crypt($ARGV[0], q{$6$}.$ARGV[1].q{$})', p, salt],
                          capture_output=True, text=True, check=True).stdout


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--secrets", required=True); ap.add_argument("--wrappers", required=True)
    ap.add_argument("--course", default="p4pilot"); ap.add_argument("--menu-wrappers")
    a = ap.parse_args()
    root = Path(os.environ["WEBWORK_ROOT"]); sd = Path(a.secrets); sd.mkdir(mode=0o700, exist_ok=True)
    prof, stud = pw(sd, "ww_prof_password"), pw(sd, "ww_student_password")
    rows = ["# Field order: student_id,last_name,first_name,status,comment,section,recitation,email_address,user_id,crypted_password,permission",
            f"p4prof,Prof,P4,C,,,,,p4prof,{crypt(prof)},10"] + [f"stud{i},Eleve,{i},C,,,,,stud{i},{crypt(stud)},0" for i in (1, 2, 3)]
    tmpd = Path(tempfile.mkdtemp(prefix="p4lst_")); os.chmod(tmpd, 0o700)
    lst = tmpd / f"{a.course}.lst"
    fd = os.open(lst, os.O_WRONLY | os.O_CREAT, 0o600); os.write(fd, ("\n".join(rows) + "\n").encode()); os.close(fd)
    try:
        subprocess.run(["perl", str(root / "bin/addcourse"), f"--users={lst}", "--professors=p4prof", a.course], check=True)
    finally:
        shutil.rmtree(tmpd)
    courses = Path(os.environ.get("WW_COURSES", root.parent / "courses"))
    tpl = courses / a.course / "templates"
    src = Path(a.wrappers)
    shutil.copytree(src / "OEF", tpl / "OEF", dirs_exist_ok=True)
    shutil.copy2(src / "setP4pilot.def", tpl / "setP4pilot.def")
    here = Path(__file__).resolve().parent
    subprocess.run(["perl", str(here / "Import_Set_P4_GROK_V1.pl"), a.course, "setP4pilot.def"], check=True)
    if a.menu_wrappers:
        m = Path(a.menu_wrappers)
        shutil.copytree(m / "OEF", tpl / "OEF", dirs_exist_ok=True)
        shutil.copy2(m / "setP4menu.def", tpl / "setP4menu.def")
        subprocess.run(["perl", str(here / "Import_Set_P4_GROK_V1.pl"), a.course, "setP4menu.def"], check=True)
    print(f"course {a.course} ready; passwords in {sd} (not printed)")


if __name__ == "__main__":
    sys.exit(main())
