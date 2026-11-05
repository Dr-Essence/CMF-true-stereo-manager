import numpy as np
rate=48000
def plan():
    freqs=[100,150,200,300,500,800,1200,2000,3000,5000,8000]; levels=[-30,-24,-18,-12,-9,-6,-3]
    on=rate*3//10; gap=rate*3//10; out=[]; t=rate//2
    out.append((t,rate//2,0,1000.0,-12.0,'marker')); t+=rate
    for ch,name in [(0,'earpiece'),(1,'speaker')]:
        for f in freqs:
            for db in levels: out.append((t,on,ch,f,db,name)); t+=on+gap
        t+=rate
    return out
segs=plan(); total=segs[-1][0]+segs[-1][1]+rate
fade=rate//100
play=np.zeros((total,2))
for (st,ln,ch,f,db,_) in segs:
    k=np.arange(ln); a=10**(db/20)
    w=np.ones(ln); w[:fade]=0.5-0.5*np.cos(np.pi*k[:fade]/fade); w[ln-fade:]=0.5-0.5*np.cos(np.pi*(ln-k[ln-fade:])/fade)
    play[st:st+ln,ch]=a*w*np.sin(2*np.pi*f*k/rate)
# simulated driver: earpiece clips softly above -9 dBFS at low freq (tanh), speaker clean; mic gains, delay, noise
def driver(x,drive): return np.tanh(x*drive)/drive
ear=driver(play[:,0],3.0); spk=play[:,1]
delay=rate+1234  # 1 s lead + latency
mic=np.zeros((total+2*rate,2))
mic[delay:delay+total,0]=0.3*ear+0.8*spk
mic[delay:delay+total,1]=0.5*ear+0.2*spk
mic+=np.random.default_rng(1).normal(0,10**(-70/20),mic.shape)
mic=np.round(mic*32768).clip(-32768,32767)/32768
hal=np.vstack([np.zeros((777,2)),play])  # HAL dump starts a bit early
def tone(x,fr,n,f):
    i=np.arange(n); w=0.5-0.5*np.cos(2*np.pi*i/(n-1)); seg=np.zeros(n)
    lo=max(fr,0); hi=min(fr+n,len(x))
    if hi>lo: seg[lo-fr:hi-fr]=x[lo:hi]
    v=seg*w; return 2*abs(np.sum(v*np.exp(-2j*np.pi*f*i/rate)))/w.sum()
def onset(x):
    blk=rate//100; fl=1e-9
    for b in range(min(30,len(x)//blk)): fl=max(fl,tone(x,b*blk,blk,1000))
    for b in range(len(x)//blk-1):
        if tone(x,b*blk,blk,1000)>fl*10: return b*blk
    return -1
db=lambda a:-240 if a<=1e-12 else 20*np.log10(a)
marker=segs[0][0]
o=[onset(mic[:,c]) for c in (0,1)]; micOff=min(v for v in o if v>=0)-marker
halOff=onset(hal[:,0])-marker
print('mic offset',micOff,'true',delay,'hal offset',halOff,'true',777)
win=rate*15//100
for s in segs[1:]:
    st,ln,ch,f,dbl,name=s
    if not (f in (100,1000,2000) and dbl in (-30,-9,-3)): continue
    a=st+halOff+rate//10; hl=db(tone(hal[:,0],a,win,f)); hr=db(tone(hal[:,1],a,win,f))
    for mi in (0,1):
        a=st+micOff+rate//10; fu=tone(mic[:,mi],a,win,f)
        hs=[tone(mic[:,mi],a,win,f*k) if f*k<rate/2-1000 else 0 for k in range(2,6)]
        thd=100*np.sqrt(sum(h*h for h in hs))/fu
        print(f"{name:8s} {f:5} {dbl:4} hal {hl:6.1f} {hr:6.1f} mic{mi} fund {db(fu):6.1f} thd {thd:6.2f}%")
