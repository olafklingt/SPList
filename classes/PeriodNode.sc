//PeriodNode ist eine Klasse die es ermoeglicht
//Die OSC nachrichten zu organisieren die benoetigt werden
//um eine Periode im NRT-mode wiederzugeben
//dieshier scheint mir eine gro�e hilfe zu sein
//

PeriodNode{
	var period;
	var <>startpos;
	*initClass {
		StartUp.add{
			SynthDef("periodmerge".debug(\pn),{arg bufnum,start,end,size,mult,pan;
				var env=EnvGen.ar(Env.new([1,1,0], [size/BufSampleRate.ir(bufnum),0],[0,0]).debug(\env), doneAction: 2);
				var sam=PlayBuf.ar(1,bufnum,((end-start)/SampleRate.ir)/(size/BufSampleRate.ir(bufnum)),startPos: start)*mult*env;
				var sig=LinXFade2.ar(In.ar(100),sam,pan);
				ReplaceOut.ar(100,sig);
			}).writeDefFile;
			SynthDef("periodplayer".debug(\pn),{arg bufnum,start,end,size,mult;
				var env=EnvGen.ar(Env.new([1,1,0], [size/BufSampleRate.ir(bufnum),0]), doneAction: 2);
				var sig=PlayBuf.ar(1,bufnum,((end-start)/SampleRate.ir)/(size/BufSampleRate.ir(bufnum)),startPos: start);
				ReplaceOut.ar(100,sig*env*mult);
			}).writeDefFile;
			SynthDef("envplayer".debug(\pn),{arg samplebuf,bufnum,start,end,size,mult;
				var env=EnvGen.ar(Env.new([1,1,0], [size/BufSampleRate.ir(samplebuf),0]), doneAction: 2);
				var sig=PlayBuf.ar(1,bufnum,((end-start)/SampleRate.ir)/(size/BufSampleRate.ir(samplebuf)),startPos: start);
				ReplaceOut.ar(100,sig*env*In.ar(100));
			}).writeDefFile;
			SynthDef("sinemerge".debug(\pn),{arg size,ot,sinamp,inamp;
				var env=EnvGen.ar(Env.new([1,1,0], [size/SampleRate.ir,0]), doneAction: 2);
				var sam=SinOsc.ar(ot*SampleRate.ir/size,0,sinamp);
				var sig=In.ar(100)*inamp+sam;
				ReplaceOut.ar(100,sig*env);
			}).writeDefFile;
			SynthDef("sineplayer".debug(\pn),{arg size,mult,ot;
				var env=EnvGen.ar(Env.new([1,1,0], [size/SampleRate.ir,0]), doneAction: 2);
				var sig=SinOsc.ar(ot*SampleRate.ir/size,0,mult);
				ReplaceOut.ar(100,sig*env*mult);
			}).writeDefFile;
			//in dem kopieren entsteht noch eine �berlagerung von 1nem sample
			//muss man in java weg machen
			SynthDef("buscopy".debug(\pn),{arg size,bufnum;
				var env=EnvGen.ar(Env.new([1,1,0], [size/BufSampleRate.ir(bufnum),0]), doneAction: 2);
				OffsetOut.ar(0,In.ar(100)*env);
			}).writeDefFile;
		}
	}
	*new{arg period,startpos;
		^super.new.init(period,startpos);
	}
	init{arg p,s;
		period=p;
		startpos=s;
	}
	write{|file|
		var oscmsg;
		oscmsg=[startpos,period.todobegin.value].asRawOSC;
		file.write(oscmsg.size);
		file.write(oscmsg);
		period.todolist.do{|item|
			oscmsg=[startpos,item.value].asRawOSC;
			file.write(oscmsg.size);
			file.write(oscmsg);
		};
		oscmsg=[startpos,period.todoend.value].asRawOSC;
		file.write(oscmsg.size);
		file.write(oscmsg);
	}
}

