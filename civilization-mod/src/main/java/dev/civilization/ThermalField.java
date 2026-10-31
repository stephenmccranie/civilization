package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** Sparse, block-resolution heat above regional climate. Only loaded cells simulate; no forced chunks. */
public final class ThermalField extends SavedData {
    final int maxCells;
    ThermalField(){this(ThermalRules.MAX_CELLS);}
    ThermalField(int maxCells){this.maxCells=maxCells;}
    Long2DoubleOpenCustomHashMap energy = new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);
    final Long2DoubleOpenCustomHashMap pending = new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);
    double atmosphereExchange;
    final ThermalTimeline timeline=new ThermalTimeline();
    private boolean clocked;
    private long scheduledTarget=Long.MIN_VALUE;
    void beginTick(long now){if(!clocked){timeline.resume(now);clocked=true;}else timeline.observe(now);}
    double queuedHeat(){return timeline.heat();}
    int timedReleases(){return timeline.entries();}
    // V2 stores only climate-relative heat; regional climate supplies the background.
    // Biome identity is stable during normal play. Bound this transient cache and expire it
    // for /fillbiome and reload changes; sunlight and regional rain are never cached here.
    private final Long2ObjectOpenCustomHashMap<ThermalRules.Climate> biomeClimates=new Long2ObjectOpenCustomHashMap<>(HeatMaps.POSITIONS);
    private final Map<ResourceKey<Biome>,ThermalRules.Climate> profiles=new HashMap<>();
    private long climateExpires=Long.MIN_VALUE;
    private static final Direction[] NEIGHBORS=Direction.values();
    private static final int[] OPEN_AIR={15,15,15,15,15,15},CLOSED={0,0,0,0,0,0};
    public static ThermalField get(ServerLevel l) {
        return l.getDataStorage().computeIfAbsent(new Factory<>(ThermalField::new,ThermalField::load),"civilization_heat");
    }
    public static boolean supported(Level l, BlockPos p) {
        return ThermalConfig.enabled() && l.dimension().equals(Level.OVERWORLD) && p.getY() >= l.getMinBuildHeight() && p.getY() < l.getMaxBuildHeight() && l.hasChunkAt(p);
    }
    public static double ambient(ServerLevel l, BlockPos p) {
        var biome=l.getBiome(p);
        var profile=ThermalRules.profile(biome.unwrapKey().orElse(null),biome.value().getBaseTemperature());
        return ThermalRules.climate(profile,p.getY(),ThermalRules.daylightPhase(l.getDayTime()),RegionalWeather.cooling(l,p));
    }
    public static double conductance(BlockState s) {return PhysicalMaterials.legacyConductance(s);}
    public static double capacity(BlockState s) {return PhysicalMaterials.legacyCapacity(s);}
    private static double cellCapacity(Level level,BlockPos pos,BlockState state){
        if(state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)
            return capacity(SlabIntegration.material(state));
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock))return capacity(state);
        var pieces=CutCells.read(level,pos);double sum=0;
        for(var piece:pieces)sum+=piece==null?capacity(Blocks.AIR.defaultBlockState()):capacity(piece);
        return sum/8;
    }
    private static double cellConductance(Level level,BlockPos pos,BlockState state){
        if(state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)
            return conductance(SlabIntegration.material(state));
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock))return conductance(state);
        var pieces=CutCells.read(level,pos);double sum=0;int occupied=0;
        for(var piece:pieces)if(piece!=null){sum+=conductance(piece);occupied++;}
        return occupied==0?conductance(state):sum/occupied;
    }
    private static int openFace(Level level,BlockPos pos,BlockState state,Direction face){
        if(state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)return 0;
        if(state.isAir() || ((state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)))return 15;
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock))return 0;
        var pieces=CutCells.read(level,pos);int bits=0;
        for(int i=0;i<8;i++){
            boolean onFace=switch(face){
                case WEST -> (i&1)==0; case EAST -> (i&1)!=0;
                case DOWN -> (i&2)==0; case UP -> (i&2)!=0;
                case NORTH -> (i&4)==0; case SOUTH -> (i&4)!=0;
            };
            if(!onFace||pieces[i]!=null)continue;
            int bit=switch(face.getAxis()){
                case X -> (i>>1)&3;
                case Y -> (i&1)|((i>>1)&2);
                case Z -> i&3;
            };
            bits|=1<<bit;
        }
        return bits;
    }
    public double excess(ServerLevel l,BlockPos p) {
        return localTemperature(l,p)-ambient(l,p);
    }
    double localTemperature(ServerLevel l,BlockPos p){
        return ambient(l,p)+energy.get(p.asLong())/cellCapacity(l,p,l.getBlockState(p));
    }
    public static double temperature(ServerLevel l,BlockPos p) {
        if(!supported(l,p))return 18;
        if(l.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA))return BlockHeatSources.TEMPERATURE;
        return get(l).localTemperature(l,p);
    }
    public static void fuel(Level level,BlockPos pos,double heat) {
        if(!(level instanceof ServerLevel l)||!supported(l,pos)||!Double.isFinite(heat)||heat<=0)return;
        var f=get(l);long key=pos.asLong();
        if(!f.pending.containsKey(key)&&f.pending.size()>=4096)return;
        f.pending.merge(key,heat,Double::sum);f.setDirty();
    }
    public static double efficiency(Level level,BlockPos pos) {
        if(!(level instanceof ServerLevel l)||!supported(l,pos))return 1;
        double sum=0;int n=0;
        for(var d:NEIGHBORS){var at=pos.relative(d);if(supported(l,at)&&!l.getBlockState(at).isSolidRender(l,at)){sum+=temperature(l,at);n++;}}
        return ThermalRules.efficiency(n==0?ambient(l,pos):sum/n,!l.canSeeSky(pos.above()));
    }
    /** Registered structure only: never flood-fill into the player's house. */
    static Set<BlockPos> body(ServerLevel l,BlockPos p) {
        var state=l.getBlockState(p);var cells=new HashSet<BlockPos>();cells.add(p);
        boolean heatedMachine=state.getBlock() instanceof IndustrialBlock || state.getBlock() instanceof OilEngineBlock
                || state.getBlock() instanceof WorkshopBlock
                || l.getBlockEntity(p) instanceof KilnBlockEntity kiln && kiln.requiresStructure();
        var parts=heatedMachine?MachineStructure.guideParts(state):List.<MachineStructure.Part>of();
        if(!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING))return cells;
        var front=state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        for(var part:parts)if(!part.material().equals("air")) {
            var q=MachineStructure.position(p,front,part);
            if(supported(l,q)&&MachineStructure.matches(l,q,part,front))cells.add(q);
        }
        return cells;
    }
    void add(ServerLevel l,BlockPos p,double amount) {
        if(clocked){
            if(!timeline.add(p.asLong(),amount,scheduled==null?timeline.committed:scheduledTarget))atmosphereExchange+=amount;
            setDirty();return;
        }
        long k=p.asLong();
        if(!energy.containsKey(k)&&energy.size()>=maxCells){
            if(scheduled==null)reserveHeadroom();
            if(energy.size()>=maxCells){atmosphereExchange+=amount;setDirty();return;}
        }
        energy.addTo(k,amount);
        if(scheduled!=null)scheduled.incoming.addTo(k,amount);
        setDirty();
    }
    private record Cell(BlockPos pos,BlockState state,double capacity,double conductance,double ambient,boolean outside,int[] open) {}
    private record WeakCell(long key,double magnitude) {}
    /** A full field must not permanently prevent new hot sources or upward diffusion. */
    private void reserveHeadroom(){
        if(energy.size()<maxCells-128)return;
        int remove=energy.size()-(maxCells-Math.min(8192,maxCells/4));
        if(remove<=0)return;
        var weakest=new PriorityQueue<WeakCell>(Comparator.comparingDouble(WeakCell::magnitude).reversed());
        for(var entry:energy.long2DoubleEntrySet()){
            if(pending.containsKey(entry.getLongKey()))continue;
            var cell=new WeakCell(entry.getLongKey(),Math.abs(entry.getDoubleValue()));
            if(weakest.size()<remove)weakest.add(cell);
            else if(cell.magnitude()<weakest.peek().magnitude()){weakest.remove();weakest.add(cell);}
        }
        for(var cell:weakest)atmosphereExchange+=energy.remove(cell.key());
        if(!weakest.isEmpty())setDirty();
    }
    /** Complete simulated second for deterministic checks and explicit callers. */
    public void step(ServerLevel l) { if(ThermalConfig.enabled()){releaseSources(l);diffuse(l,4);} }
    /** Immediate quarter-second exchange for focused callers; production uses scheduledStep. */
    public void quarterStep(ServerLevel l,boolean release) { if(!ThermalConfig.enabled())return;if(release)releaseSources(l);diffuse(l,1); }
    private final HeatSourceQueue sourceQueue=new HeatSourceQueue();
    void scheduledSources(ServerLevel l){if(!ThermalConfig.enabled())return;sourceQueue.tick(l.getGameTime(),pending.keySet(),key->releaseSource(l,key));}
    void releaseSources(ServerLevel l) {
        for(long key:pending.keySet().toLongArray())releaseSource(l,key);
    }
    private void releaseSource(ServerLevel l,long key){
        double available=pending.get(key);if(available<=0)return;
        var p=BlockPos.of(key);if(!supported(l,p))return;
        if(scheduled!=null&&energy.size()>=maxCells-128)return;
        if(scheduled==null)reserveHeadroom();
        double used=Math.min(ThermalRules.MAX_SOURCE_HEAT_PER_SECOND,available);
        var body=body(l,p);var vents=new HashSet<BlockPos>();
        int top=body.stream().mapToInt(BlockPos::getY).max().orElse(p.getY());
        for(var at:body)for(var d:NEIGHBORS) {
            var q=at.relative(d);
            if(body.contains(q)||!supported(l,q))continue;
            if(d==Direction.UP&&at.getY()==top)vents.add(q);
        }
        // Only exposed air receives exhaust. If every outlet is capped, the
        // spent fuel heat stays in the machine shell instead of heating a roof.
        var openVents=new HashSet<BlockPos>();
        for(var at:vents)if(l.getBlockState(at).isAir())openVents.add(at);
        vents=openVents;
        // Reserve the whole finite batch before spending its pending budget.
        if(clocked&&timeline.headroom()<body.size()+vents.size())return;
        double topShare=vents.isEmpty()?0:ThermalRules.MACHINE_TOP_SHARE;
        // Fuel waste enters only the casing and the open exhaust; surrounding air
        // warms through ordinary face exchange rather than a special side shortcut.
        double bodyHeat=used*(1-topShare);
        // The controller is one part of the hot machine shell, not a private
        // radiator. Every structural block receives a share of spent fuel heat.
        for(var at:body)add(l,at,bodyHeat/body.size());
        // Exhaust enters at the physical outlet. The solver, not source placement,
        // must transport it through every subsequent air cell and opening.
        for(var vent:vents)add(l,vent,used*topShare/vents.size());
        if(available<=used)pending.remove(key);else pending.put(key,available-used);
        setDirty();
    }
    private static boolean loaded(ServerLevel l,BlockPos p,Long2ByteOpenCustomHashMap chunks) {
        if(!l.dimension().equals(Level.OVERWORLD)||p.getY()<l.getMinBuildHeight()||p.getY()>=l.getMaxBuildHeight())return false;
        long key=net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4);
        byte value=chunks.get(key);
        if(value==0){value=(byte)(l.hasChunkAt(p)?1:-1);chunks.put(key,value);}
        return value==1;
    }
    private Exchange scheduled;
    // A busy field slows its own simulation instead of borrowing an unbounded server tick.
    static final long SOLVER_BUDGET_NS=5_000_000;
    long completedExchanges;
    private long lastExchangeTick=Long.MIN_VALUE;
    boolean exchanging(){return scheduled!=null;}
    int activeCells(){return energy.size();}
    int queuedSources(){return pending.size();}
    double simulationAgeSeconds(ServerLevel level){
        if(clocked)return Math.max(0,level.getGameTime()-timeline.committed)/20d;
        return energy.isEmpty()?0:lastExchangeTick==Long.MIN_VALUE?Double.POSITIVE_INFINITY:
                Math.max(0,level.getGameTime()-lastExchangeTick)/20d;
    }
    /** Snapshot, solve and prepare publication incrementally; never publish half an exchange. */
    void scheduledStep(ServerLevel l,int phase) {
        // Snapshot copies and publication also consume operations. A 16k ceiling
        // alone forces ordinary 30k-cell fields below four steps/second even on
        // a fast CPU. The separate soft time deadline still limits each call.
        if(clocked){advanceTimed(l,l.getGameTime(),65536);return;}
        if(!ThermalConfig.enabled()){scheduled=null;return;}
        long deadline=System.nanoTime()+SOLVER_BUDGET_NS;
        if(phase==0&&scheduled==null&&!energy.isEmpty()){reserveHeadroom();scheduled=new Exchange(l);}
        if(scheduled==null)return;
        scheduled.chunks.clear();
        int work=0;
        do {
            if(scheduled.copyCursor<scheduled.keys.length)scheduled.copy(64);
            else if(scheduled.cursor<scheduled.keys.length)scheduled.read(32);
            else if(scheduled.prepare(64)){
                energy=scheduled.result;atmosphereExchange+=scheduled.atmosphereExchange;scheduled=null;
                completedExchanges++;lastExchangeTick=l.getGameTime();setDirty();return;
            }
            work+=32;
        }while(work<16384&&System.nanoTime()<deadline);
    }
    /** Real-time inputs, fixed physical steps, bounded execution. Late work stays explicitly late. */
    void advanceTimed(ServerLevel level,long now,int workLimit){
        if(!ThermalConfig.enabled())return;
        beginTick(now);
        if(scheduled==null){
            if(energy.isEmpty()&&timeline.empty()){timeline.commit(now);return;}
            scheduledTarget=timeline.committed+5;
            if(now<scheduledTarget)return;
            reserveHeadroom();scheduled=new Exchange(level,timeline.through(scheduledTarget));
        }
        long deadline=System.nanoTime()+SOLVER_BUDGET_NS;
        scheduled.chunks.clear(); // Recheck loaded status across yielded server ticks.
        int work=0;
        do{
            int count=Math.min(32,workLimit-work);
            if(count<=0)return;
            if(scheduled.copyCursor<scheduled.copyKeys.length)scheduled.copy(count);
            else if(!scheduled.inputsReady)scheduled.stageInputs(count);
            else if(scheduled.cursor<scheduled.keys.length)scheduled.read(count);
            else if(scheduled.prepare(count)){
                energy=scheduled.result;atmosphereExchange+=scheduled.atmosphereExchange;
                timeline.commit(scheduledTarget);scheduled=null;completedExchanges++;lastExchangeTick=now;setDirty();return;
            }
            work+=count;
        }while(work<workLimit&&System.nanoTime()<deadline);
    }
    private void diffuse(ServerLevel l,int steps) {
        if(scheduled!=null)throw new IllegalStateException("Cannot synchronously step an active thermal exchange");
        for(int sub=0;sub<steps;sub++){
            reserveHeadroom();
            var exchange=new Exchange(l);exchange.copy(Integer.MAX_VALUE);exchange.read(Integer.MAX_VALUE);
            while(!exchange.prepare(Integer.MAX_VALUE)){}
            energy=exchange.result;atmosphereExchange+=exchange.atmosphereExchange;setDirty();
        }
    }
    /** Pending transfers never modify the source field until every pair has been read. */
    private final class Exchange {
        final ServerLevel level;
        // Copy the source field in bounded slices. Emissions arriving during the copy
        // stay in the live field and its journal, so they enter the published result once.
        final Long2DoubleOpenCustomHashMap snapshot=new Long2DoubleOpenCustomHashMap(Math.min(energy.size(),32768),HeatMaps.POSITIONS);
        final Long2DoubleOpenCustomHashMap result=snapshot;
        final Long2DoubleOpenCustomHashMap incoming=new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);
        final long[] copyKeys=energy.keySet().toLongArray();
        long[] keys=copyKeys;
        private final List<Long2DoubleOpenCustomHashMap> inputFrames;
        private int inputFrame;
        private it.unimi.dsi.fastutil.objects.ObjectIterator<Long2DoubleMap.Entry> inputIterator;
        boolean inputsReady;
        final Long2ObjectOpenCustomHashMap<Cell> cache=new Long2ObjectOpenCustomHashMap<>(Math.max(16,Math.min(keys.length*7,65536)),HeatMaps.POSITIONS);
        final Long2ByteOpenCustomHashMap chunks=new Long2ByteOpenCustomHashMap(HeatMaps.POSITIONS);
        final IdentityHashMap<BlockState,double[]> materials=new IdentityHashMap<>();
        final Long2DoubleOpenCustomHashMap delta=new Long2DoubleOpenCustomHashMap(Math.max(16,Math.min(keys.length*7,65536)),HeatMaps.POSITIONS);
        final Long2IntOpenCustomHashMap heights=new Long2IntOpenCustomHashMap(HeatMaps.POSITIONS);
        final Long2DoubleOpenCustomHashMap rainCooling=new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);
        final double sun;
        double atmosphereExchange;
        it.unimi.dsi.fastutil.objects.ObjectIterator<Long2DoubleMap.Entry> changes;
        int copyCursor,cursor;
        Exchange(ServerLevel level){this(level,List.of());inputsReady=true;}
        Exchange(ServerLevel level,List<Long2DoubleOpenCustomHashMap> inputs){
            inputFrames=inputs;
            heights.defaultReturnValue(Integer.MIN_VALUE);
            // The climate cache grows with cells actually read, not the whole field up front.
            this.level=level;sun=ThermalRules.daylightPhase(level.getDayTime());
            if(level.getGameTime()>=climateExpires||climateExpires==Long.MIN_VALUE){
                biomeClimates.clear();profiles.clear();climateExpires=level.getGameTime()+200;
            }
        }
        void copy(int limit){
            int end=(int)Math.min(copyKeys.length,(long)copyCursor+limit);
            while(copyCursor<end){
                long key=copyKeys[copyCursor++];
                snapshot.put(key,energy.get(key)-incoming.get(key));
            }
        }
        void stageInputs(int limit){
            int work=0;
            while(work<limit){
                if(inputIterator==null||!inputIterator.hasNext()){
                    if(inputFrame==inputFrames.size()){
                        keys=snapshot.keySet().toLongArray();inputsReady=true;return;
                    }
                    inputIterator=inputFrames.get(inputFrame++).long2DoubleEntrySet().fastIterator();continue;
                }
                var entry=inputIterator.next();long key=entry.getLongKey();
                if(snapshot.containsKey(key)||snapshot.size()<maxCells)snapshot.addTo(key,entry.getDoubleValue());
                else atmosphereExchange+=entry.getDoubleValue();work++;
            }
        }
        Cell cell(BlockPos p){
            long key=p.asLong();var cached=cache.get(key);if(cached!=null)return cached;
            var state=level.getBlockState(p);
            var material=state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock
                    ?new double[]{cellCapacity(level,p,state),cellConductance(level,p,state)}
                    :materials.computeIfAbsent(state,s->new double[]{capacity(s),conductance(s)});
            var profile=biomeClimates.get(key);
            if(profile==null){
                var biome=level.getBiome(p);
                var biomeKey=biome.unwrapKey().orElse(null);
                profile=biomeKey==null?ThermalRules.profile(null,biome.value().getBaseTemperature())
                        :profiles.computeIfAbsent(biomeKey,k->ThermalRules.profile(k,biome.value().getBaseTemperature()));
                if(biomeClimates.size()<131072)biomeClimates.put(key,profile);
            }
            long district=RegionalWeather.key(p.getX(),p.getZ());
            if(!rainCooling.containsKey(district))rainCooling.put(district,RegionalWeather.cooling(level,p));
            double ambient=ThermalRules.climate(profile,p.getY(),sun,rainCooling.get(district));
            long column=net.minecraft.world.level.ChunkPos.asLong(p.getX(),p.getZ());
            int height=heights.get(column);
            if(state.isAir()&&height==Integer.MIN_VALUE){height=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,p.getX(),p.getZ());heights.put(column,height);}
            int[] faces;
            if(state.isAir())faces=OPEN_AIR;
            else if(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock || state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock){
                faces=new int[NEIGHBORS.length];
                for(var direction:NEIGHBORS)faces[direction.ordinal()]=openFace(level,p,state,direction);
            }else faces=CLOSED;
            var next=new Cell(p,state,material[0],material[1],ambient,state.isAir()&&height<=p.getY(),faces);
            cache.put(key,next);return next;
        }
        void read(int limit){
            int end=(int)Math.min(keys.length,(long)cursor+limit);
            // Chunk availability may change between batches; never use the cache to force a world read.
            while(cursor<end){
                long key=keys[cursor++];var p=BlockPos.of(key);if(!loaded(level,p,chunks))continue;
                var a=cell(p);
                // Lava is an externally maintained reservoir. BlockHeatSources owns
                // its bounded surface exchange; treating its stored anomaly as cold
                // here would create a second, contradictory heat-transfer path.
                if(a.state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))continue;
                double ta=a.ambient+snapshot.get(key)/a.capacity;
                if(a.outside){double loss=(ta-a.ambient)*a.capacity*ThermalRules.OUTDOOR_EXCHANGE;delta.addTo(key,-loss);atmosphereExchange+=loss;}
                if(a.outside&&p.getY()==level.getMaxBuildHeight()-1){
                    double outflow=ThermalRules.EXPOSED_BUOYANT_RISE*Math.max(0,ta-a.ambient);
                    delta.addTo(key,-outflow);atmosphereExchange+=outflow;
                }
                for(var d:NEIGHBORS){
                    var q=p.relative(d);long k=q.asLong();
                    double neighborEnergy=snapshot.get(k);
                    boolean exists=neighborEnergy!=0||snapshot.containsKey(k);
                    if(exists&&Long.compare(key,k)>0)continue;
                    if(!exists&&Math.abs(ta-a.ambient)<.1)continue;
                    if(q.getY()<level.getMinBuildHeight()||q.getY()>=level.getMaxBuildHeight())continue;
                    // The center's chunk was checked above. Interior neighbors
                    // share that loaded chunk; only boundary crossings need a lookup.
                    if(((p.getX()^q.getX())>>4)!=0||((p.getZ()^q.getZ())>>4)!=0)
                        if(!loaded(level,q,chunks))continue;
                    var b=cell(q);double tb=b.ambient+neighborEnergy/b.capacity;
                    if(b.state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))continue;
                    double openness=Integer.bitCount(a.open[d.ordinal()]&b.open[d.getOpposite().ordinal()])/4d;
                    double base=ThermalRules.exchangeRate(a.state.isAir(),b.state.isAir(),d,ta,tb,a.conductance,b.conductance);
                    double air=ThermalRules.exchangeRate(true,true,d,ta,tb,1,1);
                    double rate=base*(1-openness)+air*openness;
                    // Sky-exposed air reserves more of its stable exchange budget
                    // for upward convection instead of spreading equally sideways.
                    if(d.getAxis().isHorizontal()&&a.state.isAir()&&b.state.isAir()&&(a.outside||b.outside))
                        rate=ThermalRules.EXPOSED_AIR_MIXING;
                    double flow=(ta-tb)*rate+openness*ThermalRules.buoyantFlux(a.state.isAir(),b.state.isAir(),a.outside,b.outside,
                            d,ta,tb,a.ambient,b.ambient);
                    delta.addTo(key,-flow);delta.addTo(k,flow);
                }
            }
        }
        boolean prepare(int limit){
            if(cursor!=keys.length)throw new IllegalStateException("Incomplete thermal exchange");
            if(changes==null)changes=delta.long2DoubleEntrySet().fastIterator();
            int work=0;
            while(changes.hasNext()&&work++<limit){
                var e=changes.next();long key=e.getLongKey();var c=cache.get(key);
                double value=snapshot.getOrDefault(key,0d)+e.getDoubleValue();
                if(Math.abs(value)<.0001 || c.outside&&Math.abs(value)/c.capacity<.05){
                    result.remove(key);atmosphereExchange+=value;
                }else if(result.containsKey(key)||result.size()<maxCells){
                    double kept=Math.clamp(value,-6000,6000);result.put(key,kept);atmosphereExchange+=value-kept;
                }else atmosphereExchange+=value;
            }
            if(changes.hasNext())return false;
            // Fuel/emissions remain immediately visible and saveable in the published field.
            // Merge their journal only after diffusion, so each is included exactly once.
            var additions=incoming.long2DoubleEntrySet().fastIterator();
            while(additions.hasNext()&&work++<limit){
                var e=additions.next();long key=e.getLongKey();
                if(result.containsKey(key)||result.size()<maxCells)result.addTo(key,e.getDoubleValue());
                else atmosphereExchange+=e.getDoubleValue();
                additions.remove();
            }
            return incoming.isEmpty();
        }
    }
    static ThermalField load(CompoundTag t,HolderLookup.Provider lookup) {
        var f=new ThermalField();
        // Old absolute-temperature references filled the field with stale nighttime ground.
        // Thermal state is transient; do not migrate those cells into the climate-relative model.
        if(t.getInt("version")>=2){read(t,"cells",f.energy,ThermalRules.MAX_CELLS);read(t,"sources",f.pending,4096);f.atmosphereExchange=t.getDouble("atmosphereExchange");if(t.contains("timeline"))f.timeline.load(t.getCompound("timeline"));}
        else f.setDirty();
        return f;
    }
    private static void read(CompoundTag t,String key,Map<Long,Double> out,int max) {
        for(var n:t.getList(key,10)){var e=(CompoundTag)n;double v=e.getDouble("energy");if(out.size()<max&&Double.isFinite(v)&&(key.equals("sources")?v>0:true))out.put(e.getLong("pos"),v);}
    }
    private static ListTag write(Map<Long,Double> map) {
        var list=new ListTag();map.forEach((p,v)->{var t=new CompoundTag();t.putLong("pos",p);t.putDouble("energy",v);list.add(t);});return list;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider lookup){t.putInt("version",3);t.put("cells",write(energy));t.put("sources",write(pending));t.putDouble("atmosphereExchange",atmosphereExchange);t.put("timeline",timeline.save());return t;}
}
