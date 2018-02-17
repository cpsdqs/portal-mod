package net.cloudwithlightning.portal

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11

fun easeOutExpo(t: Float): Float {
    return 1 - Math.pow(2.0, -10 * t.toDouble()).toFloat()
}

@SideOnly(Side.CLIENT)
class RenderPortal(manager: RenderManager): Render<EntityPortal>(manager) {
    private var displayList: Int? = null
    private var displayListGlow: Int? = null
    private var displayListClear: Int? = null
    private var compiled = false
    private var lastCamera: ICamera? = null
    private val innerRenderer = PortalInnerRenderer(Minecraft.getMinecraft(), Minecraft.getMinecraft().resourceManager)

    init {
        shadowOpaque = 0f
    }

    override fun shouldRender(livingEntity: EntityPortal?, camera: ICamera?, camX: Double, camY: Double, camZ: Double): Boolean {
        lastCamera = camera
        return super.shouldRender(livingEntity, camera, camX, camY, camZ)
    }

    private fun compileDisplayList() {
        val resolution = 24
        val tessellator = Tessellator.getInstance()

        run {
            displayList = GLAllocation.generateDisplayLists(1)
            GlStateManager.glNewList(displayList!!, GL11.GL_COMPILE)
            val renderer = tessellator.buffer

            renderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_NORMAL)

            renderer.pos(0.0, 0.0, 0.0)
            renderer.normal(0f, 1f, 0f)
            renderer.endVertex()

            var angleIndex = 0

            while (angleIndex <= resolution) {
                val angle = angleIndex * -Math.PI / resolution * 2
                renderer.pos(EntityPortal.PORTAL_WIDTH / 2 * Math.cos(angle), 1.0 / 32,
                        EntityPortal.PORTAL_HEIGHT / 2 * Math.sin(angle))
                renderer.normal(0f, 1f, 0f)
                renderer.endVertex()
                angleIndex++
            }

            tessellator.draw()

            GlStateManager.glEndList()
        }

        run {
            displayListGlow = GLAllocation.generateDisplayLists(1)
            GlStateManager.glNewList(displayListGlow!!, GL11.GL_COMPILE)
            val renderer = tessellator.buffer

            renderer.reset()
            renderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_NORMAL)

            var angleIndex = 0

            while (angleIndex <= resolution) {
                val angle = angleIndex * -Math.PI / resolution * 2
                val posX = EntityPortal.PORTAL_WIDTH / 2 * Math.cos(angle) * 0.9
                val posZ = EntityPortal.PORTAL_HEIGHT / 2 * Math.sin(angle) * 0.9
                renderer.pos(posX, 1.0 / 32, posZ)
                renderer.normal(0f, 0f, 0f)
                renderer.endVertex()
                renderer.pos(posX * 1.1, 7.0 / 32, posZ * 1.1)
                renderer.normal(0f, 0f, 0f)
                renderer.endVertex()
                angleIndex++
            }

            tessellator.draw()

