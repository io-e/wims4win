#! /usr/bin/python3

"""
This utility is part of chemeq's source package. It is used to
create a table of Mendeliev in HTML format with data coming from
the file mendeleiev.cc

Chemeq is a basic standalone filter written in C language,
flex and bison. It inputs strings like:

`2H2 + O2 ---> 2 H2O`

and can outputs LaTeX code and messages about the equilibrium of a
chemical reaction.
----------------------------------------

Copyright (c) 2023 Georges Khaznadar
License: GPL V3+

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""

import sys, re
from bs4 import BeautifulSoup as BS
from subprocess import call
import jinja2

niveaux_electroniques = [
    ["1s"],
    ["2s", "2p"],
    ["3s", "3p", "3d"],
    ["4s", "4p", "4d", "4f"],
    ["5s", "5p", "5d", "5f", "..."],
    ["6s", "6p", "6d", "..."],
    ["7s", "7p", "7d", "..."],
]

def regle_klechkovski():
    result = []
    for i in range(8):
        col = i; row = 0
        while col >=0:
            if row < 7 and col < len(niveaux_electroniques[row]):
                result.append(niveaux_electroniques[row][col])
            col -= 1; row += 1
    return result

def mendel_rows():
    """
    Renvoie un dictionnaire qui associe un niveau à la liste des éléments qui
    viennent peupler ce niveau d'électrons supplémentaires
    """
    capacites = {
        "s": 2,
        "p": 6,
        "d": 10,
        "f": 14,
        ".": 0, # pas d'électrons quand on a marqué "..."
    }
    result = {}
    Z = 1
    row = "0"
    for structure in regle_klechkovski():
        if structure[0] > row:
            row = structure[0]
            result[row] = {}
        if structure not in result[row]:
            result[row][structure]=[]
        for i in range(capacites[structure[1]]):
            result[row][structure].append(Z)
            Z += 1
    return result

def layerordering(layer):
    return layer.replace("s","1").replace("p","2").replace("d","3").replace("f","4")

class Element:
    def __init__(self, name= None, symbol = None, Z = None, mass = None,
                 elec_structure = None, mendel_row = None,
                 mendel_col = None, mendel_family = None,
                 parent_noble_gas = ""):
        self.name = name
        self.symbol= symbol
        self.Z = int(Z)
        self.mass = float(mass)
        self.elec_structure = elec_structure
        self.mendel_row = mendel_row
        self.mendel_col = mendel_col
        self.mendel_family = mendel_family
        self.parent_noble_gas = parent_noble_gas
        return

    def __str__(self):
        return "{Z} {symbol} {name} {mass}, row = {mendel_row}, col = {mendel_col}, elec. struct. {struc}".format(
            struc = ("[" + self.parent_noble_gas + "] " if \
                     self.parent_noble_gas else "") + \
            self.elec_structure_string,
            **self.__dict__
        )

    def toHtml(self):
        """
        Crée un DIV pour une page HTML, avec les classes qui permettent un
        placement correct, si on dispose de la bonne feuille de style.
        """
        soup = BS("""<div class="element c{mendel_col} r{mendel_row}" style="cursor: wait;"></div>""".format(**self.__dict__), features="lxml")
        def addPara(theclass, text):
            p = soup.new_tag("p")
            p["class"] = theclass
            p.string = str(text)
            soup.div.append(p)
            return p
        addPara("symbol", self.symbol)
        addPara("Z", self.Z)
        addPara("mass", self.mass)
        struc = addPara("str0", ("[" + self.parent_noble_gas + "] " if \
                     self.parent_noble_gas else ""))
        for layer in self.elec_structure:
            span = soup.new_tag("span")
            span["class"] = "layer"
            m = re.match(r'(.*)\^\{(.*)\}', layer)
            span.string = m.group(1)
            struc.extend([span, " "])
            sup = soup.new_tag("sup")
            sup.string = m.group(2)
            span.append(sup)
        soup.div["title"] = str(self)
        return str(soup.div)

    @property
    def elec_structure_string(self):
        return " ".join(sorted(self.elec_structure, key = layerordering))
    
    @staticmethod
    def get_table():
        """
        Renvoie une liste d'instances d'Element, avec toutes les données
        nécessaire pour dessiner une table de Mendéléiev
        """
        result = []
        mr = mendel_rows()
        data={}
        elt_pattern = re.compile(r'\s+.\{\{N_\("(.*)"\),\s*"(.*)",\s*"(.*)",\s*"(.*)".*')
        with open("mendeleiev.cc") as infile:
            for l in infile.readlines():
                m = elt_pattern.match(l)
                if m:
                    data[int(m.group(3))] = {
                        "name": m.group(1),
                        "symbol": m.group(2),
                        "Z": m.group(3),
                        "mass": m.group(4),
                    }
        parent_noble_gas = ""
        mendel_col = 0
        for row in sorted(mr.keys()):
            elts = mr[row]
            substructure = []
            for struct, z_list in elts.items():
                for i, z in enumerate(z_list):
                    # on détermine la colonne
                    if z == 1: mendel_col = 1
                    elif z== 2: mendel_col = 18
                    elif struct[1] == "s": mendel_col = 1  + i
                    elif struct[1] == "d": mendel_col = 3  + i
                    elif struct[1] == "p": mendel_col = 13 + i
                    elif struct[1] == "f": mendel_col = 4 + i
                    # exceptions : Lu et Lr
                    # après les lanthanides et les actinides, respectivement
                    if z == 71 or z == 103: mendel_col = 18
                    # on détermine la ligne
                    mendel_row = row
                    if struct[1] == "f":
                        if z >= 89: # actinide
                            mendel_row = 9
                        elif z >= 57: # lanthanide
                            mendel_row = 8
                    # exceptions pour la fin des lantanides et des actinides
                    if z == 71:
                        mendel_row = 8 # Lu
                    elif z == 103:
                        mendel_row = 9 # Lr                            
                    # on détermine le gaz noble parent
                    if z > 86: parent_noble_gas = "Rn"
                    elif z > 54: parent_noble_gas = "Xe"
                    elif z > 36: parent_noble_gas = "Kr"
                    elif z > 18: parent_noble_gas = "Ar"
                    elif z > 10: parent_noble_gas = "Ne"
                    elif z > 2: parent_noble_gas = "He"
                    # on finit de construire l'instance d'Element
                    structure = substructure + [struct+'^{'+str(i+1)+'}']
                    elt = Element(
                        elec_structure = structure,
                        mendel_col = mendel_col,
                        mendel_row = mendel_row,
                        parent_noble_gas = parent_noble_gas,
                        **data[z]
                    )
                    result.append(elt)
                substructure.append(struct+'^{'+str(len(z_list))+'}')
        return result

    @staticmethod
    def get_table_html(css=True, js=False, standalone=False, tocxx = False,
                       compress = True):
        """
        Crée un élément HTML prêt à insérer dans une page, ou une page
        HTML complète.
        @param css fournit un élément de stryle, pour le placement des
                   DIVs de chaque élément (True par défaut)
        @param js fournit un script javascript pour augmenter les
                   facilités offertes par le tableau (False par défaut)
        @param standalone fait une page HTML complète (False par défaut)
        @param tocxx crée du code valide pour C++11 s'il est
                   positionné à vrai. Faux par défaut.
        @param compress minifiera le code CSS ; (True par défaut)
        """
        result=""
        # éléments de style
        if css:
            """
            création à partir d'un modèle et des constantes ci-après
            """
            # DIV pour chaque élément
            div_width = 44
            div_height = 56
            # espacements verticaux
            top_margin = 20
            row_gap = 6
            row_extra_gap = 10 # supplément pour les lanthanides et actinides
            # espacements horizontaux
            left_margin = 20
            col_gap = 6
            col_extra_gap = 6 # supplément pour les lanthanides et actinides
            # caractéristiques du symbole
            symbol_top = -18
            symbol_font_size = 26
            # caractéristiques du numéro atomique
            Z_top = -16
            Z_left = 2
            Z_font_size = 14
            # caractéristiques de la masse molaire
            mass_top = 28
            mass_font_size = 12
            # caractéristiques de la structure électronique
            struct_display = "inherit"
            struct_top = -4
            struct_left = 26
            struct_font_size = 4
            struct_line_height = 3.6
            row_enum = [{"i": i+1,
                         "top": top_margin + i * (row_gap + div_height) + \
                         (row_extra_gap if i > 6 else 0)} \
                        for i in range(9)]
            col_enum = [{"i": i+1,
                         "left": left_margin + i * (col_gap + div_width) + \
                         (col_extra_gap if i > 1 else 0)} \
                        for i in range(18)]
            total_width = 18 * div_width + 17 * col_gap + col_extra_gap + \
                2 * left_margin
            total_height = 9 * div_height + 8 * row_gap + row_extra_gap + \
                2 * top_margin
            ################################
            dict = { # pour formater le fichier modèle mendeleiev_template.css
                "div_width": div_width,
                "div_height": div_height,
                "symbol_top": symbol_top,
                "symbol_font_size": symbol_font_size,
                "Z_top": Z_top,
                "Z_left": Z_left,
                "Z_font_size": Z_font_size,
                "mass_top": mass_top,
                "mass_font_size": mass_font_size,
                "struct_display": struct_display,
                "struct_top": struct_top,
                "struct_left": struct_left,
                "struct_font_size": struct_font_size,
                "struct_line_height": struct_line_height,
                "row_enum": row_enum,
                "col_enum": col_enum,
            }
            ################################
            tm = jinja2.Template(open("mendeleiev_template.css").read())
            css_code = tm.render(**dict)
            if compress:
                from cssmin import cssmin
                css_code = cssmin(css_code, wrap = 72)
            result += "<style>\n" + css_code + "</style>\n"
            
        # script interactif
        if js:
            with open("mendeleiev.js") as infile:
                result += "<script>\n" + infile.read() + "</script>\n"
        # suite de DIVs, un par élément
        result += "\n".join([elt.toHtml() for elt in Element.get_table()]) + "\n"
        # enrobage dans un DIV commun
        result = """\
