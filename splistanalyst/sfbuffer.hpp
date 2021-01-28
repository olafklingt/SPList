#pragma once
#include "periode.hpp"
#include "sndfile.hh"
#include <memory>

typedef double sample_t;

class SFBuffer {
public:
  int samplerate;

  sample_t *buffer;
  sf_count_t frames = 0;

  int start = 0;
  int pos = 0;

  int minsize;

  SFBuffer(char *filename, int minsize);
  ~SFBuffer();
  int isZeroCrossing(sample_t old, sample_t next);
  int findZeroCrossing();
  std::unique_ptr<Periode> loadNextPeriode();
  void close();
};
