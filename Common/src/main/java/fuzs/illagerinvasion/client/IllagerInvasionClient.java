package fuzs.illagerinvasion.client;

import fuzs.illagerinvasion.IllagerInvasion;
import fuzs.illagerinvasion.client.gui.screens.inventory.ImbuingScreen;
import fuzs.illagerinvasion.client.init.ClientModRegistry;
import fuzs.illagerinvasion.client.model.*;
import fuzs.illagerinvasion.client.render.entity.*;
import fuzs.illagerinvasion.init.ModRegistry;
import fuzs.puzzleslib.api.client.core.v1.ClientModConstructor;
import fuzs.puzzleslib.api.client.core.v1.context.*;
import fuzs.puzzleslib.api.core.v1.context.ModLifecycleContext;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class IllagerInvasionClient implements ClientModConstructor {

    @Override
    public void onClientSetup(ModLifecycleContext context) {
        MenuScreens.register(ModRegistry.IMBUING_MENU_TYPE.get(), ImbuingScreen::new);
    }

    @Override
    public void onRegisterEntityRenderers(EntityRenderersContext context) {
        context.registerEntityRenderer(ModRegistry.PROVOKER_ENTITY_TYPE.get(), ProvokerRender::new);
        context.registerEntityRenderer(ModRegistry.INVOKER_ENTITY_TYPE.get(), InvokerRender::new);
        context.registerEntityRenderer(ModRegistry.SURRENDERED_ENTITY_TYPE.get(), SurrenderedRender::new);
        context.registerEntityRenderer(ModRegistry.BASHER_ENTITY_TYPE.get(), BasherRender::new);
        context.registerEntityRenderer(ModRegistry.SORCERER_ENTITY_TYPE.get(), SorcererRender::new);
        context.registerEntityRenderer(ModRegistry.ARCHIVIST_ENTITY_TYPE.get(), ArchivistRender::new);
        context.registerEntityRenderer(ModRegistry.INQUISITOR_ENTITY_TYPE.get(), InquisitorRender::new);
        context.registerEntityRenderer(ModRegistry.MARAUDER_ENTITY_TYPE.get(), MarauderRender::new);
        context.registerEntityRenderer(ModRegistry.ALCHEMIST_ENTITY_TYPE.get(), AlchemistRender::new);
        context.registerEntityRenderer(ModRegistry.FIRECALLER_ENTITY_TYPE.get(), FirecallerRender::new);
        context.registerEntityRenderer(ModRegistry.INVOKER_FANGS_ENTITY_TYPE.get(), InvokerFangsRenderer::new);
        context.registerEntityRenderer(ModRegistry.HATCHET_ENTITY_TYPE.get(), HatchetRender::new);
        context.registerEntityRenderer(ModRegistry.FLYING_MAGMA_ENTITY_TYPE.get(), MagmaEntityRender::new);
    }

    @Override
    public void onRegisterParticleProviders(ParticleProvidersContext context) {
        context.registerParticleFactory(ModRegistry.MAGIC_FLAME_PARTICLE_TYPE.get(), FlameParticle.Provider::new);
    }

    @Override
    public void onRegisterLayerDefinitions(LayerDefinitionsContext context) {
        context.registerLayerDefinition(ClientModRegistry.CAPED_ILLAGER, InvokerEntityModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.INVOKER_SHIELD, InvokerEntityModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.INVOKER_FANGS, InvokerFangsModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.ARMORED_ILLAGER, ArmoredIllagerEntityModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.HAT_ILLAGER, HatIllagerEntityModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.STAFF_ILLAGER, IllagerWithStaffEntityModel::getTexturedModelData);
        context.registerLayerDefinition(ClientModRegistry.BRIM_HAT_ILLAGER, BrimmedHatIllagerEntityModel::getTexturedModelData);
    }

    @Override
    public void onRegisterBlockRenderTypes(RenderTypesContext<Block> context) {
        context.registerRenderType(RenderType.cutout(), ModRegistry.MAGIC_FIRE_BLOCK.get());
    }

    @Override
    public void onRegisterItemModelProperties(ItemModelPropertiesContext context) {
        context.registerItemProperty(IllagerInvasion.id("pull"), (ItemStack itemStack, ClientLevel world, LivingEntity livingEntity, int i) -> {
            return livingEntity == null || livingEntity.getUseItem() != itemStack ? 0.0F : (float) (itemStack.getUseDuration() - livingEntity.getUseItemRemainingTicks()) / 20.0F;
        }, ModRegistry.HORN_OF_SIGHT_ITEM.get());
        context.registerItemProperty(IllagerInvasion.id("pulling"), (ItemStack itemStack, ClientLevel clientWorld, LivingEntity livingEntity, int i) -> {
            return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getUseItem() == itemStack ? 1.0F : 0.0F;
        }, ModRegistry.HORN_OF_SIGHT_ITEM.get());
    }
}
