INCDIR = /usr/local/include/
OBJ = main.o periode.o
LIBS = `pkg-config sndfile --libs`
FLAGS = -g -Wall -I$(INCDIR) 

all: splistanalyst

%o: %cpp
	g++ -c $(FLAGS) -o $@ $<

splistanalyst: $(OBJ)
	g++ -o splistanalyst $(OBJ) $(LIBS)

install: 
	cp splistanalyst /usr/local/bin
