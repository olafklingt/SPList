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
