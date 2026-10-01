package dev.civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Immutable generated geometry, not a stock ledger. Only real blocks contain resources. */
public final class Deposits {
    public enum Kind { COAL, OIL }
    static boolean naturalRock(BlockState state){
        return state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(Blocks.TUFF)
                ||state.is(Blocks.ANDESITE)||state.is(Blocks.DIORITE)||state.is(Blocks.GRANITE)
                ||state.is(Blocks.CALCITE)||state.is(Blocks.DRIPSTONE_BLOCK)||state.is(Blocks.GRAVEL);
    }
    public record Site(int x,int y,int z,Kind kind,int radius,int depth) {
        public Site(int x,int y,int z,Kind kind,int radius){this(x,y,z,kind,radius,kind==Kind.OIL?32:24);}
        public boolean broad(){return radius>32;}
        public int centerY(){return y-depth;}
        public int halfHeight(){return broad()?(kind==Kind.OIL?5:4):(kind==Kind.OIL?8:5);}
        public int width(){return radius*2+1;}
        public int top(){return centerY()+(kind==Kind.OIL?(broad()?0:1):halfHeight());}
        public int ceiling(){return broad()&&kind==Kind.OIL?centerY()+2:centerY()+halfHeight();}
        public int bottom(){return centerY()-halfHeight();}
        public int minorRadius(){return broad()?Math.round(radius*(kind==Kind.COAL?.70f:.80f)):radius;}
        public double footprint(BlockPos p){
            if(Math.abs(p.getX()-x)>radius||Math.abs(p.getZ()-z)>radius)return Double.POSITIVE_INFINITY;
            boolean turned=broad()&&((x^z)&1)!=0;
            double dx=(p.getX()-x)/(double)(turned?minorRadius():radius);
            double dz=(p.getZ()-z)/(double)(turned?radius:minorRadius());
            double q=dx*dx+dz*dz;
            if(!broad())return q;
            double ripple=.07*Math.sin(dx*7+dz*3)+.045*Math.sin(dz*11-dx*2);
            return q/(1+ripple);
        }
        public boolean contains(BlockPos p){long distance=(long)(p.getX()-x)*(p.getX()-x)+(long)(p.getZ()-z)*(p.getZ()-z);
            if(!broad())return kind==Kind.OIL?p.getY()>=top()&&distance<=(long)radius*radius:Math.abs(p.getY()-y)<=16&&distance<=32*32;
            return (kind==Kind.OIL?p.getY()>=top():Math.abs(p.getY()-y)<=16)&&footprint(p)<=1;
        }
        public boolean body(BlockPos p){
            if(!broad()){long dx=p.getX()-x,dz=p.getZ()-z,dy=p.getY()-centerY(),rr=radius*radius,hh=halfHeight()*halfHeight();return (dx*dx+dz*dz)*hh+dy*dy*rr<=rr*hh;}
            double q=footprint(p);if(q>1)return false;
            if(kind==Kind.OIL){int depth=1+(int)Math.floor(5*(1-q));return p.getY()>=centerY()-depth+1&&p.getY()<=ceiling();}
            int middle=centerY()+(int)Math.round((p.getX()-x)/(double)radius*1.5);
            int thickness=q<.17?5:q<.55?3:2;
            return p.getY()>=middle-(thickness-1)/2&&p.getY()<=middle+thickness/2;
        }
        public int cells(){return width()*width()*(ceiling()-bottom()+1);}
        public BlockPos cell(int index){
            int height=ceiling()-bottom()+1,plane=index/height;
            if(plane==0)return new BlockPos(x,ceiling()-index%height,z);
            int ring=(int)Math.ceil((Math.sqrt(plane+1)-1)/2);
            int edge=ring*2,step=plane-(2*ring-1)*(2*ring-1);
            int dx,dz;
            if(step<edge){dx=ring;dz=-ring+1+step;}
            else if((step-=edge)<edge){dx=ring-1-step;dz=ring;}
            else if((step-=edge)<edge){dx=-ring;dz=ring-1-step;}
            else{step-=edge;dx=-ring+1+step;dz=-ring;}
            return new BlockPos(x+dx,ceiling()-index%height,z+dz);
        }
        public int workSize(){return cells();}
        public BlockPos workCell(int index){return cell(index);}
        public boolean loaded(ServerLevel l){
            for(int cx=(x-radius)>>4;cx<=(x+radius)>>4;cx++)for(int cz=(z-radius)>>4;cz<=(z+radius)>>4;cz++){
                boolean stock=false;
                for(int px=cx<<4;px<=(cx<<4)+15&&!stock;px++)for(int pz=cz<<4;pz<=(cz<<4)+15;pz++)
                    if(footprint(new BlockPos(px,centerY(),pz))<=1){stock=true;break;}
                if(stock&&!l.hasChunk(cx,cz))return false;
            }
            return true;
        }
    }
    // Worldgen workers and the server share immutable sampled sites; values never retain the level.
    private static final Map<ServerLevel,Map<Long,Optional<Site>>> CACHES=new WeakHashMap<>();
    private static Map<Long,Optional<Site>> cache(ServerLevel level){synchronized(CACHES){return CACHES.computeIfAbsent(level,ignored->Collections.synchronizedMap(new LinkedHashMap<>(256,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,Optional<Site>> entry){return size()>4096;}}));}}
    private static final Map<ServerLevel,Map<Long,Optional<Site>>> OIL_REGIONS=new WeakHashMap<>();
    private static Map<Long,Optional<Site>> oilCache(ServerLevel level){synchronized(OIL_REGIONS){return OIL_REGIONS.computeIfAbsent(level,ignored->Collections.synchronizedMap(new LinkedHashMap<>(64,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,Optional<Site>> entry){return size()>1024;}}));}}
    private static long mix(long x) { x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31); }
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static long cellHash(ServerLevel l,int x,int z){return mix(l.getSeed() ^ ((long)x*341873128712L) ^ ((long)z*132897987541L));}
    public static Site candidate(ServerLevel l,int cellX,int cellZ) {
        var cache=cache(l);long key=key(cellX,cellZ);var known=cache.get(key);if(known!=null)return known.orElse(null);
        if(!Geography.supported(l)){cache.put(key,Optional.empty());return null;}
        var coal=sampleCoal(l,cellX,cellZ);
        var oil=coal==null?oilRegion(l,Math.floorDiv(cellX,4),Math.floorDiv(cellZ,4)):null;
        var site=coal!=null?coal:oil!=null&&Math.floorDiv(oil.x(),256)==cellX&&Math.floorDiv(oil.z(),256)==cellZ?oil:null;
        cache.put(key,Optional.ofNullable(site));return site;
    }
    /** One oil field per suitable 4x4-cell region; failed individual rolls cannot strand an entire plains belt. */
    private static Site oilRegion(ServerLevel l,int regionX,int regionZ){
        var cache=oilCache(l);long key=key(regionX,regionZ);var known=cache.get(key);if(known!=null)return known.orElse(null);
        var generator=l.getChunkSource().getGenerator();var random=l.getChunkSource().randomState();
        long order=mix(l.getSeed() ^ ((long)regionX*0x632be59bd9b4e019L) ^ ((long)regionZ*0x9e3779b97f4a7c15L));
        int start=(int)Math.floorMod(order,16),step=((int)(order>>>8)&7)*2+1;
        Site result=null;
        for(int n=0;n<16&&result==null;n++){
            int index=(start+n*step)&15,cellX=regionX*4+(index&3),cellZ=regionZ*4+(index>>>2);
            long hash=cellHash(l,cellX,cellZ);
            for(int attempt=0;attempt<4;attempt++){
                long choice=mix(hash+attempt*0x9e3779b97f4a7c15L);
                int x=cellX*256+80+(int)Math.floorMod(choice,96),z=cellZ*256+80+(int)Math.floorMod(choice>>>24,96);
                int y=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,l,random);
                if(y>=l.getMaxBuildHeight()-8||y-40<l.getMinBuildHeight()+5)continue;
                var biome=generator.getBiomeSource().getNoiseBiome(x>>2,(y-1)>>2,z>>2,random.sampler());
                if(!biome.is(IndustrialContent.OIL_REGIONS))continue;
                var site=new Site(x,y,z,Kind.OIL,34+(int)Math.floorMod(choice>>>40,3));
                if(groundFits(l,site)&&sampleCoal(l,cellX,cellZ)==null){result=site;break;}
            }
        }
        cache.put(key,Optional.ofNullable(result));return result;
    }
    private static Site sampleCoal(ServerLevel l,int cellX,int cellZ) {
        long hash=cellHash(l,cellX,cellZ);
        if(Math.floorMod(hash,3)!=0) return null;
        var generator=l.getChunkSource().getGenerator(); var random=l.getChunkSource().randomState();
        // Try a few positions inside each selected region. A single point on a Tectonic cliff
        // must not make an otherwise suitable 256-block region permanently empty.
        for(int attempt=0;attempt<4;attempt++) {
            long choice=mix(hash+attempt*0x9e3779b97f4a7c15L);
            int x=cellX*256+80+(int)Math.floorMod(choice,96);
            int z=cellZ*256+80+(int)Math.floorMod(choice>>>24,96);
            int y=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,l,random);
            if(y>=l.getMaxBuildHeight()-8 || y-40<l.getMinBuildHeight()+5)continue;
            // Biomes can change with altitude; classify the ground, not an arbitrary Y=64 slice.
            var biome=generator.getBiomeSource().getNoiseBiome(x>>2,(y-1)>>2,z>>2,random.sampler());
            if(!biome.is(IndustrialContent.COAL_REGIONS))continue;
            Site site=new Site(x,y,z,Kind.COAL,70+(int)Math.floorMod(choice>>>40,11));
            // Some seams crop out where natural rock meets air or river water.
            // The visible blocks are part of the same extractable body, not a clue.
            if(Math.floorMod(choice,5)<3){
                int depth=outcropDepth(l,site);
                if(depth>0){var exposed=new Site(x,y,z,Kind.COAL,site.radius(),depth);if(groundFits(l,exposed))return exposed;}
            }
            if(groundFits(l,site))return site;
        }
        return null;
    }
    private static int outcropDepth(ServerLevel l,Site site){
        var generator=l.getChunkSource().getGenerator();var random=l.getChunkSource().randomState();
        int r=site.radius(),m=site.minorRadius();if(((site.x()^site.z())&1)!=0){int t=r;r=m;m=t;}
        int[][] offsets={{r/2,0},{-r/2,0},{0,m/2},{0,-m/2},
                {r/2,m/2},{r/2,-m/2},{-r/2,m/2},{-r/2,-m/2},
                {3*r/4,0},{-3*r/4,0},{0,3*m/4},{0,-3*m/4}};
        for(var offset:offsets){
            int x=site.x()+offset[0],z=site.z()+offset[1];
            int surface=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,l,random);
            var column=generator.getBaseColumn(x,z,l,random);
            for(int drop=0;drop<=4;drop++){
                int rockY=surface-1-drop;
                var state=column.getBlock(rockY);
                if(naturalRock(state)){
                    int tilt=(int)Math.round(offset[0]/(double)site.radius()*1.5);
                    int depth=site.y()-rockY+tilt;
                    if(depth>=4&&depth<=20)return depth;
                    break;
                }
                // A rock floor beneath water is visible; dirt, wood or other cover is not.
                if(state.getFluidState().isEmpty())break;
            }
        }
        return -1;
    }
    private static boolean groundFits(ServerLevel l,Site site) {
        var generator=l.getChunkSource().getGenerator();var random=l.getChunkSource().randomState();
        int r=site.radius(),shortRadius=site.minorRadius(),limit=site.kind()==Kind.OIL?8:16;
        if(((site.x()^site.z())&1)!=0){int swap=r;r=shortRadius;shortRadius=swap;}
        int diagonalX=(int)Math.round(r/Math.sqrt(2)),diagonalZ=(int)Math.round(shortRadius/Math.sqrt(2));
        int[][] offsets={{0,0},{r,0},{-r,0},{0,shortRadius},{0,-shortRadius},{diagonalX,diagonalZ},{diagonalX,-diagonalZ},{-diagonalX,diagonalZ},{-diagonalX,-diagonalZ}};
        int solidCoalSamples=0;
        for(var offset:offsets) {
            int x=site.x()+offset[0],z=site.z()+offset[1];
            int surface=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,l,random);
            if(Math.abs(surface-site.y())>limit)return false;
            var column=generator.getBaseColumn(x,z,l,random);
            var top=column.getBlock(surface-1);
            // The center is a dry place for the extractor. A sampled bank or
            // cave opening farther out is a desirable coal outcrop, not a failed field.
            if((site.kind()==Kind.OIL || offset[0]==0&&offset[1]==0)
                    &&(top.isAir() || !top.getFluidState().isEmpty()))return false;
            // Natural worlds need a buried, solid body. Development superflat fixtures build
            // their test deposits below the shallow flat layers explicitly.
            if(!(generator instanceof FlatLevelSource)) {
                var rock=column.getBlock(site.centerY());
                boolean solid=!rock.isAir() && rock.getFluidState().isEmpty() && !rock.is(Blocks.BEDROCK);
                if(site.kind()==Kind.OIL || offset[0]==0&&offset[1]==0){if(!solid)return false;}
                if(solid)solidCoalSamples++;
            }
        }
        // Prevent a mostly open seam while allowing individual river/cave cuts.
        return site.kind()==Kind.OIL || generator instanceof FlatLevelSource || solidCoalSamples>=6;
    }
    /** Optional authored-map geometry in vanilla command storage; blocks still hold all stock.
     * No entries exist in ordinary worlds. Bounded and persistent without a second stock ledger. */
    private static List<Site> authored(ServerLevel l){
        var entries=l.getServer().getCommandStorage().get(net.minecraft.resources.ResourceLocation.parse("civilization:authored_deposits")).getList(l.dimension().location().toString(),10);
        var sites=new ArrayList<Site>();
        for(int i=0;i<Math.min(64,entries.size());i++){var t=entries.getCompound(i);int radius=t.getInt("radius"),kind=t.getInt("kind"),y=t.getInt("y");
            if(radius<8||radius>32||kind<0||kind>1||y-40<l.getMinBuildHeight()+5||y>=l.getMaxBuildHeight()-8)continue;
            sites.add(new Site(t.getInt("x"),y,t.getInt("z"),Kind.values()[kind],radius));
        }return sites;
    }
    public static Site at(ServerLevel l,BlockPos p) { for(var site:authored(l))if(site.contains(p))return site;var s=candidate(l,Math.floorDiv(p.getX(),256),Math.floorDiv(p.getZ(),256));return s!=null&&s.contains(p)?s:null; }
    public static Site nearby(ServerLevel l,BlockPos p) {
        Site best=null;long distance=192L*192;
        for(var s:authored(l)){long d=(long)(p.getX()-s.x)*(p.getX()-s.x)+(long)(p.getZ()-s.z)*(p.getZ()-s.z);if(d<distance){distance=d;best=s;}}
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var s=candidate(l,Math.floorDiv(p.getX(),256)+dx,Math.floorDiv(p.getZ(),256)+dz);if(s==null)continue;
            long d=(long)(p.getX()-s.x)*(p.getX()-s.x)+(long)(p.getZ()-s.z)*(p.getZ()-s.z);
            if(d<distance){distance=d;best=s;}
        }return best;
    }
}
