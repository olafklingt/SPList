#include <iostream>
#include <fstream>
#include "periode.hpp"
#include <math.h>
using namespace std;



Periode::Periode(sample_t* _s_vec,int _size,int _start,int _sr)
  :s_vec(_s_vec),size(_size),v_start(_start),sr(_sr){
  analyse();
};

Periode::~Periode(){
  delete s_vec;
};

/*
sample_t* Periode::getVec(){
  return s_vec;
};

int Periode::getSize(){
  return size;
};
*/

void Periode::analyse(){
  a_rms();
  a_delta();
  a_summe();
  a_peaks();
  a_amp();
  a_min();
  a_max();
};

double Periode::amp(){return v_amp;};
double Periode::rms(){return v_rms;};
double Periode::delta(){return v_delta;};
double Periode::summe(){return v_summe;};
double Periode::peaks(){return v_peaks;};
double Periode::min(){return v_min;};
double Periode::max(){return v_max;};

double Periode::length(){return (double)size;};
double Periode::start(){return v_start;};
double Periode::freq(){return (double)sr/(double)size;};


void Periode::a_rms()
{
  sample_t *buff;
  v_rms=0;
  int s,l=size;
  s=l;
  buff=s_vec;
  while(l--)
    {
      v_rms+=fabs(*buff)/s;
      buff++;
    }
}

void Periode::a_delta()
{
  sample_t *buff,old=0;
  v_delta=0;
  int s,l=size;
  s=l;
  buff=s_vec;
  while(l--)
    {
      v_delta+=fabs(*buff-old);
      old=*buff;
      buff++;
    }
}

void Periode::a_summe()
{
  sample_t *buff;
  float v;
  v_summe=0;
  int l=size;
  buff=s_vec;
  while(l--)
    {
      v=*buff;
      v_summe+=fabs(v);
      buff++;
    }
}

void Periode::a_peaks()
{
  int off=1;
  v_peaks=0;
  sample_t *buff_p,*buff_old,*buff;
  int l=size;
  buff_old=buff_p=buff=s_vec;

  while(l--)
    {
      if(l > off){
	while (*buff == buff[off]){
	  if (l > (off+1)) off++;
	  else break;
	}
	if ((*buff > *buff_old) && (*buff > buff[off]))
	  v_peaks++;
	if ((*buff < *buff_old) && (*buff < buff[1]))
	  v_peaks++;
      }
      if (1<off)
      off--;
      buff++;
      if (l != ((size)-1))
	buff_old++;
    }
}


void Periode::a_amp()
{
  sample_t *buff;
  float v;
  v_amp=0;
  int s,l=size;
  s=l;
  buff=s_vec;
  while(l--)
    {
      v=fabs(*buff);
      if(v_amp<v)
	v_amp=v;
      buff++;
    }
}

void Periode::a_min()
{
  sample_t *buff;
  float v;
  v_min=0;
  int s,l=size;
  s=l;
  buff=s_vec;
  while(l--)
    {
      v=*buff;
      if(v_min>v)
	v_min=v;
      buff++;
    }
  v_min=fabs(v_min);
}

void Periode::a_max()
{
  sample_t *buff;
  float v;
  v_max=0;
  int s,l=size;
  s=l;
  buff=s_vec;
  while(l--)
    {
      v=*buff;
      if(v_max<v)
	v_max=v;
      buff++;
    }
  v_max=fabs(v_max);
}

void Periode::print(){
  cout << start() << "\t";
  //cout << start+size << "\t";
  cout << size << "\t";
  //cout << pos << "\t";
  cout << amp()<< "\t";
  cout << rms()<< "\t";
  cout << peaks()<< "\t";
  cout << delta()<< "\t"<<endl<<flush;
}

void Periode::write_analyse(ofstream *f){
  //char flo='1';
  double s=start();
  double l=length();
  double a=amp();
  double r=rms();
  double p=peaks();
  double d=delta();

  //  cout << d << " " << flush;
  //f->wri(reinterpret_cast<char*>(&flo), sizeof (double));
  f->write(reinterpret_cast<char*>(&s),sizeof(double));
  f->write(reinterpret_cast<char*>(&l),sizeof(double));
  f->write(reinterpret_cast<char*>(&a),sizeof(double));
  f->write(reinterpret_cast<char*>(&r),sizeof(double));
  f->write(reinterpret_cast<char*>(&p),sizeof(double));
  f->write(reinterpret_cast<char*>(&d),sizeof(double));

    /*
  ds.writeFloat(start);
  //ds.writeFloat(ende+1);
  ds.writeFloat(length+1);
  //ds.writeFloat(pos);
  ds.writeFloat(amp);
  ds.writeFloat(rms);
  ds.writeFloat(peaks);
  ds.writeFloat(delta); */
}
