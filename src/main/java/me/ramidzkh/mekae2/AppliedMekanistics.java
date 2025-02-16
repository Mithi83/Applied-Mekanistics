package me.ramidzkh.mekae2;

import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import me.ramidzkh.mekae2.ae2.*;
import me.ramidzkh.mekae2.ae2.stack.MekanismExternalStorageStrategy;
import me.ramidzkh.mekae2.ae2.stack.MekanismStackExportStrategy;
import me.ramidzkh.mekae2.ae2.stack.MekanismStackImportStrategy;
import me.ramidzkh.mekae2.data.MekAE2DataGenerators;
import me.ramidzkh.mekae2.qio.QioSupport;

import appeng.api.AECapabilities;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.behaviors.GenericSlotCapacities;
import appeng.api.client.StorageCellModels;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.parts.automation.StackWorldBehaviors;

@Mod(AppliedMekanistics.ID)
public class AppliedMekanistics {

    public static final String ID = "appmek";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public AppliedMekanistics(IEventBus bus) {
        AMItems.initialize(bus);
        AMMenus.initialize(bus);

        bus.addListener(MekAE2DataGenerators::onGatherData);

        bus.addListener((RegisterEvent event) -> {
            if (!event.getRegistryKey().equals(Registries.BLOCK)) {
                return;
            }

            AEKeyTypes.register(MekanismKeyType.TYPE);
        });

        StackWorldBehaviors.registerImportStrategy(MekanismKeyType.TYPE, MekanismStackImportStrategy::new);
        StackWorldBehaviors.registerExportStrategy(MekanismKeyType.TYPE, MekanismStackExportStrategy::new);
        StackWorldBehaviors.registerExternalStorageStrategy(MekanismKeyType.TYPE, MekanismExternalStorageStrategy::new);

        ContainerItemStrategy.register(MekanismKeyType.TYPE, MekanismKey.class, new ChemicalContainerItemStrategy());
        GenericSlotCapacities.register(MekanismKeyType.TYPE, GenericSlotCapacities.getMap().get(AEKeyType.fluids()));

        bus.addListener(EventPriority.LOWEST, this::registerGenericAdapters);

        bus.addListener(QioSupport::onBlockEntityCapability);
        bus.addListener(this::registerPartCapabilities);

        bus.addListener((FMLCommonSetupEvent event) -> {
            event.enqueueWork(this::initializeModels);
            event.enqueueWork(this::initializeUpgrades);
            event.enqueueWork(this::initializeAttunement);
        });

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AppliedMekanisticsClient.initialize(bus);
        }
    }

    private void registerPartCapabilities(RegisterPartCapabilitiesEvent event) {
        event.register(MekCapabilities.CHEMICAL.block(), (part, context) -> part.getExposedApi(),
                ChemicalP2PTunnelPart.class);
    }

    private void registerGenericAdapters(RegisterCapabilitiesEvent event) {
        for (var block : BuiltInRegistries.BLOCK) {
            if (event.isBlockRegistered(AECapabilities.GENERIC_INTERNAL_INV, block)) {
                registerGenericInvAdapter(event, block, MekCapabilities.CHEMICAL.block(),
                        GenericStackChemicalStorage::new);
            }
        }
    }

    private void initializeModels() {
        for (var tier : AMItems.Tier.values()) {
            var cell = AMItems.get(tier);
            var portable = AMItems.getPortableCell(tier);
            var path = cell.getId().getPath();

            StorageCellModels.registerModel(cell.get(), id("block/drive/cells/" + path));
            StorageCellModels.registerModel(portable.get(), id("block/drive/cells/" + path));
        }
    }

    private void initializeUpgrades() {
        var storageCellGroup = GuiText.StorageCells.getTranslationKey();
        var portableStorageCellGroup = GuiText.PortableCells.getTranslationKey();

        for (var tier : AMItems.Tier.values()) {
            var cell = AMItems.get(tier);
            var portableCell = AMItems.getPortableCell(tier);

            Upgrades.add(AEItems.INVERTER_CARD, cell::get, 1, storageCellGroup);
            Upgrades.add(AEItems.EQUAL_DISTRIBUTION_CARD, cell::get, 1, storageCellGroup);
            Upgrades.add(AEItems.VOID_CARD, cell::get, 1, storageCellGroup);

            Upgrades.add(AEItems.INVERTER_CARD, portableCell::get, 1, portableStorageCellGroup);
            Upgrades.add(AEItems.ENERGY_CARD, portableCell::get, 2, portableStorageCellGroup);
        }
    }

    private void initializeAttunement() {
        P2PTunnelAttunement.registerAttunementTag(AMItems.CHEMICAL_P2P_TUNNEL::get);
    }

    private static <T> void registerGenericInvAdapter(RegisterCapabilitiesEvent event, Block block,
            BlockCapability<T, Direction> capability, Function<GenericInternalInventory, T> adapter) {
        event.registerBlock(capability, (level, pos, state, blockEntity, context) -> {
            var genericInv = level.getCapability(AECapabilities.GENERIC_INTERNAL_INV, pos, state,
                    blockEntity, context);
            if (genericInv != null) {
                return adapter.apply(genericInv);
            }
            return null;
        }, block);
    }
}
