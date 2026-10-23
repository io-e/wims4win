// -*- coding: utf-8 -*-
#include "chemeq.h"
#include <cmath>
#include <cstdlib>

atome lesatomes[] ={
{-1, "e"},
{1, "H"},
{2, "He"},
{3, "Li"},
{4, "Be"},
{5, "B"},
{6, "C"},
{7, "N"},
{8, "O"},
{9, "F"},
{10, "Ne"},
{11, "Na"},
{12, "Mg"},
{13, "Al"},
{14, "Si"},
{15, "P"},
{16, "S"},
{17, "Cl"},
{18, "Ar"},
{19, "K"},
{20, "Ca"},
{21, "Sc"},
{22, "Ti"},
{23, "V"},
{24, "Cr"},
{25, "Mn"},
{26, "Fe"},
{27, "Co"},
{28, "Ni"},
{29, "Cu"},
{30, "Zn"},
{31, "Ga"},
{32, "Ge"},
{33, "As"},
{34, "Se"},
{35, "Br"},
{36, "Kr"},
{37, "Rb"},
{38, "Sr"},
{39, "Y"},
{40, "Zr"},
{41, "Nb"},
{42, "Mo"},
{43, "Tc"},
{44, "Ru"},
{45, "Rh"},
{46, "Pd"},
{47, "Ag"},
{48, "Cd"},
{49, "In"},
{50, "Sn"},
{51, "Sb"},
{52, "Te"},
{53, "I"},
{54, "Xe"},
{55, "Cs"},
{56, "Ba"},
{57, "La"},
{58, "Ce"},
{59, "Pr"},
{60, "Nd"},
{61, "Pm"},
{62, "Sm"},
{63, "Eu"},
{64, "Gd"},
{65, "Tb"},
{66, "Dy"},
{67, "Ho"},
{68, "Er"},
{69, "Tm"},
{70, "Yb"},
{71, "Lu"},
{72, "Hf"},
{73, "Ta"},
{74, "W"},
{75, "Re"},
{76, "Os"},
{77, "Ir"},
{78, "Pt"},
{79, "Au"},
{80, "Hg"},
{81, "Tl"},
{82, "Pb"},
{83, "Bi"},
{84, "Po"},
{85, "At"},
{86, "Rn"},
{87, "Fr"},
{88, "Ra"},
{89, "Ac"},
{90, "Th"},
{91, "Pa"},
{92, "U"},
{93, "Np"},
{94, "Pu"},
{95, "Am"},
{96, "Cm"},
{97, "Bk"},
{98, "Cf"},
{99, "Es"},
{100, "Fm"},
{101, "Md"},
{102, "No"},
{103, "Lr"},
{104, "Rf"},
{105, "Db"},
{106, "Sg"},
{107, "Bh"},
{108, "Hs"},
{109, "Mt"},
{110, "Ds"},
{111, "Rg"},
{112, "Cn"},
{113, "Nh"},
{114, "Fl"},
{115, "Mc"},
{116, "Lv"},
{117, "Ts"},
{118, "Og"},
{0,"fin"}
};

std::ostream & Compteur::operator << (std::ostream & o)const{
  const_iterator i=begin(); 
  while (i != end()){
    o << i->first << " : " << i->second;
    if (++i != end()) o << " ";
  }
  return o;
}

std::ostream & operator << (std::ostream & o, const Compteur & c){
  return c.operator << (o);
}

double AtomeListe::weight(fraction mult)const{
  // const AtomeListe * al;
  double w=0.0;
  if(Zed!=0 && Zed!=-1 ){ /* cas où ce n'est pas un groupe ou un électron */
    w=mendelweight(symb)*nb*mult.i/mult.d;
  }
  else if (Zed==0){ /* cas d'un groupe */
    if (group) w+=group->weight(mult*nb);
  }
  if (suiv) w+=suiv->weight(mult);
  return w;
}

