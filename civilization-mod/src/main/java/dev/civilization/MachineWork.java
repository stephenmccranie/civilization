package dev.civilization;

import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A small server/client-shared recipe quotation. It never moves or creates inventory items. */
public final class MachineWork {
    public record Match(WorkshopJobs.Job job,int[] consumed,ItemStack first) {}
    private MachineWork(){}
    public static ItemStack target(Container c){for(int i=0;i<4;i++)if(WorkshopJobs.gated(c.getItem(i))&&c.getItem(i).isDamaged())return c.getItem(i);return ItemStack.EMPTY;}
    public static WorkshopJobs.Job job(Container c,int kind,int selection){
        if(selection<0){var matches=matches(c,kind);return matches.size()==1?matches.getFirst().job():kind<2?WorkshopJobs.jobs(kind).getFirst():null;}
        if(selection>=WorkshopJobs.jobs(kind).size())return null;
        return kind==ManufacturingJobs.BENCH&&selection==0?WorkshopJobs.repair(target(c)):WorkshopJobs.jobs(kind).get(selection);
    }
    public static List<Match> matches(Container c,int kind){
        var result=new ArrayList<Match>();var jobs=WorkshopJobs.jobs(kind);
        for(int i=0;i<jobs.size();i++){var job=kind==ManufacturingJobs.BENCH&&i==0?WorkshopJobs.repair(target(c)):jobs.get(i);var match=match(c,job);if(match!=null)result.add(match);}
        return result;
    }
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
    public static boolean accepts(Container c,int kind,int selection,ItemStack s){
        if(kind==ManufacturingJobs.BENCH&&WorkshopJobs.gated(s)&&s.isDamaged()&&(selection<=0))return true;
        if(selection>=0){var job=job(c,kind,selection);return job!=null&&job.needs().stream().anyMatch(n->n.count()>0&&n.ingredient().test(s));}
        return WorkshopJobs.jobs(kind).stream().anyMatch(j->j.needs().stream().anyMatch(n->n.count()>0&&n.ingredient().test(s)))
            ||kind==ManufacturingJobs.BENCH&&(s.is(net.minecraft.world.item.Items.LEATHER)||s.is(WorkshopContent.CLOTH.get()));
    }
}
