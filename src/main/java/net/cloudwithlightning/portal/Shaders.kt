package net.cloudwithlightning.portal

import net.minecraft.client.Minecraft
import net.minecraft.client.shader.ShaderManager
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
object Shaders {
    val PORTAL_SHADER = ShaderManager(Minecraft.getMinecraft().resourceManager, "clportal:portal")
    val PORTAL_GLOW_SHADER = ShaderManager(Minecraft.getMinecraft().resourceManager, "clportal:portal_glow")
    val PORTAL_CLEAR_SHADER = ShaderManager(Minecraft.getMinecraft().resourceManager, "clportal:portal_clear")
}
