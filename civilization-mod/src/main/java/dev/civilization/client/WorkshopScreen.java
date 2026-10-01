package dev.civilization.client;
import java.util.*;
import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class WorkshopScreen extends MachineScreen<WorkshopMenu> {
    public WorkshopScreen(WorkshopMenu m,Inventory i,Component title){super(m,i,title);imageHeight=214;inventoryLabelY=120;}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(WorkshopContent.MENU.get(),WorkshopScreen::new);}
    @Override protected int recipeCatalogKey(){return menu.kind();}
    @Override protected List<RecipeEntry> recipes(){
        var list=new ArrayList<RecipeEntry>();var jobs=WorkshopJobs.jobs(menu.kind());
        for(int i=0;i<jobs.size();i++){
            var job=jobs.get(i);var output=job.output();var equipment=WorkshopJobs.equipment(output);
            String category=output.isEmpty()?"Repair":equipment==null?"Components":equipment.armor()||output.is(net.minecraft.world.item.Items.SHIELD)?"Armor":output.getItem() instanceof net.minecraft.world.item.SwordItem?"Weapons":"Tools";
            var name=Component.translatable(job.name());
            if(equipment!=null&&equipment.material()==net.minecraft.world.item.Items.NETHERITE_INGOT)name.append(" (upgrade)");
            list.add(new RecipeEntry(i,name,output,category,equipment==null?net.minecraft.world.item.Items.AIR:equipment.material()));
        }return list;
    }
    @Override protected int chosen(){return menu.selection();}
    @Override protected void choose(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private ItemStack ghost(int slot){
        if(slot==4||slot==6)return KilnContent.MINERAL_COAL.toStack();
        var job=menu.job();if(job==null)return ItemStack.EMPTY;
        if(slot==5||slot==7)return WorkshopJobs.result(job,MachineWork.first(menu.inventory,job));
        var missing=new ArrayList<ItemStack>();
        for(var need:job.needs())if(need.count()>0){int count=0;for(int i=0;i<4;i++)if(need.ingredient().test(menu.inventory.getItem(i)))count+=menu.inventory.getItem(i).getCount();if(count<need.count())missing.add(need.icon().copyWithCount(need.count()-count));}
        int empty=0;for(int i=0;i<slot;i++)if(menu.inventory.getItem(i).isEmpty())empty++;
        return empty<missing.size()?missing.get(empty):ItemStack.EMPTY;
    }
    @Override protected void drawMachine(GuiGraphics g,float dt,int mx,int my){
        var job=menu.job();
        g.drawString(font,"Materials",leftPos+17,topPos+29,MachineUi.INK,false);g.drawString(font,"Output",leftPos+130,topPos+29,MachineUi.INK,false);
        for(int i=0;i<8;i++){var s=menu.getSlot(i);if(s.isActive()&&!s.hasItem())MachineUi.ghost(g,ghost(i),leftPos+s.x,topPos+s.y);}
        MachineUi.progress(g,leftPos+94,topPos+49,23,job==null?0:menu.data.get(3)/(float)job.ticks());
        String state=switch(menu.data.get(4)){case MachineStatus.Workshop.INCOMPLETE->"Finish the structure";case MachineStatus.Workshop.NEEDS_MATERIALS->menu.kind()==2&&menu.selection()==0&&job==null?"Add damaged equipment":"Add materials";case MachineStatus.Workshop.OUTPUT_FULL->"Collect the output";case MachineStatus.Workshop.FIRE_UNLIT->(menu.fireState()&1)!=0?"Fire lit":"Add Coal and strike to light";case MachineStatus.Workshop.AMBIGUOUS->"Choose a matching recipe";case MachineStatus.Workshop.WET->"Shelter the work face from rain";default->"Working";};
        g.drawString(font,state,leftPos+9,topPos+64,MachineUi.MUTED,false);
        g.drawString(font,"Coal",leftPos+17,topPos+76,MachineUi.INK,false);
        MachineUi.fire(g,leftPos+66,topPos+85,44,23,(menu.fireState()&1)!=0);
        if(job!=null&&job.name().equals("Repair"))g.drawString(font,"After repair: "+job.output().getMaxDamage()+" durability",leftPos+9,topPos+110,MachineUi.MUTED,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float dt){super.render(g,x,y,dt);
        for(int i=0;i<8;i++){var s=menu.getSlot(i);if(s.isActive()&&!s.hasItem()&&isHovering(s.x,s.y,16,16,x,y)){var ghost=ghost(i);g.renderTooltip(font,ghost.isEmpty()?Component.literal((i==5||i==7)?"Output":"Materials — any input slot"):Component.literal(ghost.getCount()+" × ").append(ghost.getHoverName()),x,y);}}
    }
}
