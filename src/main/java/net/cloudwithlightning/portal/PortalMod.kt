package net.cloudwithlightning.portal

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import org.apache.logging.log4j.Logger

@Mod(modid = PortalMod.MODID, name = PortalMod.NAME, version = PortalMod.VERSION)
@Mod.EventBusSubscriber(modid = PortalMod.MODID)
class PortalMod {
    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog

        clientProxy?.preInit()
    }

    @EventHandler
    fun init(event: FMLInitializationEvent) {
        EntityRegistry.registerModEntity(EntityPortal.REGISTRY_NAME, EntityPortal::class.java, EntityPortal.NAME, 0,
                this, 64, 20, true)
    }

    companion object {
        const val MODID = "clportal"
        const val NAME = "Portal"
        const val VERSION = "1.0"

        var logger: Logger? = null

        private val hardLightBridge = BlockHardLightBridge()
        private val hardLightBridgeGenerator = BlockHardLightBridgeGenerator()
        private val ibHardLightBridgeGenerator = ItemBlockHardLightBridgeGenerator(hardLightBridgeGenerator)

        private val portalGun = ItemPortalGun()

        @SubscribeEvent
        @JvmStatic
        fun registerBlocks(event: RegistryEvent.Register<Block>) {
            val registry = event.registry
            registry.register(hardLightBridge)
            registry.register(hardLightBridgeGenerator)
        }

        @SubscribeEvent
        @JvmStatic
        fun registerItemBlocks(event: RegistryEvent.Register<Item>) {
            val registry = event.registry;
            registry.register(ibHardLightBridgeGenerator)
        }

        @SubscribeEvent
        @JvmStatic
        fun registerItems(event: RegistryEvent.Register<Item>) {
            val registry = event.registry
            registry.register(portalGun)
        }

        @SubscribeEvent
        @JvmStatic
        fun registerModels(event: ModelRegistryEvent) {
            portalGun.registerModel()
            ibHardLightBridgeGenerator.registerModel()
        }

        fun soundEventForName(name: String): SoundEvent {
            return SoundEvent(ResourceLocation(MODID, name)).setRegistryName(name)
        }

        @SubscribeEvent
        @JvmStatic
        fun registerSounds(event: RegistryEvent.Register<SoundEvent>) {
            val registry = event.registry
            registry.register(soundEventForName("portalgun_fire_blue"))
            registry.register(soundEventForName("portalgun_fire_orange"))
            registry.register(soundEventForName("portalgun_miss_blue"))
            registry.register(soundEventForName("portalgun_miss_orange"))
            registry.register(soundEventForName("portalgun_invalid_surface"))
            registry.register(soundEventForName("portal_open_blue"))
            registry.register(soundEventForName("portal_open_orange"))
            registry.register(soundEventForName("portal_fizzle"))
            registry.register(soundEventForName("light_bridge_step"))
            registry.register(soundEventForName("light_bridge_land"))
            registry.register(soundEventForName("portal_ambient"))
            registry.register(soundEventForName("null_sound"))
        }

        @SidedProxy(clientSide = "net.cloudwithlightning.portal.ClientProxy")
        @JvmStatic
        var clientProxy: ClientProxy? = null
    }
}
