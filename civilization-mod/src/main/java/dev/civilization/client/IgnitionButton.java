package dev.civilization.client;

import dev.civilization.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Two-frame mechanical strike; outcome and one-second cooldown belong to the server. */
final class IgnitionButton extends Button {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("civilization:textures/gui/ignition.png");
    private final CoalFireMenu fire;
    private long clicked=-10000;
    IgnitionButton(int x,int y,AbstractContainerMenu menu,CoalFireMenu fire){
        super(x,y,24,24,Component.literal("Strike to light"),b->Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,CoalFire.BUTTON),DEFAULT_NARRATION);
        this.fire=fire;
    }
    @Override public void onPress(){if((fire.fireState()&1)!=0||(fire.fireState()>>1)>0||net.minecraft.Util.getMillis()-clicked<1000)return;clicked=net.minecraft.Util.getMillis();super.onPress();}
    @Override public void playDownSound(SoundManager sound){} // Real flint/ignition audio comes from the server.
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float dt){
        visible=fire.hasCoalFire();if(!visible)return;
        boolean lit=(fire.fireState()&1)!=0;active=!lit;
        boolean strike=net.minecraft.Util.getMillis()-clicked<180;
        g.blit(TEXTURE,getX(),getY(),24,24,strike?64:0,0,64,64,128,64);
        if(lit)g.fill(getX()+9,getY()+20,getX()+15,getY()+22,0xffffb955);
        setTooltip(Tooltip.create(Component.literal(lit?"Fire lit — burns slowly while idle":"Strike to light • one attempt per second")));
    }
}