<div id="mendeleiev_table" style="position: relative; width: {w}px; height: {h}px;">{r}</div>
<div id="one_element"></div>
<div id="wait" style="width:500; text-align: center;
     background: rgba(200,200,255,0.5); position: absolute;
     top: 50px; left: 200px; border: 1px navy solid; border-radius: 3px;">
        Wait a few seconds...
</div>
""".format(r=result, w=total_width, h=total_height)
        if standalone:
            result = """\
<html>
  <head>
     <title>Mendeleiev table</title>
     <meta charset="UTF-8"/>
  </head>
  <body>
    {r}
  </body>
</html>
""".format(r = result)
        elif tocxx:
            cxxcode = 'const char * html_table = '
            for line in result.split("\n"):
                line = line.replace('"', r'\"') + r'\n'
                cxxcode += '"'+line+'"\n'
            cxxcode =   cxxcode[:-1] + ";\n"
            result = cxxcode
        return result

def create_cxx_source( target = "html_table.cc"):
    """
    Écrit une source valide pour C++11, qui définit un élément HTML valide
    contenant une table périodique des éléments, qu'on peut insérer dans une
    page HTML.
    """
    print("Écriture dans le fichier", target)
    with open(target,"w") as outfile:
        outfile.write(Element.get_table_html(tocxx=True, js=True))
    return

def demo():
    print("succession des niveaux selon la règle de Klechkovski")
    print(regle_klechkovski())
    print("struture de données pour la table de classification périodique")
    print(mendel_rows())
    print("Écriture dans le fichier /tmp/mendeleiev.html")
    with open("/tmp/mendeleiev.html","w") as outfile:
        outfile.write(Element.get_table_html())
    return

if __name__ == "__main__":
    create_cxx_source()
    # demo()
