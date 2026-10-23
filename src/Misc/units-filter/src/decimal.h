#ifndef DECIMAL_H
#define DECIMAL_H
#include <gmpxx.h>
#include <stdio.h>
#include <string>

using namespace std;

class Decimal{
 private:
  mpz_class val, powten;
  bool nan; // Not a Number if true
 public:
  Decimal(){
    val=0;
    powten=0;
    nan=false;
  };
  Decimal(double v);
  Decimal(const char* s);
  void debug();
  const char * c_str() const;
  const string get_str() const;
  double get_double() const;
  void multiplyByTen(mpz_class times);
  void keepSignificant (int nb);
  friend  ostream & operator << (ostream & o, const Decimal & d);
  friend bool operator <= (const Decimal & d1, const Decimal & d2);
  friend bool operator == (const Decimal & d1, const Decimal & d2);
  friend Decimal operator * (const Decimal & d1, const Decimal & d2);
  friend Decimal operator + (const Decimal & d1, const Decimal & d2);
};

ostream & operator << (ostream & o, const Decimal & d);
bool operator <= (const Decimal & d1, const Decimal & d2);
bool operator == (const Decimal & d1, const Decimal & d2);
Decimal operator * (const Decimal & d1, const Decimal & d2);
Decimal operator + (const Decimal & d1, const Decimal & d2);

#endif //DECIMAL_H
