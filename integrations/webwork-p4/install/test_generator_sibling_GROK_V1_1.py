# GROK_V1_1 P4 test (generator sibling) harness for Apply_WebWork4Win_Conf_P4_GROK_V1_1.py on scratch copies (no real config touched)
# Usage: copy a base generator (Windows win-original or Debian webwork4win-mariadb) into DIR with DIR/src/conf/localOverrides.conf.dist
# and DIR/src/pg/conf/pg_config.dist.yml (upstream .dist files); run: python3 test_generator_sibling_GROK_V1_1.py DIR
import os, sys, importlib.util
from pathlib import Path
d = Path(sys.argv[1]).resolve()
os.environ["P4_BASE_GENERATOR"] = str(d / "Apply_WebWork4Win_Conf_GROK_V1.py")
os.environ.setdefault("WW_DEBIAN", str(d)); os.environ["WEBWORK_ROOT"] = str(d / "src")
spec = importlib.util.spec_from_file_location("sib", str(Path(__file__).resolve().with_name("Apply_WebWork4Win_Conf_P4_GROK_V1_1.py")))
sib = importlib.util.module_from_spec(spec); spec.loader.exec_module(sib)
base = sib.load_base()
base.SRC, base.PG, base.RUNTIME = d / "src", d / "src/pg", d / "rt"
sib.patch(base)
cat = [{"name": "OPL", "label": "OPL", "type": "opl", "path": Path("/x/opl")},
       {"name": "Contrib", "label": "Contrib", "type": "contrib", "path": Path("/x/contrib")}]
for run in (1, 2):  # regenerate twice: the P4 lines must come back each time, once
    base.write_overrides(cat); base.write_pg(cat)
    lo = (d / "src/conf/localOverrides.conf").read_text(); pc = (d / "src/pg/conf/pg_config.yml").read_text()
    print("run", run, "LO OEFBridge lines:", lo.count("WeBWorK::OEFBridge"), "| push form:", "push @{$pg{modules}}" in lo,
          "| assignment $pg{modules} = (replace) added:", "$pg{modules} = [qw(WeBWorK" in lo,
          "| PG items:", pc.count("WeBWorK::OEFBridge"), "| generator's own block kept:", "GROK_V1_7" in lo)
