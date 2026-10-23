DESTDIR=

MANSOURCES = $(shell ls doc/source/*.rst)
DOCDIR = $(DESTDIR)/usr/share/doc/units-filter/html

all :   units-filter.1 units-filter.html
	gzip -9 -c units-filter.1 > units-filter.1.gz
	(cd src; ${MAKE} all)
test : all
	(cd src; ${MAKE} test)

clean :
	rm -f *~ *stamp *.gz
	(cd src; ${MAKE} clean)
	(cd doc; ${MAKE} clean)

install : all
	install -m 775 -d ${DESTDIR}/usr/bin
	install -m 775 -d ${DESTDIR}/usr/share/man/man1
	install -m 755 src/units-filter ${DESTDIR}/usr/bin
	install -m 644 units-filter.1.gz ${DESTDIR}/usr/share/man/man1
	install -m 775 -d $(DOCDIR)
	cp -a units-filter.html _static $(DOCDIR)
	# replace a few JS files by symlinks for a Debian build
	if grep -q Debian /etc/issue; then \
	  rm -f $(DOCDIR)/_static/jquery.js; \
	  ln -s /usr/share/javascript/jquery/jquery.js $(DOCDIR)/_static; \
	  rm -f $(DOCDIR)/_static/underscore.js; \
	  ln -s /usr/share/javascript/underscore/underscore.js $(DOCDIR)/_static; \
	fi

uninstall :
	rm -f ${DESTDIR}/usr/bin/units-filter
	rm -f ${DESTDIR}/usr/share/man/man1/units-filter.1.gz
	rm -rf $(DOCDIR)

units-filter.1: $(MANSOURCES)
	make -C doc man
	cp doc/build/man/units-filter.1 .

units-filter.html:  $(MANSOURCES)
	make -C doc singlehtml
	cp doc/build/singlehtml/index.html $@
	cp -a doc/build/singlehtml/_static .
