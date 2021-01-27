#include "periode.hpp"
#include <iostream>
#include <cmath>
using namespace std;

Periode::Periode(sample_t *_buf, int _size, int _start, int _sr)
    : buf(_buf, _size), v_start(_start), sr(_sr) {
  analyse();
};

Periode::~Periode(){};

void Periode::analyse() {
  a_rms();
  a_delta();
  a_summe();
  a_peaks();
  a_amp();
};

double Periode::amp() { return v_amp; };
double Periode::rms() { return v_rms; };
double Periode::delta() { return v_delta; };
double Periode::summe() { return v_summe; };
double Periode::peaks() { return v_peaks; };

double Periode::length() { return (double)buf.size(); };
double Periode::start() { return v_start; };
double Periode::freq() { return (double)sr / (double)buf.size(); };

void Periode::a_rms() {
  int s = buf.size();
  v_rms = 0;
  for (sample_t i : buf) {
    v_rms += abs(i) / s;
  }
}

void Periode::a_delta() {
  sample_t old = 0;
  v_delta = 0;
  for (sample_t sample : buf) {
    v_delta += abs(sample - old);
    old = sample;
  }
}

void Periode::a_summe() {
  v_summe = 0;
  for (sample_t sample : buf) {
    v_summe += abs(sample);
  }
}

void Periode::a_peaks() {
  unsigned int off = 1;
  v_peaks = 0;
  sample_t *buff_p, *buff_old, *buff;
  unsigned int l = buf.size();
  buff_old = buff_p = buff = buf.data();

  while (l--) {
    if (l > off) {
      while (*buff == buff[off]) {
        if (l > (off + 1))
          off++;
        else
          break;
      }
      if ((*buff > *buff_old) && (*buff > buff[off]))
        v_peaks++;
      if ((*buff < *buff_old) && (*buff < buff[1]))
        v_peaks++;
    }
    if (1 < off)
      off--;
    buff++;
    if (l != ((buf.size()) - 1))
      buff_old++;
  }
}

void Periode::a_amp() {
  float v;
  v_amp = 0;
  for (sample_t sample : buf) {
    v = abs(sample);
    if (v_amp < v)
      v_amp = v;
  }
}

void Periode::print() {
  cout << start() << "\t";
  cout << length() << "\t";
  cout << amp() << "\t";
  cout << rms() << "\t";
  cout << peaks() << "\t";
  cout << delta() << "\t" << endl << flush;
}

void Periode::write_analyse(ofstream *f) {
  double s = start();
  double l = length();
  double a = amp();
  double r = rms();
  double p = peaks();
  double d = delta();

  f->write(reinterpret_cast<char *>(&s), sizeof(double));
  f->write(reinterpret_cast<char *>(&l), sizeof(double));
  f->write(reinterpret_cast<char *>(&a), sizeof(double));
  f->write(reinterpret_cast<char *>(&r), sizeof(double));
  f->write(reinterpret_cast<char *>(&p), sizeof(double));
  f->write(reinterpret_cast<char *>(&d), sizeof(double));
}
