#ifndef PERIODE_H
#define PERIODE_H
#include <fstream>

using namespace std;


typedef double sample_t;

class Periode{
private:
  sample_t* s_vec;
  int size;
  double v_start;
  int sr;

  double v_rms;
  double v_delta;
  double v_summe;
  double v_peaks;
  double v_amp;
  double v_min;
  double v_max;


public:
  Periode(sample_t* _s_vec,int _size,int _start,int _sr);
  ~Periode();
  sample_t* getVec();  
  int getSize();

  double rms();
  double delta();
  double summe();
  double peaks();
  double amp();
  double min();
  double max();

  double length();
  double start();
  double freq();
  
  void print();
  void write_analyse(ofstream *f);

private:
  void analyse();
  void a_rms();
  void a_delta();
  void a_summe();
  void a_peaks();
  void a_amp();
  void a_min();
  void a_max();
  
};

#endif
