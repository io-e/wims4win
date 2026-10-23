#include <iostream>
#include "decimal.h"
#include <math.h>
#include <assert.h>

using namespace std;

/**
 * Constructor
 * @param v a numeric value.
 **/
Decimal::Decimal(double v){
  val=0;
  powten=0;
  nan=0;
  if (v==0.0){
    return;
  }
  while(trunc(v)!=v){
    v*=10;
    powten--;
    assert(powten > -100); // such a small value should not be reached with doubles
  }
  val=v;
  return;
}

/**
 * Constructor
 * @param s a string
 **/
Decimal::Decimal(const char * s){
  val=0;
  powten=0;
  nan=0;
  int sign=1;
  int powsign=1;
  const char * cur, *end;
  end=strchr(s,'\0');
  cur=s;
  // search a sign
  while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
  if(cur<end && *cur=='-'){
    sign=-1; cur++;
  } else{
    if (cur<end && *cur=='+'){
      cur++;
    }
  }
  // search an integer part
  while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
  while (cur<end && strchr("0123456789", *cur)){
    val=val*10+(*cur-'0');
    cur++;
  }
  // search a mantissa
  if(cur<end && *cur=='.'){
    cur++; // eat the decimal dot
    while (cur<end && strchr("0123456789", *cur)){
      val=val*10+(*cur-'0');
      powten--;
      cur++;
    }
  }
  // search power of ten
  while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
  if(cur<end && strchr("eE", *cur)){
    cur++;
    int pt=0;
    while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
    if(cur<end && *cur=='-'){
      powsign=-1;
      cur++;
    }
    while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
    while (cur<end && strchr("0123456789",*cur)){
      pt=pt*10+(*cur-'0');
      cur++;
    }
    powten+=pt;
  }
  while (cur<end && strchr(" \t",*cur)) cur++; // eat up spaces
  assert (cur==end);
  val=sign*val;
  powten=powsign*powten;
}

void Decimal::debug(){
  cout << "Decimal: val=" << val.get_str() << ", powten=" << powten.get_str() << ", nan=" << nan << endl;
}

/**
 * Multiplies self by ten, 'times' times
 * @param times an integer to multiply the value (default: times = 1)
 **/
void Decimal::multiplyByTen(mpz_class times){
  powten+=times;
}

/**
 * Keeps at least nb significant digits
 * @param nb the number of digits to keep
 **/
void Decimal::keepSignificant (int nb){
  string alldigits = val.get_str();
  int negShift=0;
  if (alldigits[0]=='-') negShift=1; // shift one char if negative
  char nextchar='0';
  int l = alldigits.length();
  if (nb+negShift<l) nextchar=alldigits[nb+negShift]; 
  // number next to the least significant
  for (int i=nb+negShift; i<l; i++) alldigits[i]='0'; 
  // zeroes non-significant digits
  mpz_class result(alldigits.c_str());
  if (nextchar >= '5'){ // must modify the rounding
    string addon="1";
    addon.append(l-nb-negShift,'0');
    if (result>0) result = result + mpz_class(addon);
    else result = result - mpz_class(addon);
  }
  val=result;
}

ostream & operator << (ostream & o, const Decimal & d){
  double r=d.get_double();
  o << r;
  return o;
}

Decimal operator * (const Decimal & d1, const Decimal & d2){
  Decimal result;
  result.val=d1.val*d2.val;
  result.powten=d1.powten+d2.powten;
  result.nan=d1.nan && d2.nan;
  return result;
}

Decimal operator + (const Decimal & d1, const Decimal & d2){
  Decimal result;
  Decimal op1, op2;
  if(d1.powten <= d2.powten) {op1=d1; op2=d2;}
  else  {op1=d2; op2=d1;}
  // the first operand has the least power of ten
  // decrease the power of ten for the other one, and keep its value constant
  while(op2.powten > op1.powten) {op2.powten-=1; op2.val *= 10;}
  result.powten=op1.powten;
  result.val=op1.val+op2.val;
  return result;
}

/**
 * Create a C string with the decimal value
 * @return a zero terminated string
 **/
const char * Decimal::c_str() const{
  string s = get_str();
  char *t = new char[s.length()+1];
  std::strcpy(t,s.c_str());
  return t;
}

/**
 * Créate a Std string with the decimal value
 * @return a standard string instance
 **/
const string Decimal::get_str() const{
  Decimal r(*this);
  string result="0";
  if (r.powten>=0){
    while (r.powten>0) {r.powten -= 1; r.val *=10;}
    return r.val.get_str();
  } else {
    result.append(-powten.get_si(),'0'); // fills with abs(powten) additional zeroes
    r.val *= sgn(val); // make an absolute value to discard the minus sign
    result.append(r.val.get_str());
    result.insert(result.length()-1+powten.get_si(),"."); // insert the decimal dot
    int i=0;
    while (result[i]=='0' && result[i+1]!='.') i++;
    result.erase(0, i-1); // remove useless zeroes
    if (sgn(val)==-1) result.insert(0,"-"); // restores minus sign
    return result;
  }
}

bool operator == (const Decimal & d1, const Decimal & d2){
   Decimal op1, op2;
  if(d1.powten <= d2.powten) {op1=d1; op2=d2;}
  else  {op1=d2; op2=d1;}
  // the first operand has the least power of ten
  // decrease the power of ten for the other one, and keep its value constant
  while(op2.powten > op1.powten) {op2.powten-=1; op2.val *= 10;}
  // both operands have the same powten
  return op1.val == op2.val;
}

bool operator <= (const Decimal & d1, const Decimal & d2){
   Decimal op1, op2;
  if(d1.powten <= d2.powten) {op1=d1; op2=d2;}
  else  {op1=d2; op2=d1;}
  // the first operand has the least power of ten
  // decrease the power of ten for the other one, and keep its value constant
  while(op2.powten > op1.powten) {op2.powten-=1; op2.val *= 10;}
  // both operands have the same powten
  return op1.val <= op2.val;
}

double Decimal::get_double() const{
  double result = val.get_d();
  int i;
  if (powten>0){
    for(i=0; i < powten; i++) result *= 10;
  } else {
    for(i=0; i < -powten; i++) result /= 10;
  }
  return result;
}

