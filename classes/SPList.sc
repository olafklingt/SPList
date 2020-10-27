SPLSourceFile{
	classvar <>program;
	classvar <>programargs;
	var <>minsize;
	var <>minvol;
	var <>soundfilename,<>anafilename,<>sampleRate;
	var <>bufnum=nil;
	*initClass {
		Platform.case(
			\osx,       {
				program=nil;
				programargs=nil;
			},
			\linux,     {
				if("type -P \"splistanalyst\"".systemCmd==0){
					program="splistanalyst";
					programargs="";
				}{
					program=nil;
					programargs=nil;
				}
			},
			\windows,   {
				program=nil;
				programargs=nil;
			}
		);

	}
	// returns a SPLSourceFile which is normaly not used
	*new{arg minsize,soundfilename,anafilename,minvol=0;
		^super.new.init(soundfilename,anafilename,minsize,minvol);
	}
	// returns a SPList
	*newSPL{arg minsize,soundfilename,anafilename,minvol=0;//minvol is not implemented in splistanalyst!!!
		var splsf=super.new.init(soundfilename,anafilename,minsize,minvol);
		splsf.analyseSoundfile;
		^splsf.getNewSPL;
	}
	init{arg fn,an,ms,mv;
		minsize=ms;
		minvol=mv;
		soundfilename=fn;
		if(an.isNil,
			{anafilename=fn++".jspbl"++ms},
			{anafilename=an}
		);
	}
	// starts the analyse command only when its not done allready
	analyseSoundfile{
		var orgfile,com,shouldAna;
		var soundfile=SoundFile.new;
		var afe=File.exists(anafilename).debug(\afe);
		soundfile.openRead(soundfilename);
		this.sampleRate=soundfile.sampleRate;
		soundfile.close;
		shouldAna=if(afe){File.mtime(soundfilename).debug(\sft)>File.mtime(anafilename).debug(\aft)}{true};
		if(shouldAna.debug(\sa)){
			if(program.isNil.debug(\pin)){
				this.prNoProgWriteAnaFile(soundfilename,anafilename);
			}{
				com=program+programargs;
				com=com+"\""++soundfilename++"\"";
				com=com+"\""++anafilename++"\"";
				com=com+minsize;
				com=com+minvol;
				com.systemCmd.postln;
				if(File.exists(anafilename),{
					"analysis success".postln;
				},{
					("file should exist but does not:"++anafilename).postln
				});
			}
		}{
			("file exists:"+anafilename+"no need to analyze again.").postln
		};
	}

	prFillPeriodArray{arg parent;
		var ana=File.new(anafilename, "rb");
		parent.array=Array.fill(ana.length/48,{|i|
			Period.newUsing(this,parent,i,*({ana.getDoubleLE}!6))});
	}

	// reads the data from the analyse-file and put it into a new SPList
	getNewSPL{
		var spl=SPList.new;
		this.prFillPeriodArray(spl);
		^spl;
	}

	prNoProgGetAnz{|fn|
		var f=SoundFile.openRead(fn).postln;
		var nf= f.numFrames;
		var sfa=SoundFileAnalysis();
		var result;
		sfa.add(\pn, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var ct = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);

			var count = PulseCount.ar(ct);
			var wt = Phasor.ar(0,1,nf * -1 +1,inf);
			[wt, count]
		});
		result=sfa.analyzeFileForSPList(fn,0,nf);
		^(result.pn.last+1);
	}

	prNoProgWriteAnaFile{|sfn,afn|
		var result;
		var f=SoundFile.openRead(sfn).postln;
		var afh=File(afn,"wb");
		var nf= f.numFrames;
		var anz=this.prNoProgGetAnz(sfn);
		var sfa=SoundFileAnalysis();
		sfa.add(\stt, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var end = Latch.ar(Phasor.ar(0, 1, 0, inf),trig);
			var start = Latch.ar(Delay1.ar(end),trig);
			[trig, start]
		});
		sfa.add(\end, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var end = Latch.ar(Phasor.ar(0, 1, 0, inf),trig);
			[trig, end]
		});
		sfa.add(\sum, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var sum = Integrator.ar(Delay1.ar(sig).abs,1-trig);
			var out = Latch.ar(sum,trig);
			[trig, out]
		});
		sfa.add(\dlt, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var a=Delay1.ar(sig);
			var b=Delay1.ar(Select.ar(trig,[a,DC.ar(0)]));
			// var old =
			var sum = Integrator.ar((a-b).abs,1-trig);
			var out = Latch.ar(sum,trig);
			// Poll(trig,sum,\s);
			// Poll(trig,out,\o);
			// Poll(trig,p,\p);
			[trig, out]
		});
		sfa.add(\amp, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var sigd=Delay1.ar(sig);
			var sum = Amplitude.ar(sigd,0,Delay1.ar(Select.ar(trig,[DC.ar(inf),DC.ar(0)])));
			var out = Latch.ar(sum,trig);
			[trig, out]
		});
		sfa.add(\pek, \trig, { |sig|
			var p=Phasor.ar(0, 1, 0, inf);
			var trig = (Delay1.ar(sig)<0)&(sig>=0)|((p-nf+1)>0);
			var p1=Delay1.ar(sig);
			var p2=Delay1.ar(p1);
			var p3=Delay1.ar(p2);
			var pt=((p1<p2)&(p2>p3))|((p1>p2)&(p2<p3));
			var count = PulseCount.ar(pt,trig);
			[trig, count]
		});

		result=sfa.analyzeFileForSPList(sfn,0,nf,maxDataPoints: anz);
		// Period.newUsing(

		result[\stt].size.do{|i|
			var start=result[\stt][i];
			var length=result[\end][i]-start;
			var amp=result[\amp][i];
			var sum=result[\sum][i];
			var rms=sum/length;
			afh.putDoubleLE(start);
			afh.putDoubleLE(length);
			afh.putDoubleLE(amp);
			afh.putDoubleLE(rms);
			afh.putDoubleLE(result[\pek][i]);
			afh.putDoubleLE(result[\dlt][i]);

			// parent.array=Array.fill(ana.length/48,{|i|
			// Period.newUsing(this,parent,i,*({ana.getDoubleLE}!6))});
		};
		afh.close;
	}
}

