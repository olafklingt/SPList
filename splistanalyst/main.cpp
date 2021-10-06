#include "periode.hpp"
#include "sfbuffer.hpp"
#include <fstream>
#include <iostream>
#include <memory>
#include <sndfile.h>
#include <span>

#include <unistd.h>

using namespace std;

int main(int argc, char *argv[]) {
  int minsize;
  cout << "argc" << argc<<endl;
  if (argc < 4) {
    cout << "usage: infile outfile minsize" << endl << flush;
    return -1;
  };
  try {
    minsize = stoi(argv[3]);

    SFBuffer sf(argv[1], minsize);

    ofstream outfile(argv[2], ios::out | ios::binary);

    while (auto p = sf.loadNextPeriode()) {
      sleep(10);
      p->write_analyse(&outfile);
    }

    outfile.close();
    return 0;
  } catch (const invalid_argument &ia) {
    cout << "argument 3 should be an integer and not: " << argv[3] << endl;
    return -1;
  } catch (char const *ia) {
    cout << ia << endl;
    return -1;
  }
}
