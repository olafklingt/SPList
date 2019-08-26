// vieleicht sollte spenv kein env sein sondern object
// also ein wrapper innerhalb dessen man den env austauschen
// kann

SPEnv : Env {
	var <>filename;
	var <>bufnum;
	var <>sampleRate;
	var <>size=0;
	write {
		var file;
		file = SoundFile.new;
		file.sampleRate=sampleRate;
		file.openWrite(filename);
		if (file.notNil, {
			file.writeData(this.asSignal(size));
			file.close;
		},{
			"file is NIL".postln;
		});
	}
}