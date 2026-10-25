# Image Linux avec le moteur WIMS 4.31b + oef2html.py (pour Windows : Docker Desktop)
# docker build -t oef2html .   (le tarball wims_4.31b+dfsg1.orig.tar.gz doit etre dans ce dossier)
# docker run --rm -v "%cd%":/data oef2html /data/mes_oef --out /data/html_out
FROM debian:bookworm
RUN apt-get update && apt-get install -y --no-install-recommends build-essential autoconf flex bison \
    libgd-dev pari-gp maxima python3 && rm -rf /var/lib/apt/lists/*
COPY wims_4.31b+dfsg1.orig.tar.gz build_wims_runtime.sh oef2html.py /opt/oef2html/
COPY template /opt/oef2html/template
RUN cd /opt/oef2html && sh build_wims_runtime.sh wims_4.31b+dfsg1.orig.tar.gz wims-runtime

ENTRYPOINT ["python3", "/opt/oef2html/oef2html.py", "--wims-home", "/opt/oef2html/wims-runtime"]