void AtomeListe::compte(Compteur &c, fraction mult)const{
  // const AtomeListe * al;
  if(Zed!=0 && Zed!=-1 ){ /* cas où ce n'est pas un groupe ou un électron */
    std::string key(symb);
    c[key] +=1.0*nb*mult.i/mult.d;
  }
  else if (Zed==0){ /* cas d'un groupe */
    if (group) group->compte(c,mult*nb);
  }
  if (suiv) suiv->compte(c,mult);
}

void AtomeListe::numerote(int n){
  if(Zed!=0){ /* cas où ce n'est pas un groupe */
    no = n;
  }
  else if (Zed==0){ /* cas d'un groupe */
    no = n;
    if (group) group->numerote();
  }
  if (suiv) suiv->numerote(n+1);
}

AtomeListe * AtomeListe::triage(AtomeListe * al){
  AtomeListe * al1;
  if(al->Z()!=0){ /* cas où ce n'est pas un groupe */
    if (al->suiv){
      al->suiv = triage(al->suiv);
    }
    while (al->suiv && al->suiv->Zed!=0 && 
	   strcmp(al->symbole(), al->suiv->symbole()) > 0){
      al1=al; al=al->suiv; al1->suiv=al->suiv; al->suiv=triage(al1);
    }
  }
  else{ /* cas d'un groupe */
     if (al->groupe()) al->groupe(triage(al->groupe()));
  }
  return al;
}

bool AtomeListe::isEqual(const AtomeListe & a2) const {
  std::stringstream s1, s2;
  printnorm(s1);
  a2.printnorm(s2);
  return s1.str() == s2.str();
}

void AtomeListe::printcount(std::ostream & o, const fraction& n, int multiple=1) const{
  if(Zed!=0){ /* cas où ce n'est pas un groupe */
    o << symb;
    o << ':' << n << '*' << multiple*nb;
  }
  else{ /* cas d'un groupe */
    if (group) group->printcount(o,n,nb);
  }  
  if (suiv) {o << ' '; suiv->printcount(o,n,multiple);}
}

void AtomeListe::printnorm(std::ostream & o) const{
  if (sqbr) o << "[";
  if(Zed!=0){ /* cas où ce n'est pas un groupe */
    o << symb;
    if (nb!=1) o << nb;
  }
  else{ /* cas d'un groupe */
    o << "(";
    if (group) group->printnorm(o);
    o << ")" << nb;
  }  
  if (suiv) suiv->printnorm(o);
  if (sqbr) o << "]";
}

std::ostream & operator << (std::ostream & o, const AtomeListe & l){
  int n;
  const AtomeListe * al;
  if (l.sq()) o << "[";
  if(l.Z()>0 || l.Z()<-1){
    o << "\\mathrm{" << l.symbole() << "}";
  }
  else if (l.Z()==-1){ // cas de l'électron
    o << "\\mathrm{e}";
  }
  else{                // cas des groupes parenthésés
    o << "(";
    if((al=l.groupe())) o << *al;
    o << ")";
  }
  if((n=l.getmolecularite())>1) o << "_{" << n << "}";
  if((al=l.suivant())) o << *al;
  if (l.sq()) o << "]";
  return o;
}

// from the header file:
// typedef enum { aqueous, aqueous_explicit, gas, liquid, sol } moltype;
const char* moltypeStr[] = { "", "(aq)", "(g)", "(l)", "(s)" };

const std::string Molec::signature()const{
  std::ostringstream o;
  o << liste();
  if (charge()){
    o << "^{";
    if(fabs(1.0*charge())!=1) o << fabs(1.0*charge());
    if(charge()>0) o << "+}"; else o << "-}";
  }
  if (t != aqueous) o << moltypeStr[t];
  return std::string (o.str());
}

bool Molec::printcount(std::ostream & o, bool first) const{
  if (!first) o << ", ";
  first=false;
  printnorm(o);
  o << '|';
  al-> printcount(o,nb);
  return first;
}

bool Molec::printelec(std::ostream & o, bool first) const{
  if (!first) o << ", ";
  first=false;
  printnorm(o);
  o << '|';
  o << nb << '*' << ch;
  return first;
}

bool Molec::printspecies(std::ostream & o, bool first) const{
  if (!first) o << ", ";
  first=false;
  printnorm(o);
  return first;
}

