package dev.civilization.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;

/** Shared cabinet and contextual recipe catalog. Processing remains entirely server-owned. */
public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected record RecipeEntry(int id,Component name,ItemStack icon,String category,Item material) {}
    private EditBox search;
    private final List<RecipeWidget> rows=new ArrayList<>(),categories=new ArrayList<>(),materials=new ArrayList<>();
    private final List<String> categoryKeys=new ArrayList<>();
    private final List<Item> materialKeys=new ArrayList<>();
    private List<RecipeEntry> catalog=List.of();
    private RecipeWidget automatic,repair,toggle;
    private boolean drawer=true;
    private int offset,drawerWidth,drawerX,listY,rowCount,catalogKey;
    private String category="All";
    private Item material=Items.AIR;
    protected int guide=-1;
    protected MachineScreen(M menu,Inventory inv,Component title){super(menu,inv,title);}
    protected List<RecipeEntry> recipes(){return List.of();}
    protected int recipeCatalogKey(){return 0;}
    protected int chosen(){return guide;}
    /** External overlays must leave the recipe drawer and side tab unobstructed. */
    public net.minecraft.client.renderer.Rect2i recipeDrawerBounds(){
        int x=drawer&&search!=null?drawerX-24:leftPos-24;
        return new net.minecraft.client.renderer.Rect2i(x,topPos,leftPos-x,imageHeight);
    }
    protected void choose(int id){guide=id;}
    private RecipeWidget button(int x,int y,int w,int h,boolean row,String label,ItemStack icon,Runnable action){
        var b=addRenderableWidget(new RecipeWidget(x,y,w,h,row,Component.literal(label),action));b.icon=icon;
        b.setTooltip(Tooltip.create(Component.literal(label)));return b;
    }
    @Override protected void init(){
        super.init();repair=null;catalogKey=recipeCatalogKey();rows.clear();categories.clear();materials.clear();categoryKeys.clear();materialKeys.clear();
        catalog=List.copyOf(recipes());if(catalog.isEmpty()){ignition();return;}
        drawerWidth=Math.min(144,width-imageWidth-28);
        drawer=drawer&&drawerWidth>=108;
        drawerWidth=Math.max(108,drawerWidth);
        if(width>=imageWidth+drawerWidth+24)leftPos=Math.max(leftPos,drawerWidth+24);
        drawerX=leftPos-drawerWidth;String query=search==null?"":search.getValue();
        toggle=button(leftPos-22,topPos+4,20,20,false,"Recipes",new ItemStack(Items.BOOK),()->{drawer=!drawer;updateDrawer();});
        toggle.dark=true;toggle.active=width>=imageWidth+drawerWidth+24;
        search=addRenderableWidget(new EditBox(font,drawerX+27,topPos+16,drawerWidth-39,12,Component.literal("Search recipes")));
        search.setBordered(false);search.setTextColor(0xffb9b6aa);search.setMaxLength(64);search.setHint(Component.literal("Search...").withStyle(style->style.withColor(0x85877e)));search.setValue(query);
        search.setResponder(s->{offset=0;updateDrawer();});
        for(String key:List.of("Tools","Weapons","Armor","Components"))if(catalog.stream().anyMatch(r->r.category().equals(key)))categoryKeys.add(key);
        if(categoryKeys.size()>1){categoryKeys.addFirst("All");for(int i=0;i<categoryKeys.size();i++){
            String key=categoryKeys.get(i);Item icon=switch(key){case "Tools"->Items.IRON_PICKAXE;case "Weapons"->Items.IRON_SWORD;case "Armor"->Items.IRON_CHESTPLATE;case "Components"->dev.civilization.KilnContent.MACHINE_PARTS.get();default->Items.AIR;};
            var b=button(drawerX-23,topPos+28+i*24,22,22,false,key,new ItemStack(icon),()->{category=key;offset=0;updateDrawer();});b.dark=true;if(icon==Items.AIR)b.symbol="all";categories.add(b);
        }}else categoryKeys.clear();
        for(Item key:List.of(Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.NETHERITE_INGOT))if(catalog.stream().anyMatch(r->r.material()==key))materialKeys.add(key);
        if(materialKeys.size()>1){materialKeys.addFirst(Items.AIR);for(int i=0;i<materialKeys.size();i++){
            Item key=materialKeys.get(i);var b=button(drawerX+9+i*20,topPos+35,18,18,false,materialName(key),new ItemStack(key),()->{material=key;offset=0;updateDrawer();});if(key==Items.AIR)b.symbol="all";materials.add(b);
        }}else materialKeys.clear();
        if(categoryKeys.isEmpty())category="All";if(materialKeys.isEmpty())material=Items.AIR;
        listY=topPos+(materials.isEmpty()?48:70);rowCount=(topPos+imageHeight-34-listY)/18;
        for(int i=0;i<rowCount;i++){final int row=i;rows.add(button(drawerX+9,listY+i*18,drawerWidth-22,18,true,"",ItemStack.EMPTY,()->{var matches=filtered();if(offset+row<matches.size())choose(matches.get(offset+row).id());}));}
        automatic=button(drawerX+9,topPos+imageHeight-28,20,20,false,"Automatic",ItemStack.EMPTY,()->choose(-1));automatic.symbol="auto";
        catalog.stream().filter(r->r.category().equals("Repair")).findFirst().ifPresent(r->repair=button(drawerX+33,topPos+imageHeight-28,20,20,false,"Repair",new ItemStack(Items.ANVIL),()->choose(r.id())));
        updateDrawer();
        ignition();
    }
    private void ignition(){if(menu instanceof dev.civilization.CoalFireMenu fire)addRenderableWidget(new IgnitionButton(leftPos+132,topPos+84,menu,fire));}
    private String materialName(Item item){return item==Items.AIR?"All materials":item==Items.IRON_INGOT?"Iron":item==Items.GOLD_INGOT?"Gold":item==Items.DIAMOND?"Diamond":"Netherite";}
    private List<RecipeEntry> filtered(){String term=search==null?"":search.getValue().toLowerCase(Locale.ROOT);return catalog.stream().filter(r->!r.category().equals("Repair")&&(category.equals("All")||category.equals(r.category()))&&(material==Items.AIR||material==r.material())&&r.name().getString().toLowerCase(Locale.ROOT).contains(term)).toList();}
    private void updateDrawer(){
        if(search==null)return;var list=filtered();offset=Math.clamp(offset,0,Math.max(0,list.size()-rowCount));
        search.visible=automatic.visible=drawer;automatic.selected=chosen()==-1;if(!drawer)search.setFocused(false);
        if(repair!=null){repair.visible=drawer;repair.selected=catalog.stream().anyMatch(r->r.category().equals("Repair")&&r.id()==chosen());}
        for(int i=0;i<categories.size();i++){var b=categories.get(i);b.visible=drawer;b.selected=categoryKeys.get(i).equals(category);}
        for(int i=0;i<materials.size();i++){var b=materials.get(i);b.visible=drawer;b.selected=materialKeys.get(i)==material;}
        for(int i=0;i<rows.size();i++){var b=rows.get(i);int index=offset+i;b.visible=drawer&&index<list.size();if(b.visible){var r=list.get(index);b.setMessage(r.name());b.icon=r.icon();b.setTooltip(Tooltip.create(r.name()));b.selected=chosen()==r.id();}}
        toggle.setX(drawer?drawerX-23:leftPos-22);toggle.icon=drawer?ItemStack.EMPTY:new ItemStack(Items.BOOK);toggle.symbol=drawer?"close":"";
    }
    @Override protected void containerTick(){super.containerTick();for(var child:children())if(child instanceof IgnitionButton b&&menu instanceof dev.civilization.CoalFireMenu fire)b.visible=fire.hasCoalFire();if(catalogKey!=recipeCatalogKey())rebuildWidgets();updateDrawer();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        if(drawer&&search!=null){
            MachineUi.panel(g,drawerX,topPos,drawerWidth-6,imageHeight);
            g.fill(drawerX+9,topPos+11,leftPos-9,topPos+30,0xffd0c9b6);
            g.fill(drawerX+9,topPos+11,leftPos-10,topPos+29,0xff55534b);
            g.fill(drawerX+10,topPos+12,leftPos-10,topPos+29,0xff34352f);
            g.fill(drawerX+23,topPos+13,drawerX+24,topPos+28,0xff55564f);
            String[] lens={"0011110000","0110011000","1100001100","1100001100","0110011000","0011110000","0000011000","0000001100","0000000110","0000000011"};
            for(int j=0;j<10;j++)for(int i=0;i<10;i++)if(lens[j].charAt(i)=='1')g.fill(drawerX+12+i,topPos+15+j,drawerX+13+i,topPos+16+j,0xffcccbbc);
            String label=category.equals("All")?"Recipes":category;if(material!=Items.AIR)label+=" / "+materialName(material);
            g.drawString(font,font.plainSubstrByWidth(label,drawerWidth-18),drawerX+9,listY-12,MachineUi.MUTED,false);
            var list=filtered();if(list.isEmpty())g.drawString(font,"No recipes",drawerX+10,listY+5,MachineUi.MUTED,false);
            if(list.size()>rowCount){int h=rowCount*18,x=leftPos-10;g.fill(x,listY,x+3,listY+h,0xff6b665a);int thumb=Math.max(8,h*rowCount/list.size()),y=listY+(h-thumb)*offset/(list.size()-rowCount);g.fill(x,y,x+3,y+thumb,0xffd2b77e);}
            g.drawString(font,list.size()+(drawerWidth<128?"":" recipes"),drawerX+61,topPos+imageHeight-21,MachineUi.MUTED,false);
        }
        MachineUi.panel(g,leftPos,topPos,imageWidth,imageHeight);MachineUi.title(g,title.getString(),leftPos,topPos,imageWidth);MachineUi.slots(g,menu,leftPos,topPos);drawMachine(g,partial,mx,my);
        if(menu instanceof dev.civilization.CoalFireMenu fire&&fire.hasCoalFire()){
            int x=leftPos+116,y=topPos+85,h=Math.round(21*fire.coalRemaining()/1000f);
            g.fill(x,y,x+6,y+23,0xff37352f);g.fill(x+1,y+1,x+5,y+22,0xff191919);
            if(h>0){g.fill(x+1,y+22-h,x+5,y+22,0xffe99a28);g.fill(x+1,y+22-h,x+2,y+22,0xffffdf73);}
        }
    }
    protected abstract void drawMachine(GuiGraphics g,float partial,int mx,int my);
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,MachineUi.INK,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float dt){super.render(g,mx,my,dt);renderTooltip(g,mx,my);if(menu instanceof dev.civilization.CoalFireMenu fire&&fire.hasCoalFire()&&isHovering(115,84,8,25,mx,my))g.renderTooltip(font,Component.literal("Burning coal: "+Math.round(fire.coalRemaining()/10f)+"% remaining"),mx,my);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(drawer&&x>=drawerX&&x<leftPos&&y>=listY&&y<listY+rowCount*18){offset-= (int)Math.signum(vertical);updateDrawer();return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(search!=null&&search.visible&&search.isFocused()&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE&&key!=org.lwjgl.glfw.GLFW.GLFW_KEY_TAB){search.keyPressed(key,scan,mods);return true;}
        if(drawer&&search!=null&&(key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN||key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP)){offset+=key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN?rowCount:-rowCount;updateDrawer();return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button){if(search!=null&&x>=left-(drawer?drawerWidth+24:22)&&x<left&&y>=top&&y<top+(drawer?imageHeight:25))return false;return super.hasClickedOutside(x,y,left,top,button);}
}

