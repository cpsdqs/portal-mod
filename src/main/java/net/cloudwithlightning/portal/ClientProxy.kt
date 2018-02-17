package net.cloudwithlightning.portal

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraftforge.fml.client.registry.IRenderFactory
import net.minecraftforge.fml.client.registry.RenderingRegistry

class ClientProxy {
    fun preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityPortal::class.java, RenderPortalFactory())
        Minecraft.getMinecraft().framebuffer.enableStencil()
    }

    class RenderPortalFactory: IRenderFactory<EntityPortal> {
        override fun createRenderFor(manager: RenderManager): Render<EntityPortal> {
            return RenderPortal(manager)
        }
    }
}