void Molec::delete_aq(){
  // retire les attributs aqueux
  if (t==aqueous_explicit) t=aqueous;
}

void Molec::printnorm(std::ostream & o)const{
  if (nb!=1) o << nb << " ";
  al-> printnorm(o);
  if (ch) {
    o << "^";
    if(fabs(1.0*ch)!=1) o << fabs(1.0*ch);
    if(ch>0) o << "+"; else o << "-";
  }
  // the aqueous type is the default, so: no _(aq)
  // to have this suffix, on must make the type to aqueous_explicit
  if (!iswater() && !iselectron() && t != aqueous) o << "_"  << moltypeStr[t];
}

/**
 * Affiche le coefficient stœchimétrique
 * @param o un flux de sortie
 * @param sign plus ou moins un selon que c'est un produit ou un réactif.
 **/
void Molec::printcoeff(std::ostream & o, int sign)const{
  o << nb * sign;
}

void Molec::coeff( fraction f){
  nb.i *= f.i;
  nb.d *= f.d;
  nb.simplifie();
}

bool Molec::printNernst(std::ostream & o, const char * prefix){
  switch(t){
  case sol :
  case liquid:
    return 0;
  case aqueous :
  case aqueous_explicit:
    if (iswater() || iselectron()) return 0;
    o << prefix << "[" << *al;
    if (ch){
      o << "^{";
      if(fabs(1.0*ch)!=1) o << fabs(1.0*ch);
      if(ch>0) o << "+}"; else o << "-}";
    }
    if (t != aqueous) o << "_{" << moltypeStr[t] << "}";
    o  <<"]";
    if (nb!=1) {
      o << "^{"; 
      printNombre(o);
      o << "}";
    }
    return 1;
  case gas :
    o << prefix << "P_{" << *al << "}";
    if (nb!=1) {
      o << "^{"; 
      printNombre(o);
      o << "}";
    }
    return 1;
  }
  return 0;
}

bool Molec::printNernstWIMS(std::ostream & o, bool wantedlatex){
  if (iswater() || iselectron()) {
    return false;
  }
  switch(t){
  case sol :
  case liquid :
    {
      return false;
    }
  case aqueous :
  case aqueous_explicit :
    if(wantedlatex){
      o <<  "[" << *al;
    }else{
      o << "["; al->printnorm(o);
    }
    if (ch){
      if(wantedlatex){
	o << "^{";
      }else{
	o << "^";
      }
      if(fabs(1.0*ch)!=1) o << fabs(1.0*ch);
      if(wantedlatex){
	if(ch>0) o << "+}"; else o << "-}";
      }else{
	if(ch>0) o << "+"; else o << "-";
      }
    }
    o  <<"]";
    if (nb!=1) {
      if(wantedlatex){
	o << "^{";
	if (nb.d==1){
	  o << nb.i ;
	}
	else {
	  o << "\\frac{" << nb.i << "}{" << nb.d << "}";
	}
	o << "}";
      }else{
	o << "^" << nb; 
      }
    }
    return true;
  case gas :
    if(wantedlatex){
      o << "P_{" << *al << "}";
    }else{
      o << "P_"; al->printnorm(o);
    }
    if (nb!=1) {
      if(wantedlatex){
	o << "^{";
	if (nb.d==1){
	  o << nb.i ;
	}
	else {
	  o << "\\frac{" << nb.i << "}{" << nb.d << "}";
	}
	o << "}";
      }else{
	o << "^" << nb; 
      }
    }
    return true;
  }
  return false;
}

bool Molec::iswater()const{
  if (t != aqueous) return 0;
  if (signature()==std::string("\\mathrm{H}_{2}\\mathrm{O}") || 
      signature()==std::string("\\mathrm{O}\\mathrm{H}_{2}")) return 1;
  else return 0;
}

bool Molec::iselectron()const{
  return (signature()==std::string("\\mathrm{e}^{-}"));
}

fraction Molec::nbelectron()const{
  if (iselectron()) return nb;
  else return fraction(0);
}