            GlStateManager.glEndList()
        }

        run {
            displayListClear = GLAllocation.generateDisplayLists(1)
            GlStateManager.glNewList(displayListClear!!, GL11.GL_COMPILE)
            val renderer = tessellator.buffer

            renderer.reset()
            renderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_NORMAL)

            // a big square
            renderer.pos(1.0, 1.0, 0.0)
            renderer.normal(0f, 0f, 1f)
            renderer.endVertex()

            renderer.pos(-1.0, 1.0, 0.0)
            renderer.normal(0f, 0f, 1f)
            renderer.endVertex()

            renderer.pos(1.0, -1.0, 0.0)
            renderer.normal(0f, 0f, 1f)
            renderer.endVertex()

            renderer.pos(-1.0, -1.0, 0.0)
            renderer.normal(0f, 0f, 1f)
            renderer.endVertex()

            tessellator.draw()

            GlStateManager.glEndList()
        }

        compiled = true
    }

    override fun doRender(entity: EntityPortal?, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        if (!compiled) compileDisplayList()

        entity!!

        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        GlStateManager.disableLighting()
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        GlStateManager.alphaFunc(GL11.GL_ALWAYS, 0f)
        GlStateManager.enableBlend()

        GlStateManager.translate(x, y, z)

        val facing = entity.facing ?: EnumFacing.UP

        when (facing) {
            EnumFacing.DOWN -> {
                GlStateManager.rotate(180f, 0f, 0f, 1f)
                GlStateManager.rotate(180f - entityYaw, 0f, 1f, 0f)
            }
            EnumFacing.UP -> {
                GlStateManager.rotate(180f - entityYaw, 0f, 1f, 0f)
            }
            EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH -> {
                GlStateManager.rotate(90f, 1f, 0f, 0f)
                if (facing == EnumFacing.SOUTH || facing == EnumFacing.NORTH) {
                    GlStateManager.rotate(-facing.horizontalAngle, 0f, 0f, 1f)
                } else {
                    GlStateManager.rotate(180f - facing.horizontalAngle, 0f, 0f, 1f)
                }
            }
        }

        val openAnim = if (entity.animationTicks < OPEN_ANIM_TICKS) {
            easeOutExpo((entity.animationTicks.toFloat() + partialTicks) / OPEN_ANIM_TICKS)
        } else { 1f }

        // TODO
        val linkAnim = 1f;

        val fizzleAnim = if (entity.deathTime > 0) {
            (entity.deathTime.toFloat() + partialTicks) / EntityPortal.DEATH_ANIMATION_TICKS
        } else { 0f }

        val pColor = entity.portalColor?.asVec3() ?: floatArrayOf(1f, 0f, 1f)

        // for when there's no shaders, if that ever happens
        GlStateManager.color(pColor[0], pColor[1], pColor[2], 1f)

        if (entity.deathTime == 0) {
            Shaders.PORTAL_SHADER.getShaderUniform("time")?.set(entity.animationTicks.toFloat())
            Shaders.PORTAL_SHADER.getShaderUniform("open_anim")?.set(openAnim)
            Shaders.PORTAL_SHADER.getShaderUniform("link_anim")?.set(linkAnim)
            Shaders.PORTAL_SHADER.getShaderUniform("portal_size")?.set(EntityPortal.PORTAL_WIDTH.toFloat(),
                    EntityPortal.PORTAL_HEIGHT.toFloat())
            Shaders.PORTAL_SHADER.getShaderUniform("portal_color")?.set(pColor[0], pColor[1], pColor[2])

            GL11.glEnable(GL11.GL_STENCIL_TEST)
            GL11.glColorMask(false, false, false, false)
            GL11.glDepthMask(false)
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xf);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)
            GL11.glStencilMask(0xff)
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)

            GlStateManager.pushMatrix()
            GlStateManager.scale(.87 * openAnim, 1.0, .87 * openAnim)
            GlStateManager.callList(displayList!!)
            GlStateManager.popMatrix()

            GL11.glColorMask(true, true, true, true)
            GL11.glDepthMask(true)
            GL11.glStencilMask(0)
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xff)

            GlStateManager.disableDepth()

            Shaders.PORTAL_CLEAR_SHADER.useShader()
            GlStateManager.callList(displayListClear!!)
            Shaders.PORTAL_CLEAR_SHADER.endShader()

            GlStateManager.enableDepth()

            val minecraft = Minecraft.getMinecraft()
            minecraft.mcProfiler.startSection("portal_inner")

            // TODO: render everything else

            if (lastCamera != null) {
                innerRenderer.render(entity, lastCamera!!, partialTicks)
            }

            minecraft.mcProfiler.endSection()

            GL11.glDisable(GL11.GL_STENCIL_TEST)

            Shaders.PORTAL_SHADER.useShader()
            GlStateManager.callList(displayList!!)
            Shaders.PORTAL_SHADER.endShader()
        }

        if (openAnim == 1f) {
            val glowOpenAnim = if (entity.animationTicks < OPEN_ANIM_TICKS * 2) {
                easeOutExpo((entity.animationTicks.toFloat() - OPEN_ANIM_TICKS + partialTicks) / OPEN_ANIM_TICKS)
            } else { 1f }

            Shaders.PORTAL_GLOW_SHADER.getShaderUniform("time")?.set(entity.animationTicks.toFloat())
            Shaders.PORTAL_GLOW_SHADER.getShaderUniform("open_anim")?.set(glowOpenAnim)
            Shaders.PORTAL_GLOW_SHADER.getShaderUniform("fizzle_anim")?.set(fizzleAnim)
            Shaders.PORTAL_GLOW_SHADER.getShaderUniform("portal_size")?.set(EntityPortal.PORTAL_WIDTH.toFloat() *
                    .9f, 6f / 32, EntityPortal.PORTAL_HEIGHT.toFloat() * .9f)
            Shaders.PORTAL_GLOW_SHADER.getShaderUniform("portal_color")?.set(pColor[0], pColor[1], pColor[2])

            Shaders.PORTAL_GLOW_SHADER.useShader()

            GlStateManager.callList(displayListGlow!!)

            Shaders.PORTAL_GLOW_SHADER.endShader()
        }

        // TODO: reset GL state (blend func) because all subsequently rendered entities are partially black

        GlStateManager.enableLighting()
        GlStateManager.enableTexture2D()
        GlStateManager.disableAlpha()
        GlStateManager.enableCull()
        GlStateManager.popMatrix()
    }

    override fun getEntityTexture(entity: EntityPortal?): ResourceLocation? {
        return null
    }

    companion object {
        const val OPEN_ANIM_TICKS = 20
    }
}
