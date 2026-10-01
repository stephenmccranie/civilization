package dev.civilization.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/** Shared asset convention. Machine state, animation policy and collision stay with the machine. */
public final class MachineGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation geometry;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public MachineGeoModel(String asset) {
        geometry = ResourceLocation.fromNamespaceAndPath("civilization", "geo/" + asset + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath("civilization", "textures/entity/" + asset + ".png");
        animation = ResourceLocation.fromNamespaceAndPath("civilization", "animations/" + asset + ".animation.json");
    }

    @Override public ResourceLocation getModelResource(T machine) { return geometry; }
    @Override public ResourceLocation getTextureResource(T machine) { return texture; }
    @Override public ResourceLocation getAnimationResource(T machine) { return animation; }
}