void Molec::add(fraction f){
  nb = nb+f;
}

void Molec::sub(fraction f){
  nb = nb-f;
}

void Molec::printNombre(std::ostream & o)const{
  if (nb.d==1){
    o << nb.i << "\\,";
  }
  else {
    o << "\\frac{" << nb.i << "}{" << nb.d << "}\\,";
  }
}

std::ostream & operator << (std::ostream & o, const Molec & m){
  if (m.nombre() != 1) m.printNombre(o);
  o << m.liste();
  if (m.charge()){
    o << "^{";
    if(fabs(1.0*m.charge())!=1) o << fabs(1.0*m.charge());
    if(m.charge()>0) o << "+}"; else o << "-}";
  }
  if (m.typage() != aqueous && ! m.iselectron()) o << "_{" << moltypeStr[m.typage()] << "}";
  return o;
}

int Membre::findMol(const Molec * m){
  // returns the index of a molecule with the same atomlist if any
  // else returns -1.
  int result=-1;
  for(unsigned int i=0; i<size(); i++){
    if ((*this)[i]->eqMol(m)) result=i;
  }
  return result;
}

void Membre::addMol(const Molec * m){
  int i = findMol(m);
  if (i < 0){
    push_back(new Molec(*m));
  } else {
    (*this)[i]->add(m->nombre());
  }
}

void Membre::addMembre(const Membre * m){
  for(unsigned int i=0; i<m->size(); i++){
    addMol((*m)[i]);
  }
}

void Membre::eraseNull(){
  Membre m(*this);
  clear();
  for(unsigned int i=0; i < m.size();i++){
    if (m[i]->nombre().i>0) push_back(m[i]);
  }
}

void Membre::compte(Compteur & c)const{
  for(unsigned int i =0; i < size(); i++){
    operator [] (i)->compte(c);
  }
}

void Membre::numerote(){
  for (unsigned int i=0; i < size(); i++){
    operator [](i)->numero(i);
    operator [](i)->liste().numerote();
  }
}

void Membre::delete_aq(){
  // retire les attributs aqueux
  for (unsigned int i=0; i < size(); i++){
    operator [](i)->delete_aq();
  }  
}

void Membre::triage(){
  unsigned int i,j;
  for (i=0; i < size(); i++){
    operator [](i)->triage();
  }
  for (i=0; i < size(); i++){
    for (j=i+1; j < size(); j++){
      if (operator [](i)->signature() > operator [](j)->signature()){
	Molec * m = operator [](i);
	operator [](i) = operator [](j);
	operator [](j) = m;
      }
    }
  }
}

void Membre::printcount(std::ostream & o) const{
  bool first=true;
  for(unsigned int i=0; i < size(); i++){
    first=operator[](i)->printcount(o,first);
  }
}

void Membre::printelec(std::ostream & o) const{
  bool first=true;
  for(unsigned int i=0; i < size(); i++){
    first=operator[](i)->printelec(o,first);
  }
}

void Membre::printspecies(std::ostream & o) const{
  bool first=true;
  for(unsigned int i=0; i < size(); i++){
    first=operator[](i)->printspecies(o,first);
  }
}

void Membre::printnorm(std::ostream & o) const{
  for(unsigned int i=0; i < size(); i++){
    operator[](i)->printnorm(o);
    if (i < size()-1) o << " + ";
  }
}

/**
 * affiche les coefficients stœchiométriques d'un membre d'équation
 * @param o un flux de sortie
 * @param sign la valeur plus ou moins un selon que les réactifs sont produits
 *             ou consommés.
 **/
void Membre::printcoeff(std::ostream & o, int sign) const{
  for(unsigned int i=0; i < size(); i++){
    operator[](i)->printcoeff(o, sign);
    if (i < size()-1) o << ",";
  }
}

void Membre::printweight(std::ostream & o) const{
  for(unsigned int i=0; i < size(); i++){
    o << operator[](i)->weight();
    if (i < size()-1) o << " ";
  }
}

void Membre::coeff( fraction f){
  for (unsigned int i=0; i<size(); i++) operator[](i)->coeff(f);
}

