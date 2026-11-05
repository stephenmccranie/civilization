'use strict';
const $=id=>document.getElementById(id);
const canvas=$('map'),ctx=canvas.getContext('2d',{alpha:false});
const markerCanvas=$('playerCanvas'),markerCtx=markerCanvas.getContext('2d');
const formatter=new Intl.NumberFormat(),number=n=>formatter.format(n);
const MAX_LEVEL=8,MAX_CELLS=196,MAX_IMAGES=512,MAX_LOADS=6,MAX_HEIGHTS=32;
const MAX_IMAGE_BYTES=64*1024*1024,OVERVIEW_PIXELS=256;
let data=null,tiles=new Map(),levels=Array.from({length:MAX_LEVEL+1},()=>new Map());
let center=[0,0],zoom=.16,W=0,H=0,ratio=1,first=true,players=[],followPlayer=null;
let terrainDirty=false,markersDirty=false,hoverDirty=false,frame=0;
let activeLoads=0,queue=[],wanted=new Set(),heightTimer=0,heightRequest=null,heightKey=null;
let hoverPoint=null,viewRect=null,playerSignature='',playerBusy=false,coverageBusy=false;
const playerButtons=new Map(),imageCache=new Map(),heightCache=new Map();
const renderStats={terrainFrames:0,markerFrames:0,lastTileCount:0,lastLevel:0};
let deposits=[],depositHits=[],depositSignature='',depositBusy=false;
let villages=[],villageHits=[],villageSignature='',villageBusy=false;
let metricsBusy=false;
const villageStyles={village:{name:'Village',color:'#9bd4f5'}};
const depositStyles={coal:{name:'Coal seam',color:'#e1e4e8'},oil:{name:'Oil reservoir',color:'#e5b76a'}};