SPList : Collection{
	var <>array,<>parent=nil;
	var <>addlist,<>dellist;
	var allenvs;

	// creates a new empty SPList
	*new{
		^super.new.init;
	}

	init{
		array=Array.new;
		addlist=LinkedList.new;
		dellist=LinkedList.new;
	}

	*copyInstance { arg aSPList;
		^super.new.init.array_( aSPList.array.copy )
	}

	prSetParent{
		array.do{|item| item;item.parent=this};
	}

	// to reuse an Array in a SPList
	*newUsing{arg anArray,parent=nil;
		^super.new.initUsing(anArray,parent)
	}
	initUsing{arg anArray,par;
		array=anArray;
		addlist=LinkedList.new;
		dellist=LinkedList.new;
		this.parent=par;
		this.prSetParent;

	}

	/*	// klar oder?
	*newClear{arg size,par=nil;
	var out;
	^super.newClear(size).addlist_(LinkedList.new).dellist_(LinkedList.new);
	}
	*/
	prTestAllParents{|k=0|
		array.do{|i|
			//this.do{|i|
			k=k+1;
			if((i.parent===this).not,{"i.parent!=this".postln;SPList.par.last.postln;i.parent.postln;k.postln;^false});
			if((i.testAllParents(k)).not,{"!i.testAllParents".postln;k.postln;^false});
		}
		^true;
	}
	prAddlistDo{
		array=array.grow(addlist.size+array.size);
		addlist.do{|i,j|
			j=array.indexOf(i[0]);
			//mit degroup wird dieser index nicht mehr gefunden
			//da also ein remove frueher gegriffen hat als ein add?
			array=array.insert(j,i[1]);
		};
		addlist=LinkedList.new;
	}
	prDellistDo{
		dellist.do{|i|
			array.remove(i);
		};
		dellist=LinkedList.new;
	}

	do { arg function ... otherargs;
		array.do{|item,i|function.(item,i,*otherargs)};
		//perform processes which would have changed the indexes like loop and delete
		this.prAddlistDo;
		this.prDellistDo;
	}

	// perform a function like do but recursive on every layer
	doDeep{arg function,deep=0;
		this.do{|item|
			item.doDeep(function,deep+1);
		};
		function.(this,deep);
	}
	collect {|function|
		^SPList.newUsing(array.collect(function));
	}

	select { | function |
		^SPList.newUsing(array.select(function));
	}
	detect { | function |
		array.do {|elem, i| if (function.value(elem, i)) { ^elem } }
		^nil;
	}
	detectIndex { | function |
		array.do {|elem, i| if (function.value(elem, i)) { ^i } }
		^nil;
	}
	doAdjacentPairs { arg function;
		array.doAdjacentPairs(function);
	}

	// accessing
	at { arg i; ^array.at(i) }
	clipAt { arg i; ^array.at(i.clip(0,this.size -1))}
	wrapAt { arg i; ^array.at(i.wrap(0,this.size -1))}
	foldAt { arg i; ^array.at(i.fold(0,this.size -1))}

	put { arg i, item; item.parent_(this); ^array.put(i, item) }
	clipPut { arg i, item; i = i.clip(0, this.size - 1);item.parent_(this); ^array.put(i, item) }
	wrapPut { arg i, item; i = i.wrap(0, this.size - 1);item.parent_(this); ^array.put(i, item) }
	foldPut { arg i, item; i = i.fold(0, this.size - 1);item.parent_(this); ^array.put(i, item) }

	add { arg item;item.parent_(this); array = array.add(item); }
	addFirst { arg item;item.parent_(this); array = array.addFirst(item); }

	scramble{array=array.scramble;}

	first { if (this.size > 0, { ^array.at(0) }, { ^nil }) }
	last { if (this.size > 0, { ^array.at(this.size - 1) }, { ^nil }) }
	insert { arg index, item; item.parent_(this); array = array.insert(index, item); }
	removeAt { arg index;  ^array.removeAt(index); }
	remove { arg item;
		var index = this.indexOf(item);
		^if ( index.notNil, {
			this.removeAt(index);
		},{
			nil
		});
	}
	indexOf { arg item;
		this.array.do({ arg elem, i;
			if ( item === elem, { ^i })
		});
		^nil
	}
	copyFromStart{arg end;
		^this.copyRange(0, end)
	}
	copyToEnd { arg start;
		^this.copyRange(start, this.size - 1)
	}
	copyRange { arg start, end; ^this.shallowCopy.array_(array.copyRange(start, end))}
	copy { ^this.class.copyInstance(this) }
	deepCopy {var ou= this.shallowCopy;
		ou.array=array.collect{|i|i.deepCopy};
		ou.array.do(_.parent=ou);
		^ou;
	}
	replace{arg find, replace;
		var id=array.indexOf(find);
		array=array.put(id, replace);
	}

	//append a SPList to this SPList
	appendSPList{arg anSPList;
		this.array=this.array++anSPList.array;
		this.parent;
	}

	//sorting
	// sort must be  quickSort because otherwise sc da
	sort{arg func;
		this.quickSort(func);
	}
	sortBy{arg type,bund=\avr;
		this.quickSort{|a,b|a.perform(bund,type)<b.perform(bund,type)}
	}
	quickSort { arg function;
		this.quickSortRange(0, this.size - 1, function)
	}
	quickSortRange { arg i, j, function;
		//Sort elements i through j of this to be nondescending according to
		// function.
		var di, dij, dj, tt, ij, k, l, n;
		// The prefix d means the data at that index.
		if ((n = j + 1  - i) <= 1, { ^this });	// Nothing to sort.
		//Sort di,dj.
		di = this.at(i);
		dj = this.at(j);
		if (function.value(di, dj).not, { // i.e., should di precede dj?
			this.swap(i,j);
			tt = di;
			di = dj;
			dj = tt;
		});
		if ( n > 2, { // More than two elements.
			ij = (i + j) div: 2;  // ij is the midpoint of i and j.
			dij = this.at(ij);  // Sort di,dij,dj.  Make dij be their median.
			if (function.value(di,  dij), {  // i.e. should di precede dij?
				if (function.value(dij, dj).not, {  // i.e., should dij precede dj?
					this.swap(j, ij);
					dij = dj;
				})
			},{ // i.e. di should come after dij"
				this.swap(i, ij);
				dij = di;
			});
			if ( n > 3, { // More than three elements.
				// Find k>i and l<j such that dk,dij,dl are in reverse order.
				// Swap k and l.  Repeat this programedure until k and l pass each other.
				k = i;
				l = j;
				while ({
					while ({
						l = l - 1;
						k <= l and: { function.value(dij, this.at(l)) }
					}); // i.e. while dl succeeds dij
					while ({
						k = k + 1;
						k <= l and: { function.value(this.at(k), dij) };
					}); // i.e. while dij succeeds dk
					k <= l
				},{
					this.swap(k, l);
				});
				// Now l<k (either 1 or 2 less), and di through dl are all less than or equal to dk
				// through dj.  Sort those two segments.
				this.quickSortRange(i, l, function);
				this.quickSortRange(k, j, function);
			});
		});
	}
	swap { arg i, j;
		var temp;
		temp = this[i];
		this[i] = this[j];
		this[j] = temp;
	}

	// ordering
	separate { arg function;//die kann auch noch besser gemacht werden mit einer testseparate funktion
		var sublist = this.species.new;
		var oldarr=this.array;
		array = Array.new;
		oldarr.doAdjacentPairs({ arg a, b, i;
			sublist = sublist.add(a);
			if ( function.value(a, b, i), {
				this.add(sublist);
				sublist = this.species.new;
			});
		});
		sublist = sublist.add(oldarr.last);
		this.add(sublist);
	}

	clump { arg groupSize;
		var list, sublist,nsize;
		nsize=((this.size/groupSize)+1).asInteger;
		if(this.size%groupSize==0,{nsize=nsize-1});
		list = Array.new(nsize);
		nsize.do({ arg i;
			var subarr=array.copyRange(i*groupSize,(i*groupSize)+groupSize-1);
			list.add(SPList.newUsing(subarr,this));
		});
		array=list;
	}



	// VALUEFUNCTIONS
	getArrayOfValues{arg type,bund;
		^this.array.collect{|item|
			item.perform(bund,type);
		}
	}
	getValuesInArray{arg type;
		^this.array.collect{|i|
			if(i.class===SPList
				,{i.getValuesInArray(type)}
				,{i.perform(type)});
		}
	}
	// summe aller werte eines typs
	sum{arg type;
		var val=0;
		this.array.do{|item|
			if(item.size>0,{
				val=val+(item.sum(type))})};
		^val;
	}
	// durchschnitt aller werte eines typs
	avr{arg type;
		var val=0,localsize=this.size;
		this.array.do{|item|
			if(item.size>0,{
				if(item.avr(type).isNil,{["error: itemavrtype is nil",item,\avr,type,localsize].postln});
				if(localsize.isNil,{["error: size is nil",item,\avr,type,localsize].postln});
				val=val+(item.avr(type)/localsize)})};
		^val;
	}
	// kleinster aller werte eines typs
	small{arg type;
		var val=2147483647;
		this.array.do{|i|var v=i.small(type);
			if(i.size>0,{
				if(val>v,{val=v})})};
		^val;
	}
	// groester aller werte eines typs
	big{arg type;
		var val= -2147483647;
		this.array.do{|i|var v=i.big(type);
			if(i.size>0,{
				if(val<v,{val=v})})};
		^val;
	}

	length{
		^this.sum(\size);
	}
	//avr auf der obersten ebene und dadrunter bund
	subavr{arg type,bund;
		var val=0,size=this.size;
		this.array.do{|item|
			if(item.size>0,{
				val=val+(item.perform(bund,type)/size)})};
		^val;
	}
	subsum{arg type,bund;
		var val=0;
		this.array.do{|item|
			if(item.size>0,{
				val=val+(item.perform(bund,type))})};
		^val;
	}
	subsmall{arg type,bund;
		var val=2147483647;
		this.array.do{|i|var v=i.perform(bund,type);
			if(i.size>0,{
				if(val>v,{val=v})})};
		^val;
	}
	subbig{arg type,bund;
		var val= -2147483647;
		this.array.do{|i|var v=i.perform(bund,type);
			if(i.size>0,{
				if(val<v,{val=v})})};
		^val;
	}

	// RECURSE SPLIT FUNCTIONS
	// teile deep mal in 2 teile (also 2.pow(deep))
	// mithilfe der angegebenen splitfunktion und ihrer argumente
	// wird die splitposition bestimmt
	splitN{arg deep,sfunc ... args;
		this.array=[SPList.newUsing(this.array,this)];
		while({deep>0}){
			this.do{|i|i.performList(sfunc,args)};
			this.flatten;
			deep=deep-1;
		};
	}

	// SPLITFUNCTIONS
	// teilt an der haelfte
	splitHalf{
		this.splitAfter((this.size/2).asInteger);
	}
	// teilt nach einem bestimmten punkt
	splitAfter{arg at;
		var
		a=this.copyFromStart(at),
		b=this.copyToEnd(at+1);
		a.parent_(this);
		b.parent_(this);
		if(a.size==0,{this.array=[b]},{
			if(b.size==0,{this.array=[a]},{
				this.array=[a,b]
			})
		})
	}
	// teilt bevor einem bestimmten punkt
	splitBefore{arg at;
		var
		a=this.copyFromStart(at-1),
		b=this.copyToEnd(at);
		a.parent_(this);
		b.parent_(this);
		if(a.size==0,{this.array=[b]},{
			if(b.size==0,{this.array=[a]},{
				this.array=[a,b]
			})
		})
	}
	// teilt an einem minimum eines types
	splitNextMin{arg area=100,type=\amp,bund=\avr;
		this.splitAfter(this.getNextMin(this.size.percent(area),type,bund=\avr));
	}
	splitNextMax{arg area=100,type=\amp,bund=\avr;
		this.splitAfter(this.getNextMax(this.size.percent(area),type,bund=\avr));
	}

	// splithelpfunctions
	getNextMin{arg area=this.size,type=\amp,bund=\avr;
		var out=0,pre=2147483647,val=2147483647,
		begin=((this.size-area)/2).asInteger,
		end=(begin+area).asInteger,
		mitte=(this.size/2).asInteger,
		dist=((area/2)+0.5).asInteger,
		dist2=((area/2)).asInteger;
		this.array.do{|item,i|
			if((i>=begin)&&(i<=mitte),{
				pre=(((1-((i-begin)/dist))/2)+0.5)*item.avr(type);
			});
			if((i>=mitte)&&(i<=end),{
				pre=(((1+((i-end)/dist2))/2)+0.5)*item.avr(type);
			});
			if(pre<val,{val=pre;out=i});
		};
		^out;
	}
	getNextMax{arg area=this.size,type=\amp,bund=\avr;
		var out=0,pre=0,val=0,
		begin=((this.size-area)/2).asInteger,
		end=(begin+area).asInteger,
		mitte=(this.size/2).asInteger,
		dist=((area/2)+0.5).asInteger,
		dist2=((area/2)).asInteger;
		this.array.do{|item,i|
			if((i>=begin)&&(i<=mitte),{
				pre=((((i-begin)/dist)/2)+0.5)*item.avr(type);
			});
			if((i>=mitte)&&(i<=end),{
				pre=(((-1*((i-end)/dist2))/2)+0.5)*item.avr(type);
			});
			if(pre>val,{val=pre;out=i});
		};
		^out;
	}

	// BUNDLEFUNCTIONS
	// fasst alle periodenzusammen zwischen zwei lokalen minima
	grainMin{arg type,bund=\avr;
		if(this.array.size>2,{this.grain(type,bund,\minTest)});
	}
	// fasst alle periodenzusammen zwischen zwei lokalen maxima
	grainMax{arg type,bund=\avr;
		if(this.array.size>2,{this.grain(type,bund,\maxTest)});
	}

	// bundlehelpfunctions
	minTest{arg  old_v,akt_v,next_v;
		^((old_v>akt_v)&&(akt_v<next_v));
	}
	maxTest{arg  old_v,akt_v,next_v;
		^((old_v<akt_v)&&(akt_v>next_v));
	}
	grainTest{arg type,bund,testf;
		var old_v,akt_v,akt_p,next_v,arrsize,out,count=2;
		if(array.size<3,{^array.size});
		old_v=this.array[0].perform(bund,type);
		akt_v=this.array[1].perform(bund,type);
		akt_p=3;
		next_v=this.array[2].perform(bund,type);
		arrsize=this.array.size;
		out=0;
		while({akt_p<arrsize},{
			if(this.perform(testf,old_v,akt_v,next_v),
				{
					out=out+1;
					count=1;
			},{count=count+1});
			if(akt_v!=next_v,{old_v=akt_v;});
			old_v=akt_v;
			akt_v=next_v;
			next_v=this.array[akt_p].perform(bund,type);
			akt_p=akt_p+1;
		});
		^(out+1);
	}
	grain{arg type,bund,testf;//problem ist das arrays kuerzer als 3 nicht abgefangen werden
		var nsize=this.grainTest(type,bund,testf),
		oldarr=this.array,
		old_v=oldarr[0].perform(bund,type),
		akt_v=oldarr[1].perform(bund,type),
		akt_p=2,
		next_v=oldarr[2].perform(bund,type),
		arrsize=oldarr.size,count=2,arr,last=0;
		this.array=Array.new(nsize);
		while({akt_p<arrsize},{
			if(this.perform(testf,old_v,akt_v,next_v),
				{
					arr=SPList.newUsing(oldarr.copyRange(last-1,last+count-2),this);
					this.add(arr);
					last=akt_p;
					count=1;
			},{count=count+1;});
			if(akt_v!=next_v,{old_v=akt_v;});
			akt_v=next_v;
			next_v=oldarr[akt_p].perform(bund,type);
			akt_p=akt_p+1;
		});
		if(count>0,{
			arr=SPList.newUsing(oldarr.copyRange(last,last+count),this);
			this.add(arr);
		});
	}

	// eine neue grainfunktion:
	// fasst immer alle perioden zusammen ab dem punkt wo ein Threshhold unterschritten wird (bis vor dass er wieder unterschritten wird)
	grainThreshNeg{arg threshhold,type,bund;
		this.separate({arg a,b; ((a.perform(bund,type)>=threshhold)&&(b.perform(bund,type)<threshhold))})
	}
	// fasst immer alle perioden zusammen ab dem punkt wo ein Threshhold �berschritten wird (bis vor dass er wieder �berschritten wird)
	grainThreshPos{arg threshhold,type,bund;
		this.separate({arg a,b; ((a.perform(bund,type)<=threshhold)&&(b.perform(bund,type)>threshhold))})
	}
	// fasst immer alle perioden zusammen ab dem punkt wo ein Threshhold durchschritten wird (bis vor dass er wieder durchschritten wird)
	grainThreshSplit{arg threshhold,type,bund;
		this.separate({arg a,b;
			(((a.perform(bund,type)>=threshhold)&&(b.perform(bund,type)<threshhold))||
				((a.perform(bund,type)<=threshhold)&&(b.perform(bund,type)>threshhold)))
		})
	}

	// ungroup
	// deepsum und flatten haben noch einen unterschiedlichen
	// umgang mit level
	//
	deepsum{arg level= -1;
		if(level==0,{^1},{^array.collect{|i| i.deepsum(level-1);}.sum})
	}
	flat{arg prnar=nil;
		if(prnar==nil,{prnar=Array.new(this.deepsum);
			array.do{|i|i.flat(prnar)};
			this.array=prnar;
			this.prSetParent;
		},{
			array.do{|i|i.flat(prnar)};
		});
	}
	flatten{arg level=1,prnar=nil;
		if(prnar==nil,{
			prnar=Array.new(this.deepsum(level+1));
			if(level<0,{
				prnar.add(this)
			},{
				array.do{|i|i.flatten(level-1,prnar)};
				this.array=prnar;
				this.prSetParent;
			})
		},{
			if(level<0,{//workaround
				prnar.add(this)
			},{
				array.do{|i|i.flatten(level-1,prnar)};
			})
		});
	}

	flatSub{|level|
		if(level>0,{this.array.do{|item|item.flatSub(level-1);}},{this.flat});
	}
	flattenSub{|levs,levd|
		if(levs>0,{this.array.do{|item|item.flattenSub(levs-1,levd);}},{this.flatten(levd);});
	}

	// eine post funktion von mir
	print{|type|
		"SPList[".post;
		this.array.do{|i|" ".post;i.print(type)};
		"]".post;
	}
	// ja das halt
	countOfPeriods{
		var counter=0;
		this.array.do{|i|counter=counter + i.countOfPeriods};
		^counter;
	}
	// diese funktion geht so lange durch die SPList listen struktur bis
	// ein listenelement eine Period ist
	// dann wird die performOnPeriod methode der Period aufgerufen (siehe unten)
	performOnPeriod{arg method ... args;
		this.do({|i|
			i.performOnPeriod(method,*args);
		})
	}
	clear{
		this.array=[];
	}

	// clean clean2 wird garnicht mehr benutzt!!!!!
	// um leere SPList oder Period Objekte aus der SPList-Struktur
	// zu entfernen
	// clean{
	// 	this.array.do{|i|i.clean};
	// 	this.array.removeAllSuchThat{|i|i.sum(\size)==0};
	// }
	// clean2{ //als test in der do funktion
	// 	if(array.notNil,{
	// 		this.array.removeAllSuchThat{|i|i.sum(\size)==0};
	// 	});
	// }

	species { ^SPList }
	reverse{
		array=array.reverse;
	}

	//transformationen
	interleave{arg anSPList,cutend=true;
		var newarray,sa,sb,longest;
		sa=this.size;
		sb=anSPList.size;
		if(cutend===true,{
			if(sa>sb,{sa=sb},{sb=sa});
		},{if(sa>sb,{sb=sa},{sa=sb})});
		newarray=Array.new(sa+sb);
		sa.do{|i|
			newarray.add(this.wrapAt(i));
			newarray.add(anSPList.wrapAt(i));
		};
		this.array=newarray;
	}
	loop{arg n=1;
		n.do{this.parent.addlist.add([this,this.deepCopy]);};
	}
	internalloop{arg n=1;
		array=SPList.newUsing(array,this)!n;
	}

	internalloopcopy{arg n=1;
		switch(n,
			0,{array=[SPList.newUsing(array,this)]},
			{
				array=[SPList.newUsing(array,this)]++(1..n).collect{SPList.newUsing(array,this).deepCopy};
			}
		)
	}
	appendIntoParent{arg c;
		this.parent.addlist.add([this,c]);
	}
	delete{
		this.array=[];
		this.parent.dellist.add(this);
	}
	reset{
		this.flat;
		this.sortBy(\pos);
		this.array.do(_.reset);
		addlist=LinkedList.new;
		dellist=LinkedList.new;
	}
	addenv{arg envelope,sampleRate;
		var aes;
		if(this.parent.notNil,{this.parent.addenv(envelope,sampleRate)},{
			if(allenvs==nil,{allenvs=List.new});
			if(not(allenvs.includes(envelope)),{
				allenvs.add(envelope);
				aes=allenvs.size;
				envelope.sampleRate=sampleRate;
				envelope.filename=Platform.defaultTempDir +/+ "env"++aes++".aif";
				envelope.bufnum=aes+2000;
			})
		})
	}
	env{arg envelope,max=0,from=0;
		var to=0;
		var dsize=this.sum(\size);
		if(max<dsize,{max=dsize});
		if(envelope.size<dsize,
			{envelope.size=max=dsize;this.addenv(envelope,96000)}//,
			//{max=envelope.size}
		);
		array.do{|item|
			var isize=item.sum(\size);
			to=from+isize;
			item.env(envelope,max,from);
			from=to;
		}
	}

	doesNotUnderstand { arg selector ... args;
		if(Period.new.respondsTo(selector),{
			array.do{|i|
				i.perform(selector,*args);
			}
		},{
			^super.doesNotUnderstand(selector,*args);
		}
		)
	}

	nrts_buf{arg af=nil;
		array.do(_.nrts_buf(af));
	}
	node{arg arr,startpos,startposfunction,startposfunctiondeep,deep;
		var sec=0;
		var orgsp=startpos;
		var size=0;
		array.do{|item,i|
			sec=item.node(arr,startpos,startposfunctiondeep,startposfunctiondeep,deep+1);
			size=size+sec;
			startpos=startposfunction.(startpos,sec,i,item,deep);
		}
		^(startpos-orgsp);
	}
	nodecount{
		var x=0;
		array.do{|i|
			x=x+i.nodecount;
		}
		^x;
	}
	load_envs{arg file;
		allenvs.do{|item,i|
			var oscm;
			item.write;
			oscm=[0.0,[\b_allocRead,i+2000,item.filename]].asRawOSC;
			file.write(oscm.size);
			file.write(oscm);
		}
	}
	load_bufs{arg file;
		var allfiles=LinkedList.new;
		this.nrts_buf(allfiles);
		allfiles.do{|item,i|
			var oscm;
			oscm=[0.0,[\b_allocRead,i+1000,item.soundfilename]].asRawOSC;
			file.write(oscm.size);
			file.write(oscm);
		};
	}
	load_periods{arg file,spf,spfd;
		var arr=Array.new(this.nodecount);
		this.node(arr,0,spf,spfd,0);
		arr=arr.quickSort{|a,b| a.startpos<b.startpos};
		arr.do{|i| i.write(file)};
	}
	write{arg nrtServerOptions,spf={|startpos,dur,i,item|startpos+dur;},spfd={|startpos,dur,i,item,deep| startpos+dur;};
		var file=File(nrtServerOptions.oscFilename,"w");
		this.load_envs(file);
		this.load_bufs(file);
		this.load_periods(file,spf,spfd);
		file.close;
		nrtServerOptions.write;
		nrtServerOptions.deleteOSCFile;
	}
	howDeep{arg deep=0;
		^array[0].howDeep(deep+1);
	}
	cue{arg spf={|startpos,sec,i,item|startpos+sec;},spfd={|startpos,sec,i,item,deep| startpos+sec;};
		var nrtso=NRTServerOptions.tempfileOptionsString;
		var sf=SoundFile.new;
		this.write(nrtso,spf,spfd);
		sf.openRead(nrtso.outputFilename);
		^sf;
	}
	play{arg spf={|startpos,sec,i,item|startpos+sec;},spfd={|startpos,sec,i,item,deep| startpos+sec;};
		var sf;
		r{sf=this.cue(spf,spfd);1.wait;sf.play;}.play;
		sf;
	}
	plot{arg bounds;
		"NOT IMPLEMENTED ANYMORE BY SOUNDFILE CLASS".error;
		// var sf=SoundFile.new,nrtsf=NRTServerOptions.tempfileOptionsString;
		// this.write(nrtsf);
		// sf.openRead(nrtsf.outputFilename);
		// sf.plot(bounds);
	}
}