int Membre::printableNernst(){
  int result=0;
  for (unsigned int i=0; i<size(); i++) {
    if (operator[](i)->typage() != sol && 
	!operator[](i)->iswater() &&
	!operator[](i)->iselectron()) result =1;
  }  
  return result;
}

bool Membre::redox()const{
  for (unsigned int i=0; i<size(); i++){
    if ((*this)[i]->iselectron()) /* c'est un électron */ return 1;
  }
  return 0;
}

fraction  Membre::nbelectron()const{
  fraction result(0);

  for(unsigned int i = 0; i<size(); i++) result=result+(*this)[i]->nbelectron();
  return result;
}

void Membre::printNernst(std::ostream & o){
  bool printed = 0; 
  const char * prefix="";
  for (unsigned int i=0; i<size(); i++) {
    if (i>0) prefix="\\,";
    if (operator[](i)->printNernst(o, prefix)){
      printed = 1; 
    }
  }
  if (!printed) o << "1";
}

void Membre::printNernstWIMS(std::ostream & o, bool wantedlatex){
  bool printed = false; 
  bool addcomma = false;
  for (unsigned int i=0; i<size(); i++) {
    std::ostringstream w;
    if (operator[](i)->printNernstWIMS(w,wantedlatex)){
      if (addcomma) o << ", ";
      o << w.str();
      printed = true; 
      addcomma = true;
    } else {
      addcomma = false;
    }
  }
  if (!printed) o << "1";
}

std::ostream & operator << (std::ostream & o, const Membre & m){
  for(unsigned int i=0; i < m.size()-1; i++){
    o << *m[i] << "\\,+\\,";
  }
  o << *m[m.size()-1];
  return o;
}

Membre operator & (Membre & m1, Membre & m2){
  Membre result;
  //result.printnorm(std::cout);
  fraction min(1);
  for(Membre::iterator i = m1.begin(); i < m1.end(); i++){
    for(Membre::iterator j = m2.begin(); j < m2.end(); j++){
      if ((*i)->eqMol(*j)){
	Molec *m = new Molec(**i);
	if ((*i)->nb > (*j)->nb){
	  min=(*j)->nb;
	}else{
	  min=(*i)->nb;
	}
	m->nb=min;
	result.push_back(m);
      }
    }
  }
  return result;
}

Membre operator - (Membre & m1, Membre & m2){
  Membre result;
  fraction diff(1);
  for(Membre::iterator i = m1.begin(); i < m1.end(); i++){
    Molec *m = new Molec(**i);
    for(Membre::iterator j = m2.begin(); j < m2.end(); j++){
      if ((*i)->eqMol(*j)){
	diff=(*i)->nb - (*j)->nb;
	m->nb=diff;
      }
    }
    result.push_back(m);
  }
  return result;
}

bool Chemeq::valdefined()const{
  return val > MINVAL;
}

void Chemeq::addChemeq(const Chemeq * c){
  if (valdefined() && c->valdefined()){
    long double e1=enthalpy(), e2=c->enthalpy();
    fraction n1=nbelectron(), n2=c->nbelectron();
    long double e = e1+e2;
    fraction n=n1+n2;
    if (n.i==0) val=exp(-e/R/T0);
    else val=-e*n.d/n.i/Faraday;
  } else {
    val=MINVAL;
  }
  gauche->addMembre(c->gauche);
  droit->addMembre(c->droit);
  simplifie(true);
}

void Chemeq::subChemeq(const Chemeq * c){
  if (valdefined() && c->valdefined()){
    long double e1=enthalpy(), e2=c->enthalpy();
    fraction n1=nbelectron(), n2=c->nbelectron();
    long double e = e1-e2;
    fraction n=n1-n2;
    if (n.i==0) {
    val=exp(-e/R/T0);
    } else{
      val=-e*n.d/n.i/Faraday;
    }
  } else {
    val=MINVAL;
  }
  gauche->addMembre(c->droit);
  droit->addMembre(c->gauche);
  simplifie(true);
}