function text(id,value){if($(id).textContent!==value)$(id).textContent=value}
function screen(x,z){return [(x-center[0])*zoom+W/2,(z-center[1])*zoom+H/2]}
function world(x,y){return [(x-W/2)/zoom+center[0],(y-H/2)/zoom+center[1]]}
function schedule(){if(!frame&&!document.hidden)frame=requestAnimationFrame(paint)}
function draw(){terrainDirty=markersDirty=true;schedule()}
function terrainChanged(){terrainDirty=true;schedule()}
function markersChanged(){markersDirty=true;schedule()}
function paint(){
    frame=0;if(!W||!H)return;
    if(terrainDirty){terrainDirty=false;paintTerrain()}
    if(markersDirty){markersDirty=false;drawPlayers()}
    if(hoverDirty){hoverDirty=false;paintHover()}
}
function resize(){
    const r=canvas.getBoundingClientRect();viewRect=r;
    const nextRatio=Math.min(2,devicePixelRatio||1);
    if(W===r.width&&H===r.height&&ratio===nextRatio)return;
    W=r.width;H=r.height;ratio=nextRatio;
    for(const c of [canvas,markerCanvas]){c.width=Math.round(W*ratio);c.height=Math.round(H*ratio)}
    draw();
}
function chooseLevel(){
    let level=0;
    while(level<MAX_LEVEL){
        const span=512*2**level*zoom;
        const layers=$('contours').checked?2:1;
        const maxCells=level?Math.min(MAX_CELLS,Math.floor(MAX_IMAGE_BYTES/(OVERVIEW_PIXELS**2*4*layers))):MAX_CELLS;
        if(span*ratio>=96&&(Math.ceil(W/span)+2)*(Math.ceil(H/span)+2)<=maxCells)break;
        level++;
    }
    while(level>0&&!levels[level].size)level--;
    return level;
}
function visibleTiles(level){
    const span=512*2**level,lo=world(0,0),hi=world(W,H),result=[];
    const minX=Math.floor(lo[0]/span),maxX=Math.floor(hi[0]/span);
    const minZ=Math.floor(lo[1]/span),maxZ=Math.floor(hi[1]/span);
    if((maxX-minX+1)*(maxZ-minZ+1)>levels[level].size){
        for(const t of levels[level].values())if(t.x>=minX&&t.x<=maxX&&t.z>=minZ&&t.z<=maxZ)result.push(t);
        return result;
    }
    for(let z=minZ;z<=maxZ;z++)for(let x=minX;x<=maxX;x++){
        const t=levels[level].get(x+','+z);if(t)result.push(t);
    }
    return result;
}
function keyFor(t,layer){return t.level+':'+t.x+','+t.z+':'+layer}
function imageUrl(t,layer){
    const root=t.level?'/overviews/'+t.level:'/tiles';
    return root+'/r.'+t.x+'.'+t.z+(layer==='plain'?'':'.'+layer)+'.png?v='+t.version;
}
function dispose(image){if(image?.close)image.close()}
function imageBytes(image){return image?(image.width||128)*(image.height||128)*4:0}
function trimImages(){
    let bytes=0;for(const entry of imageCache.values())bytes+=imageBytes(entry.image)+imageBytes(entry.fallback);
    for(const [key,entry] of imageCache){
        if(imageCache.size<=MAX_IMAGES&&bytes<=MAX_IMAGE_BYTES)break;
        if(wanted.has(key))continue;
        bytes-=imageBytes(entry.image)+imageBytes(entry.fallback);
        imageCache.delete(key);entry.controller?.abort();dispose(entry.image);dispose(entry.fallback);
    }
}
function requestImage(t,layer){
    const key=keyFor(t,layer);wanted.add(key);
    let entry=imageCache.get(key);
    if(!entry||entry.version!==t.version){
        const fallback=entry?.image||entry?.fallback||null;
        entry?.controller?.abort();
        entry={key,t,layer,version:t.version,image:null,fallback,state:'idle',retryAt:0};
    }
    imageCache.delete(key);imageCache.set(key,entry);
    if(entry.state==='idle'&&performance.now()>=entry.retryAt){entry.state='queued';queue.push(entry)}
    return entry.image||entry.fallback;
}
async function decodeImage(blob){
    if(typeof createImageBitmap==='function')return createImageBitmap(blob);
    const url=URL.createObjectURL(blob),image=new Image();
    try{image.src=url;await image.decode();return image}finally{URL.revokeObjectURL(url)}
}
async function loadImage(entry){
    entry.state='loading';entry.controller=new AbortController();activeLoads++;
    try{
        let response=await fetch(imageUrl(entry.t,entry.layer),{signal:entry.controller.signal});
        if(!response.ok&&entry.layer==='relief')response=await fetch(imageUrl(entry.t,'plain'),{signal:entry.controller.signal});
        if(!response.ok)throw Error('Tile not ready');
        const image=await decodeImage(await response.blob());
        if(imageCache.get(entry.key)!==entry||entry.controller.signal.aborted){dispose(image);return}
        dispose(entry.fallback);entry.fallback=null;entry.image=image;entry.state='ready';
        if(wanted.has(entry.key))terrainChanged();
    }catch(e){
        if(imageCache.get(entry.key)===entry){entry.state='idle';entry.retryAt=performance.now()+5000;if(wanted.has(entry.key))setTimeout(terrainChanged,5000)}
    }finally{activeLoads--;entry.controller=null;pumpLoads()}
}
function pumpLoads(){
    while(activeLoads<MAX_LOADS&&queue.length){
        const entry=queue.shift();
        if(imageCache.get(entry.key)!==entry||!wanted.has(entry.key)){
            if(entry.state==='queued')entry.state='idle';continue;
        }
        loadImage(entry);
    }
}
function cachedAncestor(t,layer,x,y,size){
    for(let level=t.level+1;level<=MAX_LEVEL;level++){
        const factor=2**(level-t.level),px=Math.floor(t.x/factor),pz=Math.floor(t.z/factor);
        const entry=imageCache.get(level+':'+px+','+pz+':'+layer),image=entry?.image||entry?.fallback;
        if(!image)continue;
        const sw=image.width/factor,sx=(t.x-px*factor)*sw,sz=(t.z-pz*factor)*sw;
        ctx.imageSmoothingEnabled=sw>size*ratio;
        ctx.drawImage(image,sx,sz,sw,sw,x,y,size,size);return true;
    }
    return false;
}
function paintTerrain(){
    renderStats.terrainFrames++;renderStats.lastTileCount=0;
    ctx.setTransform(ratio,0,0,ratio,0,0);ctx.fillStyle='#18252c';ctx.fillRect(0,0,W,H);
    ctx.imageSmoothingEnabled=false;ctx.imageSmoothingQuality='high';
    const level=chooseLevel(),visible=visibleTiles(level),mode=$('view').querySelector('input:checked').value,contours=$('contours').checked;
    renderStats.lastLevel=level;wanted=new Set();
    visible.sort((a,b)=>Math.hypot((a.x+.5)*512*2**level-center[0],(a.z+.5)*512*2**level-center[1])-
        Math.hypot((b.x+.5)*512*2**level-center[0],(b.z+.5)*512*2**level-center[1]));
    for(const t of visible){
        const span=512*2**level,[left,top]=screen(t.x*span,t.z*span),[right,bottom]=screen((t.x+1)*span,(t.z+1)*span);
        const x=Math.round(left*ratio)/ratio,y=Math.round(top*ratio)/ratio;
        const width=Math.round(right*ratio)/ratio-x,height=Math.round(bottom*ratio)/ratio-y;
        const image=requestImage(t,mode);
        if(image){ctx.imageSmoothingEnabled=image.width>width*ratio;ctx.drawImage(image,x,y,width,height);renderStats.lastTileCount++}
        else if(cachedAncestor(t,mode,x,y,width))renderStats.lastTileCount++;
        if(contours){const overlay=requestImage(t,'contours');if(overlay){ctx.imageSmoothingEnabled=overlay.width>width*ratio;ctx.drawImage(overlay,x,y,width,height)}}
    }
    trimImages();pumpLoads();
    ctx.lineWidth=1;ctx.font='10px ui-monospace,monospace';
    if($('grid').checked)drawGrid();
    drawScale();
}
function drawGrid(){
    let step=16;while(step*zoom<100)step*=2;
    const lo=world(0,0),hi=world(W,H);ctx.strokeStyle='#bedce112';ctx.fillStyle='#b5c5ce';
    for(let x=Math.ceil(lo[0]/step)*step;x<hi[0];x+=step){const [a]=screen(x,0);ctx.beginPath();ctx.moveTo(a,0);ctx.lineTo(a,H);ctx.stroke();ctx.fillText(number(x),a+4,14)}
    for(let z=Math.ceil(lo[1]/step)*step;z<hi[1];z+=step){const [,b]=screen(0,z);ctx.beginPath();ctx.moveTo(0,b);ctx.lineTo(W,b);ctx.stroke();ctx.fillText(number(z),4,b-4)}
}
function drawScale(){
    const [ox,oz]=screen(0,0);ctx.strokeStyle='#f1f4f6';ctx.beginPath();ctx.moveTo(ox-6,oz);ctx.lineTo(ox+6,oz);ctx.moveTo(ox,oz-6);ctx.lineTo(ox,oz+6);ctx.stroke();
    let scale=16;while(scale*zoom<70)scale*=2;
    ctx.fillStyle='#d2e0e6';ctx.fillRect(W-24-scale*zoom,H-30,scale*zoom,2);ctx.fillText(number(scale)+' blocks',W-24-scale*zoom,H-38);
}
function drawPlayers(){
    renderStats.markerFrames++;
    const c=markerCtx;c.setTransform(ratio,0,0,ratio,0,0);c.clearRect(0,0,W,H);
    drawDeposits(c);
    for(const p of players){
        if(p.dimension!=='minecraft:overworld')continue;
        const [x,y]=screen(p.x,p.z);if(x<-30||y<-30||x>W+30||y>H+30)continue;
        c.save();c.translate(x,y);c.rotate(p.yaw*Math.PI/180+Math.PI);c.beginPath();c.moveTo(0,-11);c.lineTo(7,8);c.lineTo(0,4);c.lineTo(-7,8);c.closePath();c.fillStyle='#ffc76a';c.strokeStyle='#10171b';c.lineWidth=2;c.fill();c.stroke();c.restore();
        c.save();c.font='bold 12px system-ui';c.textAlign='center';const width=c.measureText(p.name).width;c.fillStyle='#10171bee';c.fillRect(x-width/2-5,y-33,width+10,18);c.fillStyle='#fff1cf';c.fillText(p.name,x,y-20);c.restore();
    }
}
function drawDeposits(c){
    depositHits=[];villageHits=[];
    if($('showDeposits').checked)drawMarkerSet(c,deposits,depositHits,depositStyles);
    if($('showVillages').checked)drawMarkerSet(c,villages,villageHits,villageStyles);
}
function drawMarkerSet(c,markers,hits,styles){
    // Group on a world-anchored grid whose cell doubles per zoom step (32-64 screen
    // pixels), so panning never regroups markers; only crossing a zoom step does.
    const cell=2**Math.ceil(Math.log2(32/zoom)),bins=new Map();
    for(const d of markers){
        if(!styles[d.kind])continue;
        const key=d.kind+':'+Math.floor(d.x/cell)+':'+Math.floor(d.z/cell),bin=bins.get(key);
        if(bin)bin.count++;else bins.set(key,{d,count:1});
    }
    c.save();c.lineWidth=3;c.strokeStyle='#10171b';
    for(const {d,count} of bins.values()){
        const [x,y]=screen(d.x,d.z);if(x<-80||y<-80||x>W+80||y>H+80)continue;
        const hit={x,y,d,count};c.beginPath();
        if(d.kind==='coal')c.arc(x,y,6,0,Math.PI*2);
        else if(d.kind==='oil'){c.moveTo(x,y-8);c.lineTo(x+7,y);c.lineTo(x,y+8);c.lineTo(x-7,y);c.closePath()}
        else{c.moveTo(x,y-9);c.lineTo(x+8,y-2);c.lineTo(x+6,y-2);c.lineTo(x+6,y+7);c.lineTo(x-6,y+7);c.lineTo(x-6,y-2);c.lineTo(x-8,y-2);c.closePath()}
        c.fillStyle=styles[d.kind].color;c.fill();c.stroke();
        if(count>1){c.font='bold 10px system-ui';c.textAlign='left';c.strokeText(String(count),x+9,y+4);c.fillStyle='#fff';c.fillText(String(count),x+9,y+4)}
        hits.push(hit);
    }
    c.restore();
}
async function updateDeposits(){
    if(document.hidden||depositBusy)return;depositBusy=true;
    try{
        const r=await fetch('/api/deposits',{cache:'no-store'});if(!r.ok)throw Error('Deposit feed unavailable');
        const v=await r.json(),next=(v.markers||[]).filter(d=>depositStyles[d.kind]&&[d.x,d.z,d.y,d.blocks].every(Number.isFinite));
        const signature=JSON.stringify(next);deposits=next;
        if(signature!==depositSignature){depositSignature=signature;markersChanged();hoverDirty=true}
        for(const kind of Object.keys(depositStyles))text(kind+'Count',number(v.counts?.[kind]||0));
        text('depositStatus',!next.length&&data?.fullChunks>0?'No deposits found in saved chunks':'');
    }catch(e){text('depositStatus','Deposit index unavailable; retrying.')}
    finally{depositBusy=false}
}
async function updateVillages(){
    if(document.hidden||villageBusy)return;villageBusy=true;
    try{
        const r=await fetch('/api/villages',{cache:'no-store'});if(!r.ok)throw Error('Village feed unavailable');
        const v=await r.json(),next=(v.markers||[]).filter(d=>d.kind==='village'&&[d.x,d.y,d.z].every(Number.isFinite));
        const signature=JSON.stringify(next);villages=next;
        if(signature!==villageSignature){villageSignature=signature;markersChanged();hoverDirty=true}
        text('villageCount',number(next.length));
        text('villageStatus',!next.length&&data?.fullChunks>0?'No villages found in saved chunks':'');
    }catch(e){text('villageStatus','Village index unavailable; retrying.')}
    finally{villageBusy=false}
}
function clearMetrics(){
    for(const id of ['cpuUsage','ramUsage','networkRx','networkTx'])text(id,'—');
}
async function updateMetrics(){
    if(document.hidden||metricsBusy)return;metricsBusy=true;
    const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),4000);
    try{
        const r=await fetch('/api/metrics',{cache:'no-store',signal:controller.signal});if(!r.ok)throw Error('Metrics unavailable');
        const v=await r.json();
        if(!Number.isFinite(v.updated)||Date.now()/1000-v.updated>15||v.updated>Date.now()/1000+5)throw Error('Stale metrics');
        const valid=n=>Number.isFinite(n)&&n>=0;
        text('cpuUsage',valid(v.cpuPercent)?v.cpuPercent.toFixed(1)+'%':'—');
        text('ramUsage',valid(v.ramUsedBytes)&&valid(v.ramTotalBytes)?(v.ramUsedBytes/1e9).toFixed(2)+' / '+(v.ramTotalBytes/1e9).toFixed(2)+' GB':'—');
        text('networkRx',valid(v.rxBytesPerSecond)?(v.rxBytesPerSecond/1e6).toFixed(2)+' MB/s':'—');
        text('networkTx',valid(v.txBytesPerSecond)?(v.txBytesPerSecond/1e6).toFixed(2)+' MB/s':'—');
    }catch(e){clearMetrics()}
    finally{clearTimeout(timeout);metricsBusy=false}
}
function playerList(stale=false){
    text('playerStatus',stale?'Positions unavailable':players.length+' online');
    const live=new Set(players.map(p=>p.uuid));
    for(const [id,button] of playerButtons)if(!live.has(id)){button.remove();playerButtons.delete(id)}
    for(const p of players){
        let button=playerButtons.get(p.uuid);
        if(!button){button=document.createElement('button');button.style.cssText='width:100%;text-align:left;font-size:14px;margin-top:6px;white-space:normal';button.onclick=()=>{
            const current=players.find(q=>q.uuid===p.uuid);if(!current||current.dimension!=='minecraft:overworld')return;
            followPlayer=current.uuid;center=[current.x,current.z];zoom=Math.max(zoom,.35);playerList();draw();
        };playerButtons.set(p.uuid,button);$('playerList').appendChild(button)}
        button.disabled=p.dimension!=='minecraft:overworld';button.style.borderColor=followPlayer===p.uuid?'#ffc76a':'#3c535d';
        button.title='X '+number(Math.floor(p.x))+' · Y '+number(Math.floor(p.y))+' · Z '+number(Math.floor(p.z));
        const label=p.name+(p.dimension==='minecraft:overworld'?'':' · '+p.dimension.replace('minecraft:',''));
        if(button.textContent!==label)button.textContent=label;
    }
}
async function updatePlayers(){
    if(document.hidden||playerBusy)return;playerBusy=true;
    try{
        const r=await fetch('/api/players',{cache:'no-store'});if(!r.ok)throw Error('Player feed unavailable');
        const v=await r.json(),next=!v.stale&&Array.isArray(v.players)?v.players.filter(p=>typeof p.uuid==='string'&&typeof p.name==='string'&&[p.x,p.y,p.z,p.yaw].every(Number.isFinite)):[];
        const signature=JSON.stringify(next),changed=signature!==playerSignature;
        playerSignature=signature;players=next;
        const followed=players.find(p=>p.uuid===followPlayer&&p.dimension==='minecraft:overworld');
        if(followed&&(center[0]!==followed.x||center[1]!==followed.z)){center=[followed.x,followed.z];draw()}
        else if(changed)markersChanged();
        if(!followed)followPlayer=null;playerList(!!v.stale);
    }catch(e){if(players.length){players=[];playerSignature='';followPlayer=null;markersChanged()}playerList(true)}
    finally{playerBusy=false}
}
function hover(x,y){hoverPoint=[x,y];hoverDirty=true;schedule()}
function paintHover(){
    if(!hoverPoint)return;
    const w=world(...hoverPoint),bx=Math.floor(w[0]),bz=Math.floor(w[1]),rx=Math.floor(bx/512),rz=Math.floor(bz/512),key=rx+','+rz,t=tiles.get(key);
    let height='';const cached=heightCache.get(key);
    if(cached&&cached.version===t?.version){
        heightCache.delete(key);heightCache.set(key,cached);
        const i=Math.floor((bz-rz*512)/4)*128+Math.floor((bx-rx*512)/4),h=cached.values.getInt16(i*2,true);
        if(h!==-32768)height=' · Y ≈ '+number(h);
    }
    let detail='';let nearest=15;
    for(const hit of depositHits){const distance=Math.hypot(hit.x-hoverPoint[0],hit.y-hoverPoint[1]);if(distance>=nearest)continue;nearest=distance;
        const d=hit.d;detail=hit.count>1?' · '+hit.count+' '+depositStyles[d.kind].name.toLowerCase()+' markers · zoom in':
            ' · '+depositStyles[d.kind].name+' at X '+number(Math.round(d.x))+' Z '+number(Math.round(d.z))+' · '+number(d.blocks)+' mapped resource blocks'+(d.minY===null?' · seep only':' · Y '+d.minY+'–'+d.maxY);
    }
    for(const hit of villageHits){const distance=Math.hypot(hit.x-hoverPoint[0],hit.y-hoverPoint[1]);if(distance>=nearest)continue;nearest=distance;
        detail=hit.count>1?' · '+hit.count+' villages · zoom in':' · Village at X '+number(Math.round(hit.d.x))+' · Y '+number(hit.d.y)+' · Z '+number(Math.round(hit.d.z));
    }
    text('coords','X '+number(bx)+' · Z '+number(bz)+height+detail);
    if(!pointers.size&&t&&(!cached||cached.version!==t.version)&&heightKey!==key+':'+t.version){
        clearTimeout(heightTimer);heightRequest?.abort();heightKey=key+':'+t.version;
        heightTimer=setTimeout(()=>loadHeight(t,key),120);
    }
}
async function loadHeight(t,key){
    const controller=new AbortController();heightRequest=controller;
    try{
        const r=await fetch('/tiles/r.'+t.x+'.'+t.z+'.height.bin?v='+t.version,{signal:controller.signal});
        if(!r.ok)throw Error('Height cache pending');const b=await r.arrayBuffer();if(b.byteLength!==32768)throw Error('Height tile size');
        heightCache.delete(key);heightCache.set(key,{version:t.version,values:new DataView(b)});
        while(heightCache.size>MAX_HEIGHTS)heightCache.delete(heightCache.keys().next().value);
        hoverDirty=true;schedule();
    }catch(e){if(heightKey===key+':'+t.version)heightKey=null}
    finally{if(heightRequest===controller)heightRequest=null}
}
function fit(){
    followPlayer=null;if(!tiles.size)return;
    let minx=Infinity,minz=Infinity,maxx=-Infinity,maxz=-Infinity;
    for(const t of tiles.values()){minx=Math.min(minx,t.x*512);maxx=Math.max(maxx,(t.x+1)*512);minz=Math.min(minz,t.z*512);maxz=Math.max(maxz,(t.z+1)*512)}
    center=[(minx+maxx)/2,(minz+maxz)/2];zoom=Math.min(W/(maxx-minx+512),H/(maxz-minz+512));draw();
}
async function update(){
    if(document.hidden||coverageBusy)return;coverageBusy=true;
    try{
        const [res,overviewResponse]=await Promise.all([fetch('/api/coverage',{cache:'no-store'}),fetch('/api/overviews',{cache:'no-store'})]);
        if(!res.ok)throw Error('HTTP '+res.status);const next=await res.json();if(!next.regions)throw Error('Waiting for surface scan');
        const overview=overviewResponse.ok?await overviewResponse.json():{tiles:[]};
        let changed=false;const keep=new Set();
        for(const [x,z,count,version] of next.regions){
            const key=x+','+z;keep.add(key);const old=tiles.get(key);
            if(old?.version===version)continue;
            tiles.set(key,{level:0,x,z,version});changed=true;
        }
        for(const key of tiles.keys())if(!keep.has(key)){tiles.delete(key);changed=true}
        levels[0]=tiles;
        for(let level=1;level<=MAX_LEVEL;level++){
            const rows=(overview.tiles||[]).filter(t=>t[0]===level),previous=levels[level],current=new Map();
            for(const [,x,z,version] of rows){const key=x+','+z,old=previous.get(key);current.set(key,{level,x,z,version});if(old?.version!==version)changed=true}
            if(current.size!==previous.size)changed=true;levels[level]=current;
        }
        data=next;text('count',number(data.fullChunks));
        text('worldSize',Number.isFinite(data.worldBytes)?(data.worldBytes/1e9).toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2})+' GB':'—');
        text('lodSize',Number.isFinite(data.lodBytes)?(data.lodBytes/1e9).toLocaleString(undefined,{minimumFractionDigits:2,maximumFractionDigits:2})+' GB':'—');
        const j=data.job||{};
        text('job',j.status==='running'?'Generating · '+(j.phase==='lods'?'LODs':'chunks'):j.status==='paused'?'Generation paused':j.status==='archived'?'Archived':'');
        $('job').title=j.tile===undefined?'':'Tile '+number(j.tile+1);
        text('error','');
        if(first&&tiles.size){first=false;fit()}else if(changed)terrainChanged();
    }catch(e){text('error','Map refresh: '+e.message+'; retrying automatically.')}
    finally{coverageBusy=false}
}

