
Period{
	var	<>todobegin,<>todolist,<>todoend;
	var <>parent;
	var <>splsourcefile;
	var <>start,<>end,size,amp,rms,<>mult,<>pos,<>delta,<>peaks;
	//var <>localpos;
	//var <>localdeep;

	do{//dummy to reduce tests
	}
	doDeep{//dummy to reduce tests
	}
	/*setLocalPos{arg i=0,d=0;
		localpos=i;
		localdeep=d;
		}*/
	testAllParents{
		^true;
	}
	*newUsing{arg spbl,par,position,start,length,amp,rms,peaks,delta;
		^super.new.init(spbl,par,position,start,length,amp,rms,peaks,delta);
	}
	*newSine{arg size,aamp,posi,ot=1,par;
		^super.new.sineInit(size,aamp,posi,ot,par);

	}
	sineInit{arg argsize,argamp,argpos,ot,par;
		todolist=LinkedList.new;
		todobegin=
		{[\s_new, \sineplayer,-1,1,0,
			\size,size,
			\amp,mult,
			\ot,ot]};
		todoend={[\s_new, \buscopy,-1,1,0,\size,size]};
		parent=par;
		mult=argamp;
		start=0;
		end=start+argsize;
		this.size=argsize;
		if(size==0,{"nuller!!!".postln});
		pos=pos;
		amp=1;
		rms=0.5;//kann man ausrechnen;
		peaks=ot*2;
		delta=0.5//kann man ausrechnen;
	}
	init{arg argspbl,argpar,argpos,argstart,arglength,argamp,argrms,argpeaks,argdelta;
		todolist=LinkedList.new;
		parent=argpar;
		splsourcefile=List.new;
		splsourcefile.add(argspbl);
		todobegin=
			{[\s_new, \periodplayer,-1,1,0,
				\bufnum,splsourcefile[0].bufnum,
					\start,start,
					\end,end,
					\size,size,
					\mult,mult]};
		todoend={[\s_new, \buscopy,-1,1,0,\size,size,\bufnum,splsourcefile[0].bufnum]};
		mult=1;
		start=argstart;
		end=argstart+arglength;
		this.size=arglength;
		if(size==0,{"nuller!!!".postln});
		pos=argpos;
		amp=argamp;
		rms=argrms;
		peaks=argpeaks;
		delta=argdelta;
	}
	size{
		// size.debug(\period_size);
		^size;
	}
	size_{arg newsize;
		size=newsize.round;
		if(newsize>960000){
			"new size is too big".postln;
			this.class.publicInstVars.collect{|v|v.post;": ".post;this.perform(v).postln};
		};
	}
	amp{
		^(amp*mult);
	}
	//amp_{arg newVal;
	//amp=newVal;
	//}
	rms{
		^(rms*mult);
	}
	//rms_{arg newVal;
	//rms=newVal;
	//}
	freq{
		^(splsourcefile[0].sampleRate/size);
	}
	freq_{arg newValue;
		size = splsourcefile[0].sampleRate/newValue;
	}
	sec{
		^(size/splsourcefile[0].sampleRate);
	}
	sec_ { arg newValue;
		size = splsourcefile[0].sampleRate*newValue; //???
	}
	length{
		^size;
	}
	//looped die periode
	loop{arg n;
		n.do{parent.addlist.add([this,this.copy])};
	}
	appendIntoParent{arg c;
		parent.addlist.add([this,c]);
	}
	//loescht die perio3de
	delete{
		parent.dellist.add(this);

		//this.clear;//also wie in shwobl2-period2.sc steht machen
		//anders muss das hier sein
		size=0;
		mult=0;
	}
	// clean{
	// }
	// ValueFunctions
	sum{|type|
		if(type.class==Function,
			{^type.value(this)},
			{^this.perform(type)});
	}
	avr{|type|
		if(type.class==Function,
			{^type.value(this)},
			{^this.perform(type)});
	}
	small{|type|
		if(type.class==Function,
			{^type.value(this)},
			{^this.perform(type)});
	}
	big{|type|
		if(type.class==Function,
			{^type.value(this)},
			{^this.perform(type)});
	}

	subsum{|type,bund|
		^this.sum(type);
	}
	subavr{|type,bund|
		^this.avr(type);
	}
	subsmall{|type,bund|
		^this.small(type);
	}
	subbig{|type,bund|
		^this.big(type);
	}

	//displayfunction
	print{|type|
		^this.perform(type).post;
	}

	//ungroup
	deepsum{
		^1;
	}
	flat{arg nar;
		nar.add(this);
	}
	flatten{arg level,nar;
		nar.add(this);
	}

	performOnPeriod{arg method ... args;
		var oargs=args.collect{|i| i.value(this)};
		this.perform(method,*oargs);
	}
	species { ^Period }

	copy{
		^this.shallowCopy;
	}
	deepCopy{
		var ou=this.shallowCopy;
		^(ou.todolist=todolist.deepCopy;)
	}
	dup { arg n = 2;
		^SPList.newUsing(Array.fill(n, { this.copy }));
	}

	// TRANSFORMATION
	// prefunctions damit hier der size faktor richtig gestellt werden kann
	// samples
	transposeToSize{arg newsize;
		if(newsize<2,{newsize=2});
		this.size=newsize;
	}
	// sekunden
	transposeToSec{arg newSec;
		this.transposeToSize((newSec*splsourcefile[0].sampleRate).asInteger);
	}
	// frequenz
	transposeToFreq{arg newfreq;
		this.transposeToSize((splsourcefile[0].sampleRate/newfreq).asInteger);
	}
	// samples*faktor
	transpose{arg mult;
		this.transposeToSize((size*mult).asInteger);
	}
	gain{arg multv;
		mult=mult*multv;
	}
	addEffect{arg name ... args;
		todolist.add({[\s_new, name,-1,1,0,\size,size]++args});
	}
	addSine{arg ot,sinamp=1,inamp=1;
		todolist.add({[\s_new, \sinemerge,-1,1,0,
			\size,size,
			\ot,ot,
			\sinamp,mult*amp*sinamp,
			\inamp,inamp]});
	}
	merge{arg aPeriod,pan=0,sizeBlend=0;
		var sppos=splsourcefile.size;
		splsourcefile.add(aPeriod.splsourcefile[0]);
		todolist.add({[\s_new, \periodmerge,-1,1,0,
			\bufnum,splsourcefile[sppos].bufnum,
			\start,aPeriod.start,
			\end,aPeriod.end,
			\size,size,
			\mult,mult,
			\pan,pan]});
		//ungenaue annäherungen !!:--(((
		amp=[amp,aPeriod.amp].blendAt(pan/2+0.5);
		rms=[rms,aPeriod.rms].blendAt(pan/2+0.5);
		delta=delta+aPeriod.delta/2;
		peaks=peaks+aPeriod.peaks;
		//[size,[size,aPeriod.size],[size,aPeriod.size].blendAt(sizeBlend/2+0.5)].postln;
		this.size=[size,aPeriod.size].blendAt(sizeBlend/2+0.5);
	}

	env{arg envelope,max=0,from=0;
		var to=0;
		var dsize=this.size;
		if(max<dsize,{max=dsize});
		if(envelope.size<dsize,
			{envelope.size=max=dsize;parent.addenv(envelope,splsourcefile[0].sampleRate)}
		);
		to=from+dsize;
		todolist.add({[\s_new, \envplayer,-1,1,0,
				\samplebuf,splsourcefile[0].bufnum,
				\bufnum,envelope.bufnum,
					\start,(from/max)*envelope.size,
					\end,(to/max)*envelope.size,
					\size,size,
						\mult,1]});
	}
	nrts_buf{arg af;
		splsourcefile.do{|spbl,i|
			if(not(af.includes(spbl)),{af.add(spbl)});
			spbl.bufnum=1000+af.indexOf(spbl)
		};
	}
	node{arg arr,startpos;
		this.size=this.size;
		arr.add(PeriodNode.new(this,startpos));
		^this.sec;//hm praktisch aber vieleicht zu viel aufeinmal?
	}
	nodecount{
		^1;
	}
	howDeep{arg deep=0;
		^(deep+1);
	}
	reset{
		todolist=LinkedList.new;
		splsourcefile=List.new.add(splsourcefile[0]);
		mult=1;
		size=end-start;
	}
	plot{ arg name, bounds, minval = -1.0, maxval = 1.0, parent, labels=true;
		Buffer.read(
			Server.default,
			splsourcefile[0].soundfilename,
			start,
			end-start,
			action: {|b|
				b.plot(name, bounds, minval, maxval, parent, labels)
			});
	}
}
