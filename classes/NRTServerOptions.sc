NRTServerOptions
{
	classvar <>server;
	var <>outputFilename;
	var <>oscFilename;
	var <>inputFilename;
	var <>sampleRate;
	var <>headerFormat;
	var <>sampleFormat;
	var <>outputChannels;
	var <>verbosity;
	*initClass {
		//because the server program is set by the platform after startup and not during intialization i copied the default settings from the different Platforms here.
		Platform.case(
			\osx,       {
				server="exec %/scsynth".format((Platform.resourceDir +/+ "../Resources").shellQuote);
			},
			\linux,     {
				server="exec scsynth";
			},
			\windows,   {
				"this package has not been used on windows if it works or not would be interesting to know please send a message to the developer on github or email.".debug("WARNING:");
				server="scsynth.exe";
			}
		);

	}
	*new{arg ofn,oscfn,ifn,sr,hf,sf,oc,v;
		^super.new.init(ofn,oscfn,ifn,sr,hf,sf,oc,v);
	}
	init{arg ofn,oscfn,ifn,sr,hf,sf,oc,v;
		if(ofn.notNil&&oscfn.isNil,{oscFilename=ofn++".osc"},{oscFilename=oscfn});
		if(ifn.isNil,{inputFilename="_"},{inputFilename=ifn});
		outputFilename=ofn;
		if(sr.isNil,{sampleRate=Server.default.sampleRate?44100},{sampleRate=sr});
		if(hf.isNil,{headerFormat="WAV"},{headerFormat=hf});
		if(sf.isNil,{sampleFormat="float"},{sampleFormat=sf});
		if(oc.isNil,{outputChannels=1},{outputChannels=oc});
		if(v.isNil,{verbosity= -2},{verbosity=v});
	}
	asOptionsString{
		var o=" -N ";
		if(oscFilename.isNil,{"warning: asOptionsString: oscFilename.isNil"});
		o=o+"\""++oscFilename++"\"";
		o=o+"\""++inputFilename++"\"";
		if(outputFilename.isNil,{"warning: asOptionsString: outputFilename.isNil"});
		o=o+"\""++outputFilename++"\"";
		o=o+sampleRate;
		o=o+headerFormat;
		o=o+sampleFormat;
		o=o+"-o"+outputChannels;
		o=o+"-V"+verbosity;
		^o;//.debug(\NRTServerOptionsAsOptionsString);
	}
	write{
		(NRTServerOptions.server++this.asOptionsString).systemCmd; // -o 1 is mono output
	}
	deleteOSCFile{
		("rm "++this.oscFilename).systemCmd;
	}
	*tempfileOptionsString{
		^super.new.init(PathName.tmp +/+ "splist_tmp_"++BeatSched.time.asInteger++".wav");
	}

}