canvas.addEventListener('wheel',e=>{
    e.preventDefault();const r=canvas.getBoundingClientRect(),x=e.clientX-r.left,y=e.clientY-r.top,before=world(x,y);
    zoom=Math.max(.00005,Math.min(20,zoom*Math.exp(-e.deltaY*.0015)));const after=world(x,y);
    center[0]+=before[0]-after[0];center[1]+=before[1]-after[1];draw();
},{passive:false});
const pointers=new Map();let gesture=null;
function resetGesture(){
    const p=[...pointers.values()],r=canvas.getBoundingClientRect();viewRect=r;
    if(p.length>=2){const x=(p[0][0]+p[1][0])/2-r.left,y=(p[0][1]+p[1][1])/2-r.top;
        gesture={type:'pinch',distance:Math.max(1,Math.hypot(p[1][0]-p[0][0],p[1][1]-p[0][1])),zoom,anchor:world(x,y)};
    }else gesture=p.length?{type:'pan',x:p[0][0],y:p[0][1]}:null;
}
canvas.addEventListener('pointerdown',e=>{
    if(e.pointerType==='mouse'&&e.button!==0)return;followPlayer=null;
    clearTimeout(heightTimer);heightRequest?.abort();heightKey=null;
    pointers.set(e.pointerId,[e.clientX,e.clientY]);canvas.setPointerCapture(e.pointerId);resetGesture();
});
canvas.addEventListener('pointermove',e=>{
    const r=viewRect||canvas.getBoundingClientRect();
    if(pointers.has(e.pointerId)){
        pointers.set(e.pointerId,[e.clientX,e.clientY]);const p=[...pointers.values()];
        if(p.length>=2&&gesture?.type==='pinch'){
            const x=(p[0][0]+p[1][0])/2-r.left,y=(p[0][1]+p[1][1])/2-r.top,distance=Math.hypot(p[1][0]-p[0][0],p[1][1]-p[0][1]);
            zoom=Math.max(.00005,Math.min(20,gesture.zoom*distance/gesture.distance));center=[gesture.anchor[0]-(x-W/2)/zoom,gesture.anchor[1]-(y-H/2)/zoom];
        }else if(gesture?.type==='pan'){
            center[0]-=(e.clientX-gesture.x)/zoom;center[1]-=(e.clientY-gesture.y)/zoom;gesture.x=e.clientX;gesture.y=e.clientY;
        }
        draw();
    }
    hover(e.clientX-r.left,e.clientY-r.top);
});
function endPointer(e){if(pointers.delete(e.pointerId))resetGesture();const r=viewRect||canvas.getBoundingClientRect();hover(e.clientX-r.left,e.clientY-r.top)}
canvas.addEventListener('pointerup',endPointer);canvas.addEventListener('pointercancel',endPointer);canvas.addEventListener('lostpointercapture',endPointer);
$('view').onchange=terrainChanged;
$('contours').onchange=$('grid').onchange=terrainChanged;
$('showDeposits').onchange=$('showVillages').onchange=()=>{hoverDirty=true;markersChanged()};
$('fit').onclick=fit;$('origin').onclick=()=>{followPlayer=null;center=[0,0];zoom=.4;draw()};
$('goto').onsubmit=e=>{e.preventDefault();const x=Number($('x').value),z=Number($('z').value);if(Number.isFinite(x)&&Number.isFinite(z)){followPlayer=null;center=[x,z];zoom=.4;draw()}};
document.addEventListener('visibilitychange',()=>{if(!document.hidden){update();updatePlayers();updateDeposits();updateVillages();updateMetrics();schedule()}});
window.addEventListener('scroll',()=>{viewRect=canvas.getBoundingClientRect()},{passive:true});
window.addEventListener('resize',resize);
new ResizeObserver(resize).observe(canvas);clearMetrics();update();updatePlayers();updateDeposits();updateVillages();updateMetrics();setInterval(update,30000);setInterval(updatePlayers,2000);setInterval(updateDeposits,30000);setInterval(updateVillages,30000);setInterval(updateMetrics,5000);