long double Chemeq::enthalpy() const{
  fraction n=nbelectron();
  if (redox()){
    return -val*n.i/n.d*Faraday;
  } else {
    return -R*T0*log(val);
  }
}

void Chemeq::simplifie(bool tri=false){
  Membre communs(*gauche & *droit);
  if (communs.size()>0){
    Membre * g, *d;
    g= new Membre(*gauche - communs);
    d= new Membre(*droit  - communs);
    delete gauche;
    delete droit;
    gauche=g;
    droit =d;
  }
  gauche->eraseNull();
  droit->eraseNull();
  if (tri){
    numerote(); 
    triage();
  }
}

void Chemeq::printnorm(std::ostream & o){
  gauche->printnorm(o);
  o << " -> ";
  droit->printnorm(o);
  if (val>MINVAL){
    o << " (";
    if (cste!=std::string("")) o << cste << " = ";
    o << val;
    if (redox()) o << " V";
    o << ")";
  }
}

/**
 * Affiche les coefficients stœchiométriques
 **/
void Chemeq::printcoeff(std::ostream & o) const {
  gauche->printcoeff(o, -1);
  o << ";";
  droit->printcoeff(o, 1);
}

void Chemeq::printcount(std::ostream & o) const {
  gauche->printcount(o);
  o << "; ";
  droit->printcount(o);
}

void Chemeq::printelec(std::ostream & o) const {
  gauche->printelec(o);
  o << "; ";
  droit->printelec(o);
}

void Chemeq::printspecies(std::ostream & o) const {
  gauche->printspecies(o);
  o << "; ";
  droit->printspecies(o);
}

void Chemeq::printweight(std::ostream & o) const{
  gauche->printweight(o);
  o << " ";
  droit->printweight(o);
}

std::string Chemeq::equilibre(){
  std::ostringstream s;
  Compteur cpg, cpd;
  gauche->compte(cpg);
  droit->compte(cpd);
  if(cpg==cpd) s << "OK";
  else s << "ERROR " << cpg << " / " << cpd;
  return std::string(s.str());
}

void Chemeq::coeff1(){
  fraction mult = gauche->operator[](0)->nombre();
  mult.inverse();
  gauche->coeff(mult);
  droit->coeff(mult);
  simplifie();
  if (!redox() && valdefined()){
    val = val*mult.i/mult.d;
  }
}

void Chemeq::delete_aq(){
  // supprime tous les attributs "aqueux" des molécules ou ions présents
  gauche->delete_aq();
  droit->delete_aq();
}

void Chemeq::multiply(int num, int den){
  fraction mult(num,den);
  gauche->coeff(mult);
  droit->coeff(mult);
  simplifie();
  if (!redox() && valdefined()){
    val = val*mult.i/mult.d;
  }
}

bool Chemeq::redox()const{
  return gauche->redox() || droit->redox();
}

void Chemeq::printNernst(std::ostream & o, 
			 std::ostream & w, 
			 bool wantedlatex){
  Membre * ga, * dr;
  if (!redox()){
    if (gauche->printableNernst()){
      o << "\\frac{";
      droit->printNernst(o);
      o << "}{";
      gauche->printNernst(o);
      o << "}";
    }
    else {
      droit->printNernst(o);
    }
    droit->printNernstWIMS(w,wantedlatex);
    w << "; ";
    gauche->printNernstWIMS(w,wantedlatex);
    if (val > MINVAL) {
      o << "\\,=\\,";
      if (cste!=std::string("")) o << cste << "\\,=\\,";
      o << valeur_latex();
      if(wantedlatex){
	w << "; " << valeur_latex();
      }else{
	w << "; " << val;
      }
    }
    else{
      o << "\\,=\\,K";
      w << "; K";
    }
  }
  else{ /* c'est une réaction redox  */
    o << "E\\,=\\,";
    if(wantedlatex){
      w << "E\\,=\\,";
    }else{
      w << "E=";
    }
    if (val > MINVAL) {
      o << val;
      w << val << ";";
    }
    else{
      o << "E_{0}";
      if(wantedlatex){
	w << "E_{0};";
      }else{
	w << "E0;";
      }
    }
    o << "\\,+\\,\\frac{R\\,T}{";
    o << gauche->nbelectron()+droit->nbelectron() << "\\,F}";
    o << "\\log";
    w << gauche->nbelectron()+droit->nbelectron() << ";";
    if (gauche->redox()){ /* c'est une réduction */
      ga=gauche; dr=droit;
    }
    else{ /* c'est une oxydation */
      ga=droit; dr=gauche;
    }
    if (dr->printableNernst()){
      o << "\\frac{";
      ga->printNernst(o);
      o << "}{";
      dr->printNernst(o);
      o << "}";
    }
    else {
      o << "(";
      ga->printNernst(o);
      o << ")";
    }
    // implanter la sortie pour Wims ici.
    dr->printNernstWIMS(w,wantedlatex);
    w << "; ";
    ga->printNernstWIMS(w,wantedlatex);
  }
}

