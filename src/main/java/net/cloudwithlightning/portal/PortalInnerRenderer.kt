package net.cloudwithlightning.portal

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.resources.IResourceManager
import net.minecraft.entity.Entity

class PortalInnerRenderer(minecraft: Minecraft, resourceManager: IResourceManager) {
    var mc: Minecraft = minecraft;

    fun render(entity: Entity, icamera: ICamera, partialTicks: Float) {
        // TODO
    }
}
