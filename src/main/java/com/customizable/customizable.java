package com.customizable;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(customizable.MODID)
public class customizable {
    public static final String MODID = "customizable";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<RecipeSerializer<CustomDyeRecipe>> CUSTOM_DYE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("custom_dye_recipe", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(CustomDyeRecipe::new));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> DUMMY_MUSIC = SOUND_EVENTS.register("dummy_music", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "dummy_music")));

    public static final RegistryObject<Block> CUSTOM_PAINTING_BLOCK = BLOCKS.register("custom_painting_block", () -> new com.customizable.CustomPaintingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.0f).noOcclusion()));
    public static final RegistryObject<Block> CUSTOM_PAINTED_BLOCK = BLOCKS.register("custom_painted_block", () -> new com.customizable.CustomPaintedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(net.minecraft.world.level.block.SoundType.WOOL)));

    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.customizable.CustomPaintingBlockEntity>> CUSTOM_PAINTING_BE = BLOCK_ENTITIES.register("custom_painting_be", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(com.customizable.CustomPaintingBlockEntity::new, CUSTOM_PAINTING_BLOCK.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<com.customizable.CustomPaintedBlockEntity>> CUSTOM_PAINTED_BE = BLOCK_ENTITIES.register("custom_painted_be", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(com.customizable.CustomPaintedBlockEntity::new, CUSTOM_PAINTED_BLOCK.get()).build(null));

    public static final RegistryObject<Item> CUSTOM_PAINTED_ITEM = ITEMS.register("custom_painted_block", () -> new BlockItem(CUSTOM_PAINTED_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> CUSTOM_MUSIC_DISC = ITEMS.register("custom_music_disc", () -> new com.customizable.item.CustomMusicDiscItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CUSTOM_PAINTING = ITEMS.register("custom_painting", () -> new com.customizable.item.CustomPaintingItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CUSTOM_DYE = ITEMS.register("custom_dye", () -> new com.customizable.item.CustomDyeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(() ->CUSTOM_MUSIC_DISC.get().getDefaultInstance()).displayItems((parameters, output) -> {
        output.accept(CUSTOM_MUSIC_DISC.get());
        output.accept(CUSTOM_PAINTING.get());
        output.accept(CUSTOM_DYE.get());
    }).build());

    public customizable() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        com.customizable.network.ModMessages.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(CUSTOM_PAINTING);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            event.enqueueWork(() -> {
                try {
                    net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(CUSTOM_PAINTING_BE.get(), com.customizable.client.CustomPaintingRenderer::new);
                    net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(CUSTOM_PAINTED_BE.get(), com.customizable.client.CustomPaintedRenderer::new);
                } catch (Exception ignored) {}
            });
        }

        @SubscribeEvent
        public static void onBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
            event.register((state, level, pos, tintIndex) -> {
                if (level != null && pos != null) {
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof com.customizable.CustomPaintedBlockEntity cpbe) {
                        return cpbe.getColor();
                    }
                }
                return 0xFFFFFF;
            }, CUSTOM_PAINTED_BLOCK.get());
        }

        @SubscribeEvent
        public static void onItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    return com.customizable.item.CustomDyeItem.getColor(stack);
                }
                return 0xFFFFFF;
            }, CUSTOM_DYE.get());

            event.register((stack, tintIndex) -> {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("BlockEntityTag")) {
                    CompoundTag beTag = tag.getCompound("BlockEntityTag");
                    if (beTag.contains("Color")) return beTag.getInt("Color");
                }
                return 0xFFFFFF;
            }, CUSTOM_PAINTED_ITEM.get());
        }
    }
}
