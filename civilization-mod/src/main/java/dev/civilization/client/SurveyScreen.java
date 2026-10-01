package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SurveyScreen extends AbstractContainerScreen<SurveyMenu> {
    private boolean claims=true,counters=true;
    private SurveyPayload.Marker selected;
    private CivicWidget claimButton,counterButton;
    public SurveyScreen(SurveyMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=416;imageHeight=304;}
    private CivicWidget button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(new CivicWidget(label,leftPos+x,topPos+y,w,20,action));}
    @Override protected void init(){
        super.init();
        claimButton=button("Claims",276,190,62,()->claims=!claims);counterButton=button("Counters",342,190,66,()->counters=!counters);

    }
    @Override protected void containerTick(){claimButton.selected=claims;counterButton.selected=counters;}
    private int px(int x){var s=menu.snapshot;return leftPos+8+(int)Math.floor((x-(s.centerX()-128.0*s.zoom()))/s.zoom());}
    private int pz(int z){var s=menu.snapshot;return topPos+26+(int)Math.floor((z-(s.centerZ()-128.0*s.zoom()))/s.zoom());}
    private SurveyPayload.Marker hit(double x,double y){return hit(x,y,false);}
    private SurveyPayload.Marker hit(double x,double y,boolean cycle){
        if(menu.snapshot==null||x<leftPos+8||x>=leftPos+264||y<topPos+26||y>=topPos+282)return null;
        var matches=new java.util.ArrayList<SurveyPayload.Marker>();
        for(var m:menu.snapshot.markers()){
            if(m.claim()&&!claims||!m.claim()&&!counters)continue;
            int reach=m.claim()?Math.max(3,m.radius()/menu.snapshot.zoom()):5;
            if(Math.abs(x-px(m.x()))<=reach&&Math.abs(y-pz(m.z()))<=reach){matches.add(m);}
        }matches.sort(java.util.Comparator.comparing(SurveyPayload.Marker::claim));
        if(matches.isEmpty())return null;int current=matches.indexOf(selected);return matches.get(cycle&&current>=0?(current+1)%matches.size():0);
    }
    @Override public boolean mouseClicked(double x,double y,int button){var m=hit(x,y,true);if(m!=null&&button==0){selected=m;return true;}return super.mouseClicked(x,y,button);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        CivicWidget.panel(g,leftPos,topPos,imageWidth,imageHeight);
        g.fill(leftPos+6,topPos+24,leftPos+266,topPos+284,0xFF4B4435);g.fill(leftPos+8,topPos+26,leftPos+264,topPos+282,0xFFAC9B78);
        var s=menu.snapshot;if(s==null)return;
        for(int z=0;z<64;z++)for(int x=0;x<64;x++){int color=s.terrain()[z*64+x];if(color!=0)g.fill(leftPos+8+x*4,topPos+26+z*4,leftPos+12+x*4,topPos+30+z*4,color);}
        g.enableScissor(leftPos+8,topPos+26,leftPos+264,topPos+282);
        for(var m:s.markers()){
            int x=px(m.x()),z=pz(m.z());
            if(m.claim()&&claims){int r=Math.max(1,m.radius()/s.zoom());g.renderOutline(x-r,z-r,r*2,r*2,0xFFFFDA64);}
            else if(!m.claim()&&counters){g.fill(x-3,z-3,x+4,z+4,0xFF182C35);g.fill(x-2,z-2,x+3,z+3,0xFF6ED6EE);}
        }
        int x=px(s.playerX()),z=pz(s.playerZ());g.fill(x-2,z-2,x+3,z+3,0xFF222222);g.fill(x-1,z-1,x+2,z+2,0xFFFFFFFF);
        int firstX=Math.floorDiv(s.centerX()-128*s.zoom(),512)*512,firstZ=Math.floorDiv(s.centerZ()-128*s.zoom(),512)*512;
        for(int boundary=firstX;boundary<s.centerX()+128*s.zoom();boundary+=512)g.fill(px(boundary),topPos+26,px(boundary)+1,topPos+282,0x887aaabb);
        for(int boundary=firstZ;boundary<s.centerZ()+128*s.zoom();boundary+=512)g.fill(leftPos+8,pz(boundary),leftPos+264,pz(boundary)+1,0x887aaabb);
        g.disableScissor();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);
        MachineUi.title(g,"Survey Table",leftPos,topPos,104);
        g.drawString(font,"Gold: claims   Blue: counters   White: you",leftPos+112,topPos+10,0xFF454545,false);
        var s=menu.snapshot;
        if(s==null){g.drawString(font,"Surveying…",leftPos+278,topPos+28,0xFF303030,false);return;}
        g.drawString(font,(256*s.zoom())+" blocks across · North ↑",leftPos+8,topPos+290,0xFF404040,false);
        if(selected!=null) selected=s.markers().stream().filter(m->m.claim()==selected.claim()&&m.x()==selected.x()&&m.y()==selected.y()&&m.z()==selected.z()).findFirst().orElse(null);
        String text=selected==null?"Select a claim or counter.\n\nNearby terrain is surveyed automatically.\n\nThe map is centered on this table.":selected.title()+"\n"+selected.x()+", "+selected.y()+", "+selected.z()+"\n\n"+selected.details();
        g.enableScissor(leftPos+276,topPos+26,leftPos+408,topPos+176);
        g.drawWordWrap(font,Component.literal(text),leftPos+277,topPos+28,128,0xFF303030);
        g.disableScissor();
        if(s.truncated())g.drawString(font,"Marker limit reached",leftPos+277,topPos+178,0xFF873525,false);
        if(dev.civilization.RegionalWeather.enabled(minecraft.level)){var at=new net.minecraft.core.BlockPos(s.centerX(),64,s.centerZ());g.drawString(font,"Weather district "+Math.floorDiv(s.centerX(),512)+", "+Math.floorDiv(s.centerZ(),512),leftPos+278,topPos+224,0xff404040,false);g.drawString(font,LocalWeather.raining(minecraft.level,at)?"Raining":"Clear",leftPos+278,topPos+237,0xff404040,false);}
        var hover=hit(mx,my);if(hover!=null)g.renderTooltip(font,Component.literal(hover.title()),mx,my);
    }
}
