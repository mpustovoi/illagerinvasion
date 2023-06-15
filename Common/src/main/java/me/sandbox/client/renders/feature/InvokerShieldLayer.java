package me.sandbox.client.renders.feature;

import fuzs.illagerinvasion.client.init.ClientModRegistry;
import me.sandbox.client.model.InvokerEntityModel;
import me.sandbox.entity.monster.Invoker;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class InvokerShieldLayer extends EnergySwirlLayer<Invoker, InvokerEntityModel<Invoker>> {
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/wither/wither_armor.png");

    private final InvokerEntityModel<Invoker> model;

    public InvokerShieldLayer(RenderLayerParent<Invoker, InvokerEntityModel<Invoker>> context, EntityModelSet loader) {
        super(context);
        this.model = new InvokerEntityModel<Invoker>(loader.bakeLayer(ClientModRegistry.INVOKER_SHIELD));
    }

    @Override
    protected float xOffset(float partialAge) {
        return Mth.cos(partialAge * 0.02f) * 3.0f;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return TEXTURE_LOCATION;
    }

    @Override
    protected InvokerEntityModel<Invoker> model() {
        return this.model;
    }
}
