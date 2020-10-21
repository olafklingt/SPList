# SPList

SPlist is a set of classes for non-realtime-soundfile-transformation, that treats sound-files as list of Periods. The name derives from SortablePeriodList.


# Installation

Copy the SPList folder in your supercollider extensions folder or use the SuperCollider Quark GUI.

```supercollider
Quarks.gui
```

On linux simply compile splistanalyst with
```
make
make install
```

Now splistanalyst is not necessary anymore but recommended because tested.


## How to use

For the most reliable and well tested analysis of a sound file you need to have the programm "splistanalyst" installed. In the moment I make the assumption that "splistanalyst" would be only installed on linux.

Since the last version of this library it is possible to not use "splistanalyst" and analyse the sound-file within SuperCollider.

Unlike usual handling of arrays in SuperCollider SPList is like a List that is operating on its own content, it is not creating a new copy!

# Examples

```supercollider
//start
s.boot

// the play method creates wav files in your /tmp/ folder maybe you have to delete them yourself when you restart your computer seldome

//load file
a=SPLSourceFile.newSPL(2,"test.wav");//choose your example file

//play file
a.play // plays file after being reconstructed

//transform: transpose each period differently
a.do{|i|i.transpose(rrand(0.9,1.1))}.play

//sort the list by the amplitude of each period
a.sortBy(\amp).play;

```

# Disclaimer

It is an old project of mine. The code is sometimes a bit obscure.

# License
All the code is licensed under GPL v3.0
