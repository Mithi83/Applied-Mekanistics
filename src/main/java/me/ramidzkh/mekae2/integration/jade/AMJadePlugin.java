package me.ramidzkh.mekae2.integration.jade;

import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.patternprovider.PatternProviderLogicHost;

/**
 * Plugin to remove the mekanism-added chemical handler lines for interfaces and pattern providers.
 */
@WailaPlugin
public class AMJadePlugin implements IWailaPlugin {
    private static final ResourceLocation CHEMICALS = ResourceLocation.fromNamespaceAndPath("mekanism", "chemical");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback((box, accessor) -> {
            var target = accessor.getTarget();

            if (target instanceof InterfaceLogicHost || target instanceof PatternProviderLogicHost) {
                box.getTooltip().remove(CHEMICALS);
            }
        });
    }
}
