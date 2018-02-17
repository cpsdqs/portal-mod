package net.cloudwithlightning.portal

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Mod.EventBusSubscriber(modid = PortalMod.MODID)
class Events {
    companion object {
        fun hasPortalGunInHand(player: EntityPlayer): Boolean {
            return player.getHeldItem(EnumHand.MAIN_HAND).item is ItemPortalGun
        }

        fun blockIsHardLightBridge(world: World, pos: BlockPos): Boolean {
            return world.getBlockState(pos).block is BlockHardLightBridge
        }

        @SubscribeEvent
        @JvmStatic
        fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
            if (hasPortalGunInHand(event.entityPlayer)
                    || blockIsHardLightBridge(event.entityPlayer.entityWorld, event.pos)) {
                event.isCanceled = true
            }
        }

        @SubscribeEvent
        @JvmStatic
        fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
            if (hasPortalGunInHand(event.entityPlayer)) {
                event.isCanceled = true
            }
        }

        @SubscribeEvent
        @JvmStatic
        fun onBlockBreakEvent(event: BlockEvent.BreakEvent) {
            if (hasPortalGunInHand(event.player)
                    || blockIsHardLightBridge(event.player.entityWorld, event.pos)) {
                event.isCanceled = true
            }
        }

        @SubscribeEvent
        @JvmStatic
        fun onEntityInteractSpecific(event: PlayerInteractEvent.EntityInteract) {
            if (hasPortalGunInHand(event.entityPlayer)) {
                event.isCanceled = true
            }
        }
    }
}

