package dev.civilization.client;

import com.mojang.blaze3d.vertex.*;
import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;

@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class PatersonRenderer extends GeoItemRenderer<PatersonItem> {
    public double lastHeldHammerAngle,lastHeldTriggerAngle;
    public PatersonRenderer(){super(new MachineGeoModel<>("colt_paterson"));}
    @SubscribeEvent public static void register(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private PatersonRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new PatersonRenderer();return renderer;}},FirearmContent.PATERSON.get());}
    @Override public void preRender(PoseStack p,PatersonItem a,software.bernie.geckolib.cache.object.BakedGeoModel m,MultiBufferSource b,VertexConsumer v,boolean rerender,float partial,int light,int overlay,int color){
        super.preRender(p,a,m,b,v,rerender,partial,light,overlay,color);
        var mc=Minecraft.getInstance();var tag=PatersonItem.state(getCurrentItemStack());double age=mc.level==null||!tag.contains("shotTick")?100:mc.level.getGameTime()+partial-tag.getLong("shotTick");
        if(renderPerspective.firstPerson()&&mc.player!=null){double kick=Math.clamp((8-age)/8,0,1);if(age>=0)p.translate(0,kick*.025,kick*.07);}
    }
    @Override public void renderRecursively(PoseStack p,PatersonItem a,software.bernie.geckolib.cache.object.GeoBone bone,RenderType type,MultiBufferSource b,VertexConsumer v,boolean rerender,float partial,int light,int overlay,int color){
        // GeckoLib resets animation transforms after preRender; apply state just before geometry is drawn.
        if(bone.getName().equals("barrel"))applyPose(partial);
        super.renderRecursively(p,a,bone,type,b,v,rerender,partial,light,overlay,color);
    }
    private void applyPose(float partial){
        var s=getCurrentItemStack();var tag=PatersonItem.state(s);var mc=Minecraft.getInstance();double now=mc.level==null?0:mc.level.getGameTime()+partial;
        double age=tag.contains("shotTick")?now-tag.getLong("shotTick"):100;
        double cockAge=tag.contains("cockTick")?now-tag.getLong("cockTick"):100;
        double cock=PatersonItem.armed(s)?Math.clamp(cockAge/6,0,1):0;
        int chamber=Math.floorMod(tag.getInt("chamber"),5);
        double rotate=chamber*72-(tag.contains("shotTick")?(1-cock)*72:0);
        double hammer=-43*cock,trigger=-145*cock;
        if(age>=0&&age<3){double fall=1-Math.clamp(age/3,0,1);hammer=-43*fall;trigger=-145*fall;}
        double reload=0;
        if(PatersonItem.reloading(s)){double start=tag.getLong("reloadStart"),end=tag.getLong("reloadEnd");reload=Math.clamp((now-start)/Math.max(1,end-start),0,1);hammer=-20;trigger=0;}
        if(renderPerspective.firstPerson()){lastHeldHammerAngle=hammer;lastHeldTriggerAngle=trigger;}
        double open=reload==0?0:Math.min(Math.clamp(reload/.16,0,1),Math.clamp((1-reload)/.16,0,1));
        set("cylinder",0,0,0,(float)Math.toRadians(rotate+reload*360),0,0);
        set("hammer",0,0,0,0,0,(float)Math.toRadians(hammer));set("trigger",0,0,0,0,0,(float)Math.toRadians(trigger));
        set("wedge",0,0,(float)(-open*3),0,0,0);set("barrel",(float)(open*7),0,0,0,0,0);
        getGeoModel().getBone("cylinder").ifPresent(bone->{bone.setPosX((float)(open*4));bone.setPosY((float)(-open*3));});
        for(int i=0;i<5;i++){final int index=i;getGeoModel().getBone("loaded_bullet_"+i).ifPresent(bone->bone.setHidden(Math.floorMod(index-chamber,5)>=PatersonItem.rounds(s)));}
    }
    private void set(String name,float x,float y,float z,float rx,float ry,float rz){getGeoModel().getBone(name).ifPresent(bone->{bone.setPosX(x);bone.setPosY(y);bone.setPosZ(z);bone.setRotX(rx);bone.setRotY(ry);bone.setRotZ(rz);});}
}
