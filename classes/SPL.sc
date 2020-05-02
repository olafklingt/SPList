+ SPList{
	//using splist to resample a file ... kind of unnecessary
	*resample{|infname,outfname,sr,transpose|
		var spl;
		spl=SPList.new;
		spl.readFile(infname,2,{
			spl.do(_.transpose(transpose));
			spl.write(outfname,sr);
		});
	}

	//strange stuff
	putSubDo{|function ... otherargs|
		var hier;
		if(this.parent.notNil,{
			this.parent.replace(this,hier=SPList.newUsing([this],this.parent));
			hier.do(function,*otherargs);
		});
	}

	//combined effects
	grainEnv{arg levels=[0,1,1,0],times=[1,1,1],curves='welch',releaseNode=nil,loopNode=nil;
		var n=SPEnv.new(levels,times,curves,releaseNode,loopNode);
		this.do(_.env(n));
	}
	pitchShift{|fx=\transpose ... args|
		var orgpos=0;
		var newpos=0;
		var is;
		var test;
		this.do{|i|
			test=(orgpos<newpos);
			is=i.sum(\size);
			orgpos=orgpos+is;
			if(test,{
				i.delete
			},{
				if(fx.class==Function,{
					fx.(this,*args);
				},{
					i.perform(fx,*args);
				});
				is=i.sum(\size);
				newpos=newpos+is;
			})
		}
	}


	pitchShiftLoop{|fx=\transpose ... args|
		var orgpos=0;
		var newpos=0;
		var is;
		var test;
		this.do{|i|
			test=(orgpos<newpos);
			is=i.sum(\size);
			orgpos=orgpos+is;
			if(test,{
				i.delete
			},{
				if(fx.class==Function,{
					fx.(i,*args);
				},{
					i.perform(fx,*args);
				});
				is=i.sum(\size);
				newpos=newpos+is;
				while({newpos<orgpos},{
					i.loop(1);
					is=i.sum(\size);
					newpos=newpos+is;
				})
			})
		}
	}

	timeStretch{arg newtime;
		var oldtime=this.length;
		if(oldtime>newtime,{
			while({oldtime>newtime},{
				array.do{|i|
					if(oldtime>newtime,{
						oldtime=oldtime-i.length;
						i.delete;
					})
				}
			});
			this.prDellistDo;
		},{
			while({oldtime<newtime},{
				array.do{|i|
					if(oldtime<newtime,{
						oldtime=oldtime+i.length;
						i.loop(1);
					})
				}
			});
			this.prAddlistDo;
		});
	}

	timeStretchRand{arg newtime;
		var oldtime=this.length;
		if(oldtime>newtime,{
			while({oldtime>newtime},{
				Array
				.series(array.size,0,1)
				.scramble
				.do{|i|
					if(oldtime>newtime,{
						oldtime=oldtime-array[i].length;
						array[i].delete;
					})
				}
			});
			this.prDellistDo;
		},{
			while({oldtime<newtime},{
				Array
				.series(array.size,0,1)
				.scramble
				.do{|i|
					if(oldtime<newtime,{
						oldtime=oldtime+array[i].length;
						array[i].loop(1);
					})
				}
			});
			this.prAddlistDo;
		});
	}
	deGroup{
		this.array.do{|i|
			this.parent.addlist.add([this,i]);};
		this.delete;
	}

	transposeCurve{|p1,p2,p3|
		var s=this.size;
		var h=s/2;
		var e=Env([p1,p2,p3],[h-1,h+1]);
		this.do{|i,j|
			i.transposeCurve(e.at(j),e.at(j+0.05),e.at(j+1));
		}
	}
}

+ Period{
	transposeCurve{|p1,p2,p3|
		this.transpose(p2);
	}
}


+ SimpleNumber{
	percent{arg size;
		^this*size/100;
	}
}

+ SoundFileAnalysis {
	analyzeFileForSPList { |path, start = (0), duration, which, maxDataPoints = (1000)|
		var result = ();
		var nix1=if(File.exists(path).not) { "\nFile not found: %\n".format(path).warn; ^this };
		var nix2=which = (which ?? { analysisMethods.keys.as(Array).sort }).asArray;
		// fork {
		var resultpaths, oscpath, score;
		var analysisDuration, soundFile, cond;
		var server = Server(("dummy"++counter).asSymbol);
		counter=counter+1;

		// get duration and numChannels from soundFile
		soundFile = SoundFile.openRead(path);
		analysisDuration = min(duration ?? { soundFile.duration - start }, soundFile.duration)+(1/soundFile.sampleRate);//todo made by me roughly
		analysisDuration=analysisDuration.roundUp(1/soundFile.sampleRate);

		// (analysisDuration*soundFile.sampleRate);

		soundFile.close;

		// first we build a score
		score = Score.new;
		resultpaths = this.prAddAnalysisToScore(score, which, server, analysisDuration, soundFile, maxDataPoints, start);

		// cond = Condition.new;

		// then we record it
		// osc file path, output path, input path - input is soundfile to analyze
		// actually this isn't really needed, but I leave it in here.
		oscpath = PathName.tmp +/+ UniqueID.next ++ ".osc";
		score.recordSyncNRT(oscpath, "/dev/null",
				//path,
			sampleRate: soundFile.sampleRate,
			options: ServerOptions.new
			.verbosity_(-1)
			//.memSize_(8192 * which.size) // REALLY NEEDED?
			.memSize_(8192 * 256) // REALLY NEEDED?
			.blockSize_(1)
			.sampleRate_(soundFile.sampleRate),
			// action: { cond.unhang }  // this re-awakens the process after NRT is finished
		);

		// result.put(\kill, { systemCmd("kill" + server.pid) });
		// cond.hang;  // wait for completion

		this.prReadAnalysisFiles(result, resultpaths, which, maxDataPoints);

		File.delete(oscpath);
		Server.all.remove(server);

		result.use {
			try { ~fileName = path.basename }; // seems not to work properly under ubuntu
			~path = path;
			~analysisStart = start;
			~analysisDuration = analysisDuration;
			~fileNumChannels = soundFile.numChannels;
			~dataDimensions = which;
			~dataTable = { ~dataDimensions.collect { |name| result.at(name) } };
		};

		// callback.value(result);
		// };
		^result
	}
}


+ Score {
	recordSyncNRT { arg oscFilePath, outputFilePath, inputFilePath, sampleRate = 44100, headerFormat =
		"AIFF", sampleFormat = "int16", options, completionString="", duration = nil, action = nil;
		if(oscFilePath.isNil) {
			oscFilePath = PathName.tmp +/+ "temp_oscscore" ++ UniqueID.next;
		};
		this.writeOSCFile(oscFilePath, 0, duration);
		systemCmd(program + " -N" + oscFilePath.quote
			+ if(inputFilePath.notNil, { inputFilePath.quote }, { "_" })
			+ outputFilePath.quote
			+ sampleRate + headerFormat + sampleFormat +
			(options ? Score.options).asOptionsString
			+ completionString, action);
	}

	*recordSyncNRT { arg list, oscFilePath, outputFilePath, inputFilePath, sampleRate = 44100,
		headerFormat = "AIFF", sampleFormat = "int16", options, completionString="", duration = nil, action = nil;
		this.new(list).recordNRTSync(oscFilePath, outputFilePath, inputFilePath, sampleRate,
			headerFormat, sampleFormat, options, completionString, duration, action);
	}

}