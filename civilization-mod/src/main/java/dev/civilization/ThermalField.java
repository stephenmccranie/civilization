package dev.civilization;

import java.util.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
    private final Scratch scratch=new Scratch();
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
        if(state.getBlock() instanceof SlabBlock&&state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)
            return capacity(SlabIntegration.material(state));
        if(state.getBlock().getClass()==SlabBlock.class){
            double full=capacity(SlabIntegration.material(state));
            return (full+capacity(Blocks.AIR.defaultBlockState()))/2;
        }
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock))return capacity(state);
        var pieces=CutCells.read(level,pos);double sum=0;
        for(var piece:pieces)sum+=piece==null?capacity(Blocks.AIR.defaultBlockState()):capacity(piece);
        return sum/8;
    }
    private static int openFace(BlockState state,BlockState[] pieces,Direction face){
        if(state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE)return 0;
        if(state.isAir() || ((state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock)
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)))return 15;
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock))return 0;
        int bits=0;
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
    private record Material(double capacity,double conductance,int[] open,boolean air,boolean lava) {}
    private static Material material(Level level,BlockPos pos,BlockState state){
        boolean air=state.isAir(),lava=state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
        if(!(state.getBlock() instanceof CutBlock || state.getBlock() instanceof SlabBlock)){
            boolean open=(state.getBlock() instanceof DoorBlock||state.getBlock() instanceof TrapDoorBlock)
                &&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);
            return new Material(capacity(state),conductance(state),air||open?OPEN_AIR:CLOSED,air,lava);
        }
        if(state.getBlock() instanceof SlabBlock&&state.getValue(SlabBlock.TYPE)==net.minecraft.world.level.block.state.properties.SlabType.DOUBLE){
            var full=SlabIntegration.material(state);return new Material(capacity(full),conductance(full),CLOSED,false,lava);
        }
        var pieces=CutCells.read(level,pos);double capacity=0,conductance=0;int occupied=0;
        for(var piece:pieces){
            capacity+=piece==null?capacity(Blocks.AIR.defaultBlockState()):capacity(piece);
            if(piece!=null){conductance+=conductance(piece);occupied++;}
        }
        int[] faces=new int[NEIGHBORS.length];for(var d:NEIGHBORS)faces[d.ordinal()]=openFace(state,pieces,d);
        return new Material(capacity/8,occupied==0?conductance(state):conductance/occupied,faces,false,lava);
    }
    private record Cell(BlockPos pos,Material material,double ambient,boolean outside) {}
    private record WeakCell(long key,double magnitude) {}
    /** Local slots avoid hashing packed world positions for every interface. */
    private static final class SectionSlots {
        final long key;
        private Int2IntOpenHashMap sparse=new Int2IntOpenHashMap(8);
        private int[] dense;
        private net.minecraft.world.level.chunk.LevelChunkSection live;
        private int liveSlice=-1;
        private boolean customChunk;
        SectionSlots(long key){this.key=key;}
        int get(int local){return (dense==null?sparse.get(local):dense[local])-1;}
        void put(int local,int index){
            if(dense!=null){dense[local]=index+1;return;}
            sparse.put(local,index+1);
            if(sparse.size()==512){
                dense=new int[4096];var entries=sparse.int2IntEntrySet().fastIterator();
                while(entries.hasNext()){var e=entries.next();dense[e.getIntKey()]=e.getIntValue();}
                sparse=null;
            }
        }
    }
    private static long sectionKey(long pos){return SectionPos.asLong(BlockPos.getX(pos)>>4,BlockPos.getY(pos)>>4,BlockPos.getZ(pos)>>4);}
    private static int localKey(long pos){return (int)((pos>>>38)&15)|(((int)pos&15)<<4)|((int)((pos>>>12)&15)<<8);}
    /** One reusable workspace per field; never shared by concurrent exchanges. */
    private static final class Scratch {
        long[] positions=new long[16];
        double[] initialEnergy=new double[16],transfers=new double[16];
        Cell[] cells=new Cell[16];
        boolean[] touched=new boolean[16];
        SectionSlots[] sections=new SectionSlots[16];
        Long2IntOpenCustomHashMap changed;
        int changedSeed;
        void release(){
            if(changed==null&&positions.length==16)return;
            positions=new long[16];initialEnergy=new double[16];transfers=new double[16];
            cells=new Cell[16];touched=new boolean[16];sections=new SectionSlots[16];changed=null;
        }
    }
    private Long2IntOpenCustomHashMap changeOrder(int seed){
        var map=scratch.changed;
        if(map==null||scratch.changedSeed!=seed)map=new Long2IntOpenCustomHashMap(seed,HeatMaps.POSITIONS);
        else {map.clear();if(!map.trim(seed))map=new Long2IntOpenCustomHashMap(seed,HeatMaps.POSITIONS);}
        scratch.changed=map;scratch.changedSeed=seed;return map;
    }
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
        scheduled.refreshChunks();
        int work=0;
        do {
            if(scheduled.copyCursor<scheduled.keys.length)scheduled.copy(64);
            else if(!scheduled.layoutReady)scheduled.buildLayout(64);
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
            if(energy.isEmpty()&&timeline.empty()){timeline.commit(now);scratch.release();return;}
            scheduledTarget=timeline.committed+5;
            if(now<scheduledTarget)return;
            reserveHeadroom();scheduled=new Exchange(level,timeline.through(scheduledTarget));
        }
        long deadline=System.nanoTime()+SOLVER_BUDGET_NS;
        scheduled.refreshChunks(); // Recheck loaded status across yielded server ticks.
        int work=0;
        do{
            int count=Math.min(32,workLimit-work);
            if(count<=0)return;
            if(scheduled.copyCursor<scheduled.copyKeys.length)scheduled.copy(count);
            else if(!scheduled.inputsReady)scheduled.stageInputs(count);
            else if(!scheduled.layoutReady)scheduled.buildLayout(count);
            else if(scheduled.cursor<scheduled.keys.length)scheduled.read(count);
            else if(scheduled.prepare(count)){
                energy=scheduled.result;atmosphereExchange+=scheduled.atmosphereExchange;
                timeline.commit(scheduledTarget);scheduled=null;completedExchanges++;lastExchangeTick=now;setDirty();
            }
            work+=count;
            // A short publication tail must not waste the rest of this tick's
            // allocation while physical time is behind. The same deadline and
            // operation ceiling cover every chained step, never a fresh budget.
            if(scheduled==null){
                if(work>=workLimit||System.nanoTime()>=deadline)return;
                if(energy.isEmpty()&&timeline.empty()){timeline.commit(now);scratch.release();return;}
                scheduledTarget=timeline.committed+5;if(now<scheduledTarget)return;
                reserveHeadroom();scheduled=new Exchange(level,timeline.through(scheduledTarget));scheduled.refreshChunks();
            }
        }while(work<workLimit&&System.nanoTime()<deadline);
    }
    private void diffuse(ServerLevel l,int steps) {
        if(scheduled!=null)throw new IllegalStateException("Cannot synchronously step an active thermal exchange");
        for(int sub=0;sub<steps;sub++){
            reserveHeadroom();
            var exchange=new Exchange(l);exchange.copy(Integer.MAX_VALUE);exchange.buildLayout(Integer.MAX_VALUE);exchange.read(Integer.MAX_VALUE);
            while(!exchange.prepare(Integer.MAX_VALUE)){}
            energy=exchange.result;atmosphereExchange+=exchange.atmosphereExchange;setDirty();
        }
    }
    /** Pending transfers never modify the source field until every pair has been read. */
    private final class Exchange {
        final ServerLevel level;
        final boolean directLevel;
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
        // Coordinate lookup is needed once per neighbor; temperatures, geometry
        // and transfers then use dense slots instead of separate hash maps.
        final Long2ObjectOpenCustomHashMap<SectionSlots> sections=new Long2ObjectOpenCustomHashMap<>(Math.max(16,keys.length/512),HeatMaps.POSITIONS);
        long[] positions=scratch.positions;
        double[] initialEnergy=scratch.initialEnergy,transfers=scratch.transfers;
        Cell[] cells=scratch.cells;
        boolean[] touched=scratch.touched;
        SectionSlots[] sectionBySlot=scratch.sections;
        int size,layoutCursor;
        int slice;
        boolean layoutReady;
        final Long2ByteOpenCustomHashMap chunks=new Long2ByteOpenCustomHashMap(HeatMaps.POSITIONS);
        final IdentityHashMap<BlockState,Material> materials=new IdentityHashMap<>();
        // Retain the old change-key iteration order, including at the field cap.
        final Long2IntOpenCustomHashMap changed=changeOrder(Math.max(16,Math.min(keys.length*7,65536)));
        final Long2IntOpenCustomHashMap heights=new Long2IntOpenCustomHashMap(HeatMaps.POSITIONS);
        final Long2DoubleOpenCustomHashMap rainCooling=new Long2DoubleOpenCustomHashMap(HeatMaps.POSITIONS);
        final double sun;
        double atmosphereExchange;
        it.unimi.dsi.fastutil.objects.ObjectIterator<Long2IntMap.Entry> changes;
        int copyCursor,cursor;
        Exchange(ServerLevel level){this(level,List.of());inputsReady=true;}
        Exchange(ServerLevel level,List<Long2DoubleOpenCustomHashMap> inputs){
            inputFrames=inputs;
            heights.defaultReturnValue(Integer.MIN_VALUE);
            rainCooling.defaultReturnValue(Double.NaN);
            // The climate cache grows with cells actually read, not the whole field up front.
            this.level=level;directLevel=level.getClass()==ServerLevel.class&&!level.isDebug();
            sun=ThermalRules.daylightPhase(level.getDayTime());
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
        void refreshChunks(){chunks.clear();slice++;}
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
        private void grow(int count){
            if(count<=positions.length)return;
            int capacity=Math.max(count,positions.length+positions.length/2);
            positions=Arrays.copyOf(positions,capacity);initialEnergy=Arrays.copyOf(initialEnergy,capacity);
            transfers=Arrays.copyOf(transfers,capacity);cells=Arrays.copyOf(cells,capacity);touched=Arrays.copyOf(touched,capacity);
            sectionBySlot=Arrays.copyOf(sectionBySlot,capacity);
            scratch.positions=positions;scratch.initialEnergy=initialEnergy;scratch.transfers=transfers;
            scratch.cells=cells;scratch.touched=touched;scratch.sections=sectionBySlot;
        }
        private int append(long key){
            grow(size+1);int index=size++;positions[index]=key;
            initialEnergy[index]=0;transfers[index]=0;cells[index]=null;touched[index]=false;
            long sectionKey=sectionKey(key);var section=sections.get(sectionKey);
            if(section==null){section=new SectionSlots(sectionKey);sections.put(sectionKey,section);}
            section.put(localKey(key),index);sectionBySlot[index]=section;return index;
        }
        void buildLayout(int limit){
            if(layoutCursor==0)grow(Math.max(16,keys.length*2));
            int end=(int)Math.min(keys.length,(long)layoutCursor+limit);
            while(layoutCursor<end){long key=keys[layoutCursor++];int index=append(key);initialEnergy[index]=snapshot.get(key);}
            layoutReady=layoutCursor==keys.length;
        }
        private void transfer(int index,double heat){
            if(!touched[index]){touched[index]=true;changed.put(positions[index],index);}
            transfers[index]+=heat;
        }
        Cell cell(int index,BlockPos p){
            long key=p.asLong();var cached=cells[index];if(cached!=null)return cached;
            BlockState state;
            if(!directLevel)state=level.getBlockState(p);
            else{
                var section=sectionBySlot[index];
                if(section.liveSlice!=slice){
                    var chunk=level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);
                    section.customChunk=chunk!=null&&chunk.getClass()!=net.minecraft.world.level.chunk.LevelChunk.class;
                    section.live=chunk==null||section.customChunk?null:chunk.getSection(level.getSectionIndex(p.getY()));section.liveSlice=slice;
                }
                if(section.customChunk)state=level.getBlockState(p);
                else{
                    if(section.live==null)return null;
                    state=section.live.hasOnlyAir()?Blocks.AIR.defaultBlockState():section.live.getBlockState(p.getX()&15,p.getY()&15,p.getZ()&15);
                }
            }
            // Cut cells have position-specific mixed materials. Native slab and
            // door geometry is state-only, so resolve it once per exchange.
            boolean positionDependent=state.getBlock() instanceof CutBlock
                    ||state.getBlock() instanceof SlabBlock&&state.getBlock().getClass()!=SlabBlock.class;
            var material=positionDependent?material(level,p,state)
                    :materials.computeIfAbsent(state,s->material(level,p,s));
            var profile=biomeClimates.get(key);
            if(profile==null){
                var biome=level.getBiome(p);
                var biomeKey=biome.unwrapKey().orElse(null);
                profile=biomeKey==null?ThermalRules.profile(null,biome.value().getBaseTemperature())
                        :profiles.computeIfAbsent(biomeKey,k->ThermalRules.profile(k,biome.value().getBaseTemperature()));
                if(biomeClimates.size()<131072)biomeClimates.put(key,profile);
            }
            long district=RegionalWeather.key(p.getX(),p.getZ());
            double cooling=rainCooling.get(district);
            if(Double.isNaN(cooling)){cooling=RegionalWeather.cooling(level,p);rainCooling.put(district,cooling);}
            double ambient=ThermalRules.climate(profile,p.getY(),sun,cooling);
            long column=net.minecraft.world.level.ChunkPos.asLong(p.getX(),p.getZ());
            int height=heights.get(column);
            if(material.air&&height==Integer.MIN_VALUE){height=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,p.getX(),p.getZ());heights.put(column,height);}
            var next=new Cell(p,material,ambient,material.air&&height<=p.getY());
            cells[index]=next;return next;
        }
        void read(int limit){
            int end=(int)Math.min(keys.length,(long)cursor+limit);
            // Chunk availability may change between batches; never use the cache to force a world read.
            while(cursor<end){
                int index=cursor++;long key=keys[index];var cached=cells[index];
                var p=cached==null?BlockPos.of(key):cached.pos;if(!loaded(level,p,chunks))continue;
                var a=cell(index,p);if(a==null)continue;
                // Lava is an externally maintained reservoir. BlockHeatSources owns
                // its bounded surface exchange; treating its stored anomaly as cold
                // here would create a second, contradictory heat-transfer path.
                if(a.material.lava)continue;
                double ta=a.ambient+initialEnergy[index]/a.material.capacity;
                if(a.outside){double loss=(ta-a.ambient)*a.material.capacity*ThermalRules.OUTDOOR_EXCHANGE;transfer(index,-loss);atmosphereExchange+=loss;}
                if(a.outside&&p.getY()==level.getMaxBuildHeight()-1){
                    double outflow=ThermalRules.EXPOSED_BUOYANT_RISE*Math.max(0,ta-a.ambient);
                    transfer(index,-outflow);atmosphereExchange+=outflow;
                }
                for(var d:NEIGHBORS){
                    long k=BlockPos.offset(key,d);
                    var section=sectionBySlot[index];long target=sectionKey(k);
                    if(target!=section.key)section=sections.get(target);
                    int other=section==null?-1:section.get(localKey(k));boolean exists=other>=0&&other<keys.length;
                    if(exists&&Long.compare(key,k)>0)continue;
                    if(!exists&&Math.abs(ta-a.ambient)<.1)continue;
                    int y=p.getY()+d.getStepY();
                    if(y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight())continue;
                    var neighbor=other<0?null:cells[other];
                    var q=neighbor==null?BlockPos.of(k):neighbor.pos;
                    // The center's chunk was checked above. Interior neighbors
                    // share that loaded chunk; only boundary crossings need a lookup.
                    if(((p.getX()^q.getX())>>4)!=0||((p.getZ()^q.getZ())>>4)!=0)
                        if(!loaded(level,q,chunks))continue;
                    if(other<0)other=append(k);
                    var b=cell(other,q);if(b==null)continue;
                    double tb=b.ambient+initialEnergy[other]/b.material.capacity;
                    if(b.material.lava)continue;
                    double openness=Integer.bitCount(a.material.open[d.ordinal()]&b.material.open[d.getOpposite().ordinal()])/4d;
                    double rate;
                    if(openness==1)rate=ThermalRules.exchangeRate(true,true,d,ta,tb,1,1);
                    else{
                        double base=ThermalRules.exchangeRate(a.material.air,b.material.air,d,ta,tb,a.material.conductance,b.material.conductance);
                        rate=openness==0?base:base*(1-openness)+ThermalRules.exchangeRate(true,true,d,ta,tb,1,1)*openness;
                    }
                    // Sky-exposed air reserves more of its stable exchange budget
                    // for upward convection instead of spreading equally sideways.
                    if(d.getAxis().isHorizontal()&&a.material.air&&b.material.air&&(a.outside||b.outside))
                        rate=ThermalRules.EXPOSED_AIR_MIXING;
                    double flow=(ta-tb)*rate+openness*ThermalRules.buoyantFlux(a.material.air,b.material.air,a.outside,b.outside,
                            d,ta,tb,a.ambient,b.ambient);
                    transfer(index,-flow);transfer(other,flow);
                }
            }
        }
        boolean prepare(int limit){
            if(cursor!=keys.length)throw new IllegalStateException("Incomplete thermal exchange");
            if(changes==null)changes=changed.long2IntEntrySet().fastIterator();
            int work=0;
            while(changes.hasNext()&&work++<limit){
                var e=changes.next();long key=e.getLongKey();int index=e.getIntValue();var c=cells[index];
                double value=initialEnergy[index]+transfers[index];
                if(Math.abs(value)<.0001 || c.outside&&Math.abs(value)/c.material.capacity<.05){
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
            if(!incoming.isEmpty())return false;
            for(var section:sections.values())section.live=null;
            return true;
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
