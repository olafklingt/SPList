INCDIR = /usr/local/include/
OBJ = main.o periode.o sfbuffer.o
LIBS = `pkg-config sndfile --libs`
# FLAGS = -std=c++20 -Ofast -Wall -I$(INCDIR)
FLAGS = -std=c++20 -g -Wall -I$(INCDIR)

all: splistanalyst

%.o: %.cpp
	g++ -c $(FLAGS) -o $@ $<

splistanalyst: $(OBJ)
	g++ -o splistanalyst $(OBJ) $(LIBS)

install:
	cp splistanalyst /usr/local/bin
