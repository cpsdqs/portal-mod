package net.cloudwithlightning.portal

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraft.util.math.BlockPos
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.EnumRarity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.*
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*


class ItemPortalGun(): Item() {
    init {
        unlocalizedName = "portal_gun"
        setRegistryName("portal_gun")
        creativeTab = CreativeTabs.TOOLS
        setMaxStackSize(1)
    }

    fun firePortal(player: EntityPlayer, soundColor: String, color: PortalColor, uuid: UUID): Boolean {
        val world = player.entityWorld

        var success = false

        val playerEyes = player.getPositionEyes(1f)
        val rayTraceResult = world.rayTraceBlocks(playerEyes, playerEyes.add(player.lookVec.scale( PORTAL_REACH_MAX)))

        var soundName = "portalgun_miss_" + soundColor
        if (rayTraceResult != null) {
            if (rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                if (EntityPortal.validateSurface(world, rayTraceResult.blockPos)) {
                    soundName = "portalgun_fire_" + soundColor

                    val portal = EntityPortal(world)
                    portal.portalID = uuid
                    portal.facing = rayTraceResult.sideHit
                    portal.portalColor = color
                    val portalPos = rayTraceResult.hitVec
                    var yaw = 0f

                    if (portal.facing == EnumFacing.UP || portal.facing == EnumFacing.DOWN) {
                        val vec = portalPos.subtract(playerEyes)
                        yaw = (Math.atan2(vec.z, vec.x) / Math.PI * 180.0).toFloat() - 90f
                    }

                    portal.setPositionAndRotation(portalPos.x, portalPos.y, portalPos.z, yaw, 0f)
                    world.spawnEntity(portal)

                    success = true
                } else {
                    val soundEvent = SoundEvent(ResourceLocation(PortalMod.MODID, "portalgun_invalid_surface"))
                    val soundPos = rayTraceResult.hitVec
                    world.playSound(null, soundPos.x, soundPos.y, soundPos.z, soundEvent, SoundCategory
                            .NEUTRAL, 1f, 1f)
                }
            } else if (rayTraceResult.typeOfHit == RayTraceResult.Type.ENTITY) {
                val soundEvent = SoundEvent(ResourceLocation(PortalMod.MODID, "portalgun_invalid_surface"))
                val soundPos = rayTraceResult.hitVec
                world.playSound(null, soundPos.x, soundPos.y, soundPos.z, soundEvent, SoundCategory
                        .NEUTRAL, 1f, 1f)
            }
        }

        val soundEvent = SoundEvent(ResourceLocation(PortalMod.MODID, soundName))
        world.playSound(null, player.posX, player.posY, player.posZ, soundEvent, SoundCategory.NEUTRAL, 1f, 1f)

        return success
    }

    fun onLeftClick(player: EntityPlayer, stack: ItemStack) {
        var uuid = getFirstID(stack)
        var fizzle: EntityPortal? = null
        if (uuid == null) {
            uuid = UUID.randomUUID()
            setFirstID(stack, uuid)
        } else {
            fizzle = EntityPortal.getPortalWithID(player.entityWorld, uuid)
        }

        if (firePortal(player, "blue", PortalColor.BLUE, uuid!!)) {
            fizzle?.destroy()
        }
    }

    fun onRightClick(player: EntityPlayer, stack: ItemStack) {
        var uuid = getSecondID(stack)
        var fizzle: EntityPortal? = null
        if (uuid == null) {
            uuid = UUID.randomUUID()
            setSecondID(stack, uuid)
        } else {
            fizzle = EntityPortal.getPortalWithID(player.entityWorld, uuid)
        }

        if (firePortal(player, "orange", PortalColor.ORANGE, uuid!!)) {
            fizzle?.destroy()
        }
    }

    fun getFirstID(stack: ItemStack): UUID? {
        return stack.tagCompound?.getUniqueId("firstPortalID")
    }

    fun setFirstID(stack: ItemStack, id: UUID) {
        if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        stack.tagCompound!!.setUniqueId("firstPortalID", id)
    }

    fun getSecondID(stack: ItemStack): UUID? {
        return stack.tagCompound?.getUniqueId("secondPortalID")
    }

    fun setSecondID(stack: ItemStack, id: UUID) {
        if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        stack.tagCompound!!.setUniqueId("secondPortalID", id)
    }

    override fun getRarity(stack: ItemStack?): EnumRarity {
        return EnumRarity.UNCOMMON
    }

    override fun onEntitySwing(entityLiving: EntityLivingBase?, stack: ItemStack?): Boolean {
        if (entityLiving is EntityPlayer) {
            if (!entityLiving.entityWorld.isRemote) {
                onLeftClick(entityLiving, stack!!)
            }
            return true
        }
        return super.onEntitySwing(entityLiving, stack)
    }

    override fun onLeftClickEntity(stack: ItemStack?, player: EntityPlayer?, entity: Entity?): Boolean {
        return true
    }

    override fun canDestroyBlockInCreative(world: World?, pos: BlockPos?, stack: ItemStack?, player: EntityPlayer?): Boolean {
        return false
    }

    override fun onItemUse(player: EntityPlayer?, worldIn: World?, pos: BlockPos?, hand: EnumHand?, facing: EnumFacing?, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
        if (!worldIn!!.isRemote) onRightClick(player!!, player.getHeldItem(hand))
        return EnumActionResult.SUCCESS
    }

    override fun onItemUseFirst(player: EntityPlayer?, world: World?, pos: BlockPos?, side: EnumFacing?, hitX: Float, hitY: Float, hitZ: Float, hand: EnumHand?): EnumActionResult {
        return EnumActionResult.SUCCESS
    }

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack> {
        if (!world.isRemote) onRightClick(player, player.getHeldItem(hand))
        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand))
    }

    @SideOnly(Side.CLIENT)
    fun registerModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!, "inventory"))
    }

    companion object {
        const val PORTAL_REACH_MAX = 300.0
    }
}
