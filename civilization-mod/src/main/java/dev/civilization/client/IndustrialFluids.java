package dev.civilization.client;
import dev.civilization.IndustrialContent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class IndustrialFluids {
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerFluidType(new IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.withDefaultNamespace("block/yellow_terracotta");}
            @Override public ResourceLocation getFlowingTexture(){return getStillTexture();}
        },IndustrialContent.FUEL_TYPE.get(),IndustrialContent.HEATED_TYPE.get(),IndustrialContent.VAPOR_TYPE.get(),IndustrialContent.LUBE_TYPE.get());
        e.registerFluidType(new IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.fromNamespaceAndPath("civilization","block/crude_oil");}
            @Override public ResourceLocation getFlowingTexture(){return getStillTexture();}
        },IndustrialContent.CRUDE_TYPE.get());
    }
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(IndustrialContent.CRUDE.get(),net.minecraft.client.renderer.RenderType.translucent());
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(IndustrialContent.CRUDE_FLOWING.get(),net.minecraft.client.renderer.RenderType.translucent());
    });}
}
