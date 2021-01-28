#include "sfbuffer.hpp"
#include "periode.hpp"
#include <fstream>
#include <iostream>
#include <memory>
#include <sndfile.h>
#include <span>

SFBuffer::SFBuffer(char *filename, int _minsize) : minsize(_minsize) {
  SndfileHandle sf{filename};

  frames = sf.frames();
  samplerate = sf.samplerate();
  if (sf.channels() > 1) {
    throw "soundfile must be mono";
  }
  buffer = new sample_t[frames];
  sf_count_t rf = sf.readf(buffer, frames);

  if (rf != sf.frames()) {
    delete[] buffer;
    throw "ERROR: Did not read same amount of frames: " +
        std::to_string(sf.frames()) + " only read: " + std::to_string(rf);
  }
}

SFBuffer::~SFBuffer() {
  delete[] buffer;
}

int SFBuffer::isZeroCrossing(sample_t old, sample_t next) {
  return (old < 0 && next >= 0);
}

int SFBuffer::findZeroCrossing() {
  int ms = minsize;
  int rp = start;
  while (ms--) {
    rp++;
  };
  // last period might be shorter than minsize
  if (rp >= frames - 1)
    return frames - start;

  while (!isZeroCrossing(buffer[rp], buffer[rp + 1])) {
    rp++;
    // last period doesn't stop with a zero crossing
    if (rp >= frames - 1)
      return frames - start;
  };
  return rp - start + 1;
}

unique_ptr<Periode> SFBuffer::loadNextPeriode() {
  int periodesize = findZeroCrossing();
  if (periodesize == 0) {
    return nullptr;
  }
  unique_ptr<Periode> goal =
      make_unique<Periode>(&buffer[start], periodesize, start, samplerate);
  start += periodesize;
  return goal;
};
