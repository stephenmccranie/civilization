package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-thread authoritative escrow. Refunds and winnings survive the controller. */
public final class ArenaData extends SavedData {
    public static final int LOBBY=0, COUNTDOWN=1, LIVE=2, RESULT=3;
    public static final class Bet {
        public int id,side;
        public UUID owner,opponent;
        public ItemStack stake=ItemStack.EMPTY;
    }
    public static final class Pit {
        public CivicData.Address at;
        public UUID owner;
        public Direction front=Direction.NORTH;
        public final UUID[] fighters=new UUID[2];
        public final ItemStack[] stakes={ItemStack.EMPTY,ItemStack.EMPTY};
        public final boolean[] accepted=new boolean[2],ready=new boolean[2];
        public final float[] health=new float[2];
        public final List<Bet> bets=new ArrayList<>();
        public int phase,revision,nextBet=1,winner=-1;
        public long deadline;
        public String notice="Join a fighter slot or watch a match";
        public int side(UUID player){for(int i=0;i<2;i++)if(player.equals(fighters[i]))return i;return -1;}
        public boolean paired(){return fighters[0]!=null&&fighters[1]!=null;}
        public void changed(){revision=(revision+1)&32767;}
    }
    public final Map<CivicData.Address,Pit> pits=new LinkedHashMap<>();
    public final Map<UUID,List<ItemStack>> credits=new LinkedHashMap<>();
    public final Map<UUID,Float> recovery=new LinkedHashMap<>();
    public static ArenaData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ArenaData::new,ArenaData::load),"civilization_arenas");}
    public void credit(UUID player,ItemStack item){if(player!=null&&!item.isEmpty())credits.computeIfAbsent(player,k->new ArrayList<>()).add(item.copy());setDirty();}
    public void settle(Pit p,int winner,String notice){
        if(p.phase==RESULT)return;
        for(int i=0;i<2;i++){
            credit(winner<0?p.fighters[i]:p.fighters[winner],p.stakes[i]);p.stakes[i]=ItemStack.EMPTY;
            if(p.ready[i]&&p.fighters[i]!=null)recovery.put(p.fighters[i],p.health[i]);
        }
        for(var bet:p.bets){
            if(winner<0||bet.opponent==null){credit(bet.owner,bet.stake);if(bet.opponent!=null)credit(bet.opponent,bet.stake);}
            else {var recipient=winner==bet.side?bet.owner:bet.opponent;credit(recipient,bet.stake);credit(recipient,bet.stake);}
        }
        p.bets.clear();p.phase=RESULT;p.winner=winner;p.notice=notice;p.deadline=0;p.changed();setDirty();
    }
    public void reset(Pit p){
        for(int i=0;i<2;i++){p.fighters[i]=null;p.accepted[i]=false;p.ready[i]=false;p.health[i]=0;}
        p.phase=LOBBY;p.deadline=0;p.changed();setDirty();
    }
    public static ArenaData load(CompoundTag root,HolderLookup.Provider provider){
        var data=new ArenaData();
        for(var entry:root.getList("credits",Tag.TAG_COMPOUND)){var t=(CompoundTag)entry;var list=new ArrayList<ItemStack>();for(var item:t.getList("items",Tag.TAG_COMPOUND)){var stack=ItemStack.parseOptional(provider,(CompoundTag)item);if(!stack.isEmpty())list.add(stack);}if(!list.isEmpty())data.credits.put(t.getUUID("player"),list);}
        for(var entry:root.getList("recovery",Tag.TAG_COMPOUND)){var t=(CompoundTag)entry;data.recovery.put(t.getUUID("player"),t.getFloat("health"));}
        for(var entry:root.getList("pits",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)entry;var p=new Pit();p.at=CivicData.Address.load(t.getCompound("at"));p.owner=t.getUUID("owner");p.front=Direction.from2DDataValue(t.getInt("front"));p.revision=t.getInt("revision");p.nextBet=Math.max(1,t.getInt("nextBet"));p.phase=t.getInt("phase");p.winner=t.getInt("winner");p.notice=t.getString("notice");
            for(int i=0;i<2;i++){String key="fighter"+i;if(t.hasUUID(key))p.fighters[i]=t.getUUID(key);p.stakes[i]=ItemStack.parseOptional(provider,t.getCompound("stake"+i));p.ready[i]=t.getBoolean("ready"+i);p.health[i]=t.getFloat("health"+i);}
            for(var betTag:t.getList("bets",Tag.TAG_COMPOUND)){var b=(CompoundTag)betTag;var bet=new Bet();bet.id=b.getInt("id");bet.side=b.getInt("side");bet.owner=b.getUUID("owner");if(b.hasUUID("opponent"))bet.opponent=b.getUUID("opponent");bet.stake=ItemStack.parseOptional(provider,b.getCompound("stake"));p.bets.add(bet);}
            // No unfinished match or accepted terms silently resumes after restart.
            if(p.phase!=RESULT)data.settle(p,-1,"Restart: unfinished match refunded");
            data.reset(p);data.pits.put(p.at,p);
        }
        data.setDirty();return data;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider provider){
        var cs=new ListTag();for(var e:credits.entrySet()){var t=new CompoundTag();t.putUUID("player",e.getKey());var items=new ListTag();for(var s:e.getValue())if(!s.isEmpty())items.add(s.save(provider));t.put("items",items);cs.add(t);}root.put("credits",cs);
        var rs=new ListTag();for(var e:recovery.entrySet()){var t=new CompoundTag();t.putUUID("player",e.getKey());t.putFloat("health",e.getValue());rs.add(t);}root.put("recovery",rs);
        var ps=new ListTag();for(var p:pits.values()){
            var t=new CompoundTag();t.put("at",p.at.save());t.putUUID("owner",p.owner);t.putInt("front",p.front.get2DDataValue());t.putInt("revision",p.revision);t.putInt("nextBet",p.nextBet);t.putInt("phase",p.phase);t.putInt("winner",p.winner);t.putString("notice",p.notice);
            for(int i=0;i<2;i++){if(p.fighters[i]!=null)t.putUUID("fighter"+i,p.fighters[i]);if(!p.stakes[i].isEmpty())t.put("stake"+i,p.stakes[i].save(provider));t.putBoolean("ready"+i,p.ready[i]);t.putFloat("health"+i,p.health[i]);}
            var bs=new ListTag();for(var bet:p.bets){var b=new CompoundTag();b.putInt("id",bet.id);b.putInt("side",bet.side);b.putUUID("owner",bet.owner);if(bet.opponent!=null)b.putUUID("opponent",bet.opponent);b.put("stake",bet.stake.save(provider));bs.add(b);}t.put("bets",bs);ps.add(t);
        }root.put("pits",ps);return root;
    }
}
