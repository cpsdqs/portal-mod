package net.cloudwithlightning.portal

import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = PortalMod.MODID)
class ClientEvents {
    companion object {
        @SubscribeEvent
        @JvmStatic
        fun onDrawBlockHighlight(event: DrawBlockHighlightEvent) {
            if (event.player.getHeldItem(EnumHand.MAIN_HAND).item is ItemPortalGun) {
                event.isCanceled = true
                return
            }

            if (event.target.typeOfHit == RayTraceResult.Type.BLOCK) {
                val world = event.player.entityWorld
                val state = world.getBlockState(event.target.blockPos)

                if (state.block is BlockHardLightBridge) {
                    event.isCanceled = true
                }
            }
        }
    }
}
