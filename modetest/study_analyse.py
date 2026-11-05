import numpy as np,struct,os
rate=48000
def wav(p):
    b=open(p,'rb').read(); i=12
    while True:
        cid=b[i:i+4]; sz=struct.unpack('<I',b[i+4:i+8])[0]
        if cid==b'fmt ': ch=struct.unpack('<H',b[i+10:i+12])[0]; bits=struct.unpack('<H',b[i+22:i+24])[0]
        if cid==b'data': d=b[i+8:]; break
        i+=8+sz
    dt='<i2' if bits==16 else '<i4'; n=len(d)//(ch*bits//8)*(ch*bits//8)
    return np.frombuffer(d[:n],dt).reshape(-1,ch).astype(float)/(2**(bits-1))
F=os.listdir('.')
hal=wav([f for f in F if f.startswith('hal_streamout')][0])
mic=wav('mic.wav')
raw=np.frombuffer(open([f for f in F if 'DspRaw' in f][0],'rb').read(),'<i4').reshape(-1,2).astype(float)/2**31*256
plan=[l.split(',') for l in open('plan.txt').read().split('\n') if l]
plan=[(int(a),int(b),c,float(d),float(e)) for a,b,c,d,e in plan]
blk=480
def envb(x): n=len(x)//blk; return np.sqrt(np.mean(x[:n*blk].reshape(n,blk)**2,axis=1))+1e-9
he=np.log(envb(hal[:,0]+hal[:,1]))
def lag(x):
    e=np.log(envb(x)); n=min(len(e),len(he))
    a=(he[:n]-he[:n].mean()); best=None
    for L in range(-200,3000):
        if L>=0: b=e[L:L+n]
        else: continue
        m=min(len(a),len(b)); c=np.corrcoef(a[:m],b[:m])[0,1]
        if best is None or c>best[0]: best=(c,L)
    return best
lm=lag(mic[:,0]+mic[:,1]); lr=lag(raw[:,0]+raw[:,1])
print('lag mic',lm,'lag raw',lr)
# hal marker onset
def onset(x):
    for b in range(len(x)//blk):
        if np.abs(x[b*blk:(b+1)*blk]).max()>0.05: return b*blk
o=onset(hal[:,0]); hoff=o-plan[0][0]; print('hal offset',hoff)
moff=hoff+lm[1]*blk; roff=hoff+lr[1]*blk
win=rate*15//100
def tone(x,fr,n,f):
    i=np.arange(n); w=0.5-0.5*np.cos(2*np.pi*i/(n-1)); seg=x[fr:fr+n]
    if len(seg)<n: return 0
    return 2*abs(np.sum(seg*w*np.exp(-2j*np.pi*f*i/rate)))/w.sum()
db=lambda a:-240 if a<=1e-12 else 20*np.log10(a)
rows=[]
for (st,ln,name,f,lv) in plan[1:]:
    ha=st+hoff+rate//10; hl=db(tone(hal[:,0],ha,win,f)); hr=db(tone(hal[:,1],ha,win,f))
    r={'name':name,'f':f,'lv':lv,'hl':hl,'hr':hr}
    for tag,x,off in [('raw',raw,roff),('app',mic,moff)]:
        a=st+off+rate//10
        for c in (0,1):
            fu=tone(x[:,c],a,win,f); hs=[tone(x[:,c],a,win,f*k) if f*k<rate/2-1000 else 0 for k in range(2,6)]
            r[f'{tag}{c}']=db(fu); r[f'{tag}{c}thd']=100*np.sqrt(sum(h*h for h in hs))/max(fu,1e-12)
            r[f'{tag}{c}h']=[db(h) for h in hs]
            r[f'{tag}{c}pk']=db(np.abs(x[a:a+win,c]).max())
    rows.append(r)
import pickle; pickle.dump(rows,open('/tmp/claude-0/-home-user/46601717-5ba7-5be7-8226-20cbfc8150ad/scratchpad/study/rows.pkl','wb'))
for name in ('earpiece','speaker'):
    print('==',name,' (hal L/R | raw0 fund thd | raw1 fund thd | pk)')
    for r in rows:
        if r['name']!=name: continue
        print(f"{r['f']:5.0f} {r['lv']:5.0f} | {r['hl']:6.1f} {r['hr']:6.1f} | {r['raw0']:6.1f} {r['raw0thd']:6.2f}% | {r['raw1']:6.1f} {r['raw1thd']:6.2f}% | {r['raw0pk']:5.1f} {r['raw1pk']:5.1f}")

print('\n== THD of what reached the HAL (after Dirac), per channel being played')
for name,ch in (('earpiece',0),('speaker',1)):
    print('--',name)
    for (st,ln,nm,f,lv) in plan[1:]:
        if nm!=name or lv not in (-18,-9,-6,-3): continue
        ha=st+hoff+rate//10; x=hal[:,ch]
        fu=tone(x,ha,win,f); hs=[tone(x,ha,win,f*k) if f*k<rate/2-1000 else 0 for k in range(2,6)]
        print(f"{f:5.0f} {lv:4.0f}  hal {db(fu):6.1f}  thd {100*np.sqrt(sum(h*h for h in hs))/fu:6.2f}%  harmonics {' '.join(f'{db(h):6.1f}' for h in hs)}  peak {db(np.abs(x[ha:ha+win]).max()):5.1f}")