std::string Chemeq::valeur_latex()const{
  std::ostringstream so;
  so << val;
  std::string s(so.str());
  std::string::size_type epos=s.find('e',0);
  if (epos!=std::string::npos){
    s.erase(epos,1);
    s.insert(epos,"\\times 10^{");
    s=s+"}";
  }
  return (std::string) s;
}

std::ostream & operator << (std::ostream & o, const Chemeq & c){
  if (c.is_equilibrium()){ /* equilibria must be rendered with two arrows */
    o << *c.membregauche() << "\\,\\leftrightharpoons\\," << *c.membredroit();
  } else {
    o << *c.membregauche() << "\\,\\longrightarrow\\," << *c.membredroit();
  }
  if (c.valeur() > MINVAL) {
    o << "\\,(";
    if (c.constante()!=std::string("")) o << c.constante() << "\\,=\\,";
    o << c.valeur_latex();
    if (c.redox()) o << " V";
    o << ")";
  }
  return o;
}

std::ostream & operator << (std::ostream & o, fraction f){
  o << f.i;
  if (f.d!=1) o << '/' << f.d;
  return o;
}

fraction operator * (fraction f, int m){
  fraction result = fraction(f.i*m, f.d);
  result.simplifie();
  return result;
}

fraction operator * (int m, fraction f){
  fraction result = fraction(f.i*m, f.d);
  result.simplifie();
  return result;
}

fraction operator * (fraction f, fraction m)
{
  fraction result = fraction(f.i*m.i, f.d*m.d);
  result.simplifie();
  return result;
}

fraction operator + (fraction f, fraction g){
  fraction result = fraction(f.i*g.d+g.i*f.d, f.d*g.d);
  result.simplifie();
  return result;
}

fraction operator - (fraction f, fraction g){
  fraction result = fraction(f.i*g.d-g.i*f.d, f.d*g.d);
  result.simplifie();
  return result;
}

const fraction & minFraction(const fraction& f1, const fraction &f2){
  if (f1.i*f2.d > f2.i*f1.d) return f1;
  else return f2;
}

void fraction::simplifie(){
  int maxprem = 23;
  int premiers[]={2,3,5,7,11,13,17,19,23,29};
  int n;

  for (n=0; premiers[n]<= maxprem; n++){
    while (i % premiers[n] == 0 && d % premiers[n] == 0){
      i /= premiers[n]; d /= premiers[n];
    }
  }
}

bool operator > (fraction f, int i){
  return f.i > f.d*i;
}

bool operator > (fraction f1, fraction f2){
  return f1.i*f2.d > f1.d*f2.i;
}

bool operator != (fraction f, int i){
  return f.i != f.d*i;
}

double mendelweight(int i){
  if (i>=0) return strtod (table[i].info[WEIGHT],0); else return 0.0;
}

int findmendel(const char * symb){
  int i=0;
  while (table[i].info[0] && strcmp(table[i].info[SYMBOL], symb) != 0) i++;
  if (table[i].info[0]) return i; else return -1;
}

double mendelweight(const char * symb){
  int i;
  i = findmendel(symb);
  return mendelweight(i);
}
