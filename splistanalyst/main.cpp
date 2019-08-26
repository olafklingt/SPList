#include "periode.hpp"
#include "sndfile.hh"
#include <fstream>
#include <iostream>
#include <sstream>

using namespace std;

typedef double sample_t;

template <typename T> int strto(string &a, T &b) {
  stringstream oss;
  oss << a;
  if (oss >> b) {
    if (!oss.eof())
      return -1;
    else
      return 0;
  }
  return -1;
}

int str2int(string a) {
  int out = 0;
  strto(a, out);
  return out;
}

class Soundfile {
  // protected:
public:
  //  Soundfile();
  SndfileHandle *sf;
  sf_count_t frames;
  int samplerate;

  sample_t *buffer;

  int start;
  int pos;

public:
  Soundfile(char *filename);
  int fillBuffer();
  int isZeroCrossing(sample_t old, sample_t next);
  int findZeroCrossing(int minsize);
  int copyArray(sample_t *goal, sample_t *source, int size);
  sample_t *loadArray(int minsize, int *size);
  Periode *loadNextPeriode(int minsize);
  void close();
};

Soundfile::Soundfile(char *filename) {
  sf = new SndfileHandle(filename);
  frames = 0;
  pos = 0;
}

int Soundfile::fillBuffer() {
  frames = sf->frames();
  samplerate = sf->samplerate();
  buffer = new sample_t[frames];
  if (sf->channels() > 1) {
    cout << "soundfile must be mono" << endl << flush;
    return -1;
  }
  sf_count_t rf = sf->readf(buffer, frames);
  return rf;
}

int Soundfile::isZeroCrossing(sample_t old, sample_t next) {
  return (old < 0 && next >= 0);
}

int Soundfile::findZeroCrossing(int minsize) {
  int size = frames;
  int rp = start;
  int nth = 0;
  if (rp >= size)
    return 0;
  while (minsize--) {
    rp++;
    nth++;
  };
  while (!isZeroCrossing(buffer[rp], buffer[rp + 1])) {
    rp++;
    nth++;
    if (rp >= size - 1)
      return 0;
  };
  nth++;
  return nth;
}

int Soundfile::copyArray(sample_t *goal, sample_t *source, int size) {
  while (size--) {
    *goal++ = *source++;
  }
  return size;
}

sample_t *Soundfile::loadArray(int minsize, int *size) {
  int periodesize = findZeroCrossing(minsize); // wo der nägste punkt ist
  sample_t *s_vec1 = 0;
  if (periodesize == 0) // wenns keinen punkt gibt
    periodesize = frames - start;
  if (periodesize == 0)
    return 0;
  s_vec1 = new sample_t[periodesize];
  copyArray(s_vec1, &buffer[start], periodesize);
  *size = periodesize;
  return s_vec1;
}

Periode *Soundfile::loadNextPeriode(int minsize) {
  Periode *goal;
  int periodesize = 0;
  sample_t *s_vec = loadArray(minsize, &periodesize);
  if (!s_vec) {
    return 0;
  }
  //  periodesize++;
  goal = new Periode(s_vec, periodesize, start, samplerate);
  start += periodesize;
  return goal;
};

int main(int argc, char *argv[]) {
  if (argc < 3) {
    cout << "usage: infile outfile minsize" << endl << flush;
    return -1;
  };
  Soundfile *sf = new Soundfile(argv[1]);
  Periode *p;
  sf->fillBuffer();
  ofstream outfile(argv[2], ios::out | ios::binary);
  int minsize = str2int(argv[3]);
  while ((p = sf->loadNextPeriode(minsize))) {
    // if (p->length() > 500) {
      // p->print();
    // };
    p->write_analyse(&outfile);
  }
}
