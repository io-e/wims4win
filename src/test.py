#!/usr/bin/python3

import os, os.path, tempfile, re, copy, subprocess, locale

titlePattern = re.compile(r"^== (.*) ==$")
varPattern = re.compile(r"^(.*) == (.*)$")
rulePattern = re.compile(r"^----.*$")
commentPattern = re.compile(r"^\s*#.*$")
codingPattern = re.compile(r"^# -\*- coding: (.*) -\*-$")

locale.setlocale(locale.LC_ALL, '')
codeset = locale.nl_langinfo(locale.CODESET)  # coding used by the user's locale


def notAtest(f):
    """
    consider some filenames as not useable to perfom tests.
    @param f a filename
    @return True if f seems to be the name of a backup file
    """
    return re.match(r".*~", f) or re.match(r".*\.bak", f)


def mkTestList(lines, cur, title, coding="utf-8"):
    """
    makes a list of objects for unitary tests of Wims modtool primitives
    @param lines a list of lines coming from a file
    @param cur a pointer on the current line to be read
    @param title a title which has been read from a previous line
    @param coding the coding of the .proc file used for tests
    @return a list of Test objects
    """
    result = []
    proclines = []
    variables = {}
    currentVar = None
    while cur < len(lines):
        l = lines[cur].decode(coding).replace('\n', '')
        t = titlePattern.match(l)
        v = varPattern.match(l)
        r = rulePattern.match(l)
        c = commentPattern.match(l)
        if t:
            result.append(Test(title, proclines, variables, coding))
            title = t.group(1)
            proclines = []
            variables = {}
            currentVar = None
        elif v:
            currentVar = v.group(1)
            variables[currentVar] = v.group(2)
        elif currentVar is not None and len(l) > 0:
            variables[currentVar] += '\n' + l
        elif not r and not c:
            # not a title, nor variable, nor a ruler, nor a comment:
            # these are commands
            proclines.append(lines[cur].strip())
        cur = cur + 1
    result.append(Test(title, proclines, variables, coding))
    return result


class Test:
    """
    This class implements data for a unitary test of Wims.
    Each test has a title, a set of lines to be pasted into
    a .proc file for Wims, and a set assertions about
    variables and the values which they should be assigned
    upon completion of the test.
    """
    def __init__(self, title, proclines, variables, coding="utf-8"):
        """
        The constructor.
        @param title a title for the unitary test
        @param proclines a list of lines to be processed by Wims in a
        .proc file.
        @param variables a dictionary associating variable names with
        values they should have when the test finishes.
        @param coding the coding used for the .proc file
        """
        self.title = "" + title
        self.proclines = copy.copy(proclines)
        self.variables = copy.copy(variables)
        self.coding = coding
        self.gotResults = {}
        self.errors = []
        self.success = None

    def __str__(self):
        """
        Conversion to a str.
        """
        result = "Test {title=«%s»," % (self.title,)
        result += " coding=%s," % self.coding
        result += " proclines=%s," % self.proclines
        result += " variables=%s" % self.variables
        result += "}"
        return result

    def __repr__(self):
        """
        The representation is the same as the conversion to a str.
        """
        return self.__str__()

    def gotError(self):
        """
        checks whether some error occurred
        @return True if an error came out of the wims subprocess
        """
        return len(self.errors) > 1 or (len(self.errors) == 1 and
                                        self.errors[0] != "")

    def run(self, path, fName="test.proc"):
        """
        runs the unitary tests, and gather the results in
        self.gotResults, self.errors and self.success
        @param path a path to the .proc file
        @param fName the name of the .proc file, defaults to "test.proc"
        """
        self.success = True
        self.failedVars = []
        cmd = "./wims test %s %s '%s'" % (path,
                                          fName,
                                          ' '.join([v for v in self.variables]))
        with open(os.path.join(path, fName), "wb") as outfile:
            for l in self.proclines:
                outfile.write(l + b'\n')
            outfile.close()
            p = subprocess.Popen(cmd,
                                 shell=True,
                                 stdout=subprocess.PIPE,
                                 stderr=subprocess.PIPE)
            out, err = p.communicate()
            currentVar = None
            for l in out.decode(self.coding).split("\n"):
                v = varPattern.match(l)
                if v:
                    currentVar = v.group(1)
                    self.gotResults[currentVar] = v.group(2)
                else:
                    if currentVar is not None and len(l) > 0:
                        # add an other line to the current variable
                        self.gotResults[currentVar] += '\n' + l
            self.errors = err.decode(codeset).split("\n")
            if self.gotError():
                self.success = False
            for v in self.variables:
                if v not in self.gotResults or self.variables[v] != self.gotResults[v]:
                    self.failedVars.append(v)
                    self.success = False

    def showResult(self, msg="", tmpDir="/tmp"):
        """
        runs the test if self.success is undefined, and
        pretty-prints the result
        @param msg a message string to prepend to the result (for instance,
        a test number)
        @param tmpDir a directory to run the tests
        """
        if self.success is None:
            self.run(tmpDir)
        if self.success:
            print("[%s] %s: OK." % (msg, self.title))
        else:
            hrule = "[%s] ========< %s >==========" % (msg, self.title)
            print(hrule)
            for l in self.proclines:
                print(l)
            hrule = "=" * len(hrule)
            print(hrule)
            if self.gotError():
                print ("ERROR: %s" % self.errors)
            for v in self.failedVars:
                if v in self.gotResults:
                    print("FAILED for variable %s, expected: «%s»; got: «%s»"
                          % (v, self.variables[v], self.gotResults[v]))
                else:
                    print("FAILED for variable %s, expected: «%s»; got nothing"
                          % (v, self.variables[v]))
                for v in self.variables:
                    if v not in self.failedVars:
                        print("OK for variable %s,  expected: «%s»; got: «%s»"
                              % (v, self.variables[v], self.gotResults[v]))
                print(hrule)


if __name__ == "__main__":
    print ("Test suite for Wims.")
    for dirpath, dirnames, filenames in os.walk("test"):
        for f in filenames:
            if notAtest(f):
                continue
            lines = open(os.path.join(dirpath, f), "rb").readlines()
            cur = 0
            t = titlePattern.match(lines[cur].decode("utf-8"))
            coding = "utf-8"  # default
            while cur < len(lines) and not t:
                # take in account coding declaration
                c = codingPattern.match(lines[cur].decode("utf-8"))
                if c:
                    coding = c.group(1)
                cur += 1
                t = titlePattern.match(lines[cur].decode(coding))
            if cur < len(lines):
                print("Running tests from {}/{} (coding: {})".format(dirpath, f, coding))
                title = t.group(1)
                tests = mkTestList(lines, cur, title, coding=coding)
                with tempfile.TemporaryDirectory(prefix='wimstest-') as tmpDir:
                    i = 1
                    ok = 0
                    nok = 0
                    for t in tests:
                        t.showResult(tmpDir=tmpDir, msg=i)
                        i += 1
                        if t.success:
                            ok += 1
                        else:
                            nok += 1
                print ("Ran {} tests; {} OK, {} WRONG.".format(i - 1, ok, nok))
            else:
                print("Error: the first line of the file should contain some == title ==")
                os.exit(1)
