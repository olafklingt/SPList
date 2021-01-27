#pragma once
#include <fstream>
#include <span>

using namespace std;

typedef double sample_t;

class Periode {
private:
  span<sample_t> buf;

  double v_start;
  int sr;

  double v_rms;
  double v_delta;
  double v_summe;
  double v_peaks;
  double v_amp;

public:
  Periode(sample_t *_buf, int _size, int _start, int _sr);
  ~Periode();

  double rms();
  double delta();
  double summe();
  double peaks();
  double amp();

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
};
