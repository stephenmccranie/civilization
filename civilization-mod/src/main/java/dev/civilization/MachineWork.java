package dev.civilization;

import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A small server/client-shared recipe quotation. It never moves or creates inventory items. */
public final class MachineWork {
    public record Match(WorkshopJobs.Job job,int[] consumed,ItemStack first) {}
    private MachineWork(){}
    public static ItemStack target(Container c){for(int i=0;i<4;i++)if(WorkshopJobs.gated(c.getItem(i))&&c.getItem(i).isDamaged())return c.getItem(i);return ItemStack.EMPTY;}
    /** The jobs a selection may run. The catalog's Repair entry stands for repairing the loaded damaged equipment, if any. */
    static List<WorkshopJobs.Job> candidates(Container c,int kind,int selection){
        return WorkshopJobs.jobs(kind,selection).stream().map(j->j.output().isEmpty()?WorkshopJobs.repair(target(c)):j).filter(Objects::nonNull).toList();
    }
    /**
     * The job a batch runs: the one candidate the loaded materials satisfy. Several matches wait for a narrower selection.
     * With none, a selection or a single-job workshop still names its first job, whose requirements the ghosts show.
     */
    public static WorkshopJobs.Job job(Container c,int kind,int selection){
        var candidates=candidates(c,kind,selection);
        var matched=candidates.stream().filter(j->match(c,j)!=null).toList();
        if(matched.size()==1)return matched.getFirst();
        return matched.isEmpty()&&!candidates.isEmpty()&&(selection>=0||candidates.size()==1)?candidates.getFirst():null;
    }
    public static List<Match> matches(Container c,int kind,int selection){return candidates(c,kind,selection).stream().map(j->match(c,j)).filter(Objects::nonNull).toList();}
    public static Match match(Container c,WorkshopJobs.Job job){
        if(job==null||job.needs().size()!=4)return null;
        int[] taken=new int[4];ItemStack first=ItemStack.EMPTY;
        for(int n=0;n<4;n++){
            var need=job.needs().get(n);int left=need.count();
            for(int i=0;i<4&&left>0;i++)if(need.ingredient().test(c.getItem(i))){
                if(n==0&&job.name().equals("Repair")&&c.getItem(i)!=target(c))continue;
                int amount=Math.min(left,c.getItem(i).getCount()-taken[i]);
                if(amount>0){taken[i]+=amount;left-=amount;if(n==0&&first.isEmpty())first=c.getItem(i);}
            }
            if(left>0)return null;
        }
        // Unrelated material never silently participates in a different automatic job.
        for(int i=0;i<4;i++){var stack=c.getItem(i);if(!stack.isEmpty()&&taken[i]==0&&job.needs().stream().noneMatch(n->n.count()>0&&n.ingredient().test(stack)))return null;}
        return new Match(job,taken,first);
    }
    /** Inputs any candidate job uses; on the Assembly Bench's Automatic or Repair, also damaged equipment to repair. */
    public static boolean accepts(Container c,int kind,int selection,ItemStack s){
        if(kind==ManufacturingJobs.BENCH&&WorkshopJobs.gated(s)&&s.isDamaged()&&selection<=0)return true;
        return candidates(c,kind,selection).stream().anyMatch(j->j.needs().stream().anyMatch(n->n.count()>0&&n.ingredient().test(s)));
    }
}
