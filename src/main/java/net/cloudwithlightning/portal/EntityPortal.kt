package net.cloudwithlightning.portal

import com.google.common.base.Optional
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.datasync.DataSerializers
import net.minecraft.network.datasync.EntityDataManager
import net.minecraft.util.*
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

enum class PortalColor {
    BLUE,
    ORANGE,
    UNDEFINED;

    fun asVec3(): FloatArray {
        return when (this) {
            PortalColor.BLUE -> floatArrayOf(.1f, .5f, 1f)
            PortalColor.ORANGE -> floatArrayOf(1f, .5f, .1f)
            PortalColor.UNDEFINED -> floatArrayOf(1f, 1f, 1f)
        }
    }

    fun asByte(): Byte {
        return when (this) {
            PortalColor.BLUE -> 0
            PortalColor.ORANGE -> 1
            PortalColor.UNDEFINED -> 15
        }
    }

    companion object {
        fun fromByte(byte: Byte): PortalColor {
            return when (byte.toInt()) {
                0 -> PortalColor.BLUE
                1 -> PortalColor.ORANGE
                else -> PortalColor.UNDEFINED
            }
        }
    }
}

class EntityPortal(worldIn: World): Entity(worldIn) {
    private var soundTickCounter = 0
    var animationTicks = 0
    var didSoundOpen = false
    var deathTime = 0

    init {
        isImmuneToFire = true
    }

    private fun updateBoundingBox() {
        if (facing == null) return
        when (facing) {
            EnumFacing.UP, EnumFacing.DOWN -> {
                var cornerLeast = Vec3d(-PORTAL_WIDTH / 2, 0.0, -PORTAL_HEIGHT / 2)
                var cornerLeastX = Vec3d(-PORTAL_WIDTH / 2, 0.0, PORTAL_HEIGHT / 2)
                var cornerMost = Vec3d(PORTAL_WIDTH / 2, 0.0, PORTAL_HEIGHT / 2)
                var cornerMostX = Vec3d(PORTAL_WIDTH / 2, 0.0, -PORTAL_HEIGHT / 2)

                cornerLeast = cornerLeast.rotateYaw(rotationYaw)
                cornerMost = cornerMost.rotateYaw(rotationYaw)
                cornerLeastX = cornerLeastX.rotateYaw(rotationYaw)
                cornerMostX = cornerMostX.rotateYaw(rotationYaw)

                var leastX = Math.min(Math.min(Math.min(cornerLeast.x, cornerLeastX.x), cornerMost.x), cornerMostX.x)
                var leastZ = Math.min(Math.min(Math.min(cornerLeast.z, cornerLeastX.z), cornerMost.z), cornerMostX.z)
                var mostX = Math.max(Math.max(Math.max(cornerLeast.x, cornerLeastX.x), cornerMost.x), cornerMostX.x)
                var mostZ = Math.max(Math.max(Math.max(cornerLeast.z, cornerLeastX.z), cornerMost.z), cornerMostX.z)

                var leastY = if (facing == EnumFacing.DOWN) { 1.0 - PORTAL_DEPTH } else { 0.0 }
                var mostY = if (facing == EnumFacing.DOWN) { 1.0 } else { PORTAL_DEPTH }

                leastX += posX
                leastY += posY
                leastZ += posZ
                mostX += posX
                mostY += posY
                mostZ += posZ

                entityBoundingBox = AxisAlignedBB(leastX, leastY, leastZ, mostX, mostY, mostZ)
                width = PORTAL_HEIGHT.toFloat()
                height = PORTAL_DEPTH.toFloat()
            }
            else -> {
                // FIXME: something doesn't work right in any rotated orientation (i.e. not south)

                var cornerLeast = Vec3d(-PORTAL_WIDTH / 2, -PORTAL_HEIGHT / 2, 0.0)
                var cornerMost = Vec3d(PORTAL_WIDTH / 2, PORTAL_HEIGHT / 2, PORTAL_DEPTH)

                val rotation = when(facing) {
                    EnumFacing.SOUTH -> 0f
                    EnumFacing.NORTH -> 180f
                    EnumFacing.WEST -> -90f
                    EnumFacing.EAST -> 90f
                    else -> 0f
                }

                cornerLeast = cornerLeast.rotateYaw(rotation)
                cornerMost = cornerMost.rotateYaw(rotation)

                var leastX = Math.min(cornerLeast.x, cornerMost.x)
                var leastY = Math.min(cornerLeast.y, cornerMost.y)
                var leastZ = Math.min(cornerLeast.z, cornerMost.z)
                var mostX = Math.max(cornerLeast.x, cornerMost.x)
                var mostY = Math.max(cornerLeast.y, cornerMost.y)
                var mostZ = Math.max(cornerLeast.z, cornerMost.z)

                leastX += posX
                leastY += posY
                leastZ += posZ
                mostX += posX
                mostY += posY
                mostZ += posZ

                entityBoundingBox = AxisAlignedBB(leastX, leastY, leastZ, mostX, mostY, mostZ)
                width = PORTAL_WIDTH.toFloat()
                height = PORTAL_HEIGHT.toFloat()
            }
        }
    }

    var facing: EnumFacing?
        get() {
            if (dataManager == null) return null
            return EnumFacing.values()[dataManager.get(FACING).toInt()]
        }
        set(facing) {
            dataManager.set(FACING, facing?.index?.toByte() ?: 0)
        }

    var portalColor: PortalColor?
        get() {
            if (dataManager == null) return null
            return PortalColor.fromByte(dataManager.get(PORTAL_COLOR))
        }
        set(color) {
            dataManager.set(PORTAL_COLOR, color?.asByte() ?: 0)
        }

    var portalID: UUID?
        get() {
            if (dataManager == null) return null
            return dataManager.get(PORTAL_ID).orNull()
        }
        set(id: UUID?) {
            dataManager.set(PORTAL_ID, Optional.fromNullable(id))
        }

    var alive: Boolean?
        get() {
            if (dataManager == null) return null
            return dataManager.get(ALIVE)
        }
        set(alive) {
            dataManager.set(ALIVE, alive)
        }

    override fun entityInit() {
        dataManager.register(FACING, 0)
        dataManager.register(PORTAL_COLOR, 0)
        dataManager.register(PORTAL_ID, Optional.of(UUID.randomUUID()))
        dataManager.register(ALIVE, true)
    }

    override fun onUpdate() {
        if (soundTickCounter-- <= 0) {
            val soundDurFac = 1f + .2f * prng.nextFloat()
            soundTickCounter = (SOUND_INTERVAL_TICKS.div(1f) * soundDurFac).toInt()

            // FIXME: it's super annoying when it phases in and out
//            this.playSound(AMBIENT_SOUND, 1f, 1f)
        }
        animationTicks++

        if (!didSoundOpen) {
            playSound(when (this.portalColor) {
                PortalColor.BLUE -> OPEN_SOUND_BLUE
                PortalColor.ORANGE -> OPEN_SOUND_ORANGE
                else -> {
                    if (prng.nextBoolean()) {
                        OPEN_SOUND_BLUE
                    } else {
                        OPEN_SOUND_ORANGE
                    }
                }
            }, 1f, 1f)
            didSoundOpen = true
        }

        if (alive != true) {
            if (deathTime == 0) {
                playSound(FIZZLE_SOUND, 1f, 1f)
            }

            deathTime++

            if (deathTime >= DEATH_ANIMATION_TICKS) {
                setDead()
            }
        }
    }

    fun destroy() {
        alive = false
    }

    override fun onKillCommand() {
        destroy()
    }

    override fun writeEntityToNBT(compound: NBTTagCompound?) {
        compound!!
        compound.setByte("Facing", facing?.index?.toByte() ?: 0)
        compound.setByte("PortalColor", portalColor?.asByte() ?: 0)
        compound.setBoolean("PortalAlive", alive ?: false)
        compound.setShort("DeathTime", deathTime.toShort())
        compound.setUniqueId("PortalID", portalID)
    }

    override fun readEntityFromNBT(compound: NBTTagCompound?) {
        compound!!
        facing = EnumFacing.values()[compound.getByte("Facing").toInt()]
        portalColor = PortalColor.fromByte(compound.getByte("PortalColor"))
        portalID = compound.getUniqueId("PortalID")
        alive = compound.getBoolean("PortalAlive")
        deathTime = compound.getShort("DeathTime").toInt()

        updateBoundingBox()
    }

    override fun setPosition(x: Double, y: Double, z: Double) {
        super.setPosition(x, y, z)

        updateBoundingBox()
    }

    override fun setRotation(yaw: Float, pitch: Float) {
        super.setRotation(yaw, pitch)

        updateBoundingBox()
    }

    override fun isImmuneToExplosions(): Boolean { return true }
    override fun canBePushed(): Boolean { return false }
    override fun hasNoGravity(): Boolean { return true }
    override fun canTrample(world: World?, block: Block?, pos: BlockPos?, fallDistance: Float): Boolean {
        return false
    }

    override fun attackEntityFrom(source: DamageSource?, amount: Float): Boolean {
        // TODO: check if supporting blocks are intact
        return false
    }

    override fun onStruckByLightning(lightningBolt: EntityLightningBolt?) {}

    companion object {
        val REGISTRY_NAME = ResourceLocation(PortalMod.MODID, "portal")
        const val NAME = "portal"

        val prng = Random()

        val AMBIENT_SOUND = SoundEvent(ResourceLocation(PortalMod.MODID, "portal_ambient"))
        val OPEN_SOUND_BLUE = SoundEvent(ResourceLocation(PortalMod.MODID, "portal_open_blue"))
        val OPEN_SOUND_ORANGE = SoundEvent(ResourceLocation(PortalMod.MODID, "portal_open_orange"))
        val FIZZLE_SOUND = SoundEvent(ResourceLocation(PortalMod.MODID, "portal_fizzle"))
        const val PORTAL_WIDTH = 1.25
        const val PORTAL_HEIGHT = 2.25
        const val PORTAL_DEPTH = 1.0 / 32
        const val SOUND_INTERVAL_TICKS = 20 * 5
        const val DEATH_ANIMATION_TICKS = 20

        val FACING = EntityDataManager.createKey(EntityPortal::class.java, DataSerializers.BYTE)
        val PORTAL_COLOR = EntityDataManager.createKey(EntityPortal::class.java, DataSerializers.BYTE)
        val PORTAL_ID = EntityDataManager.createKey(EntityPortal::class.java, DataSerializers.OPTIONAL_UNIQUE_ID)
        val ALIVE = EntityDataManager.createKey(EntityPortal::class.java, DataSerializers.BOOLEAN)

        fun validateSurface(world: World, pos: BlockPos): Boolean {
            val state = world.getBlockState(pos)
            val block = state.block
            val material = block.getMaterial(state)

            if (!block.isFullBlock(state) || !block.isFullCube(state)) return false
            if (!block.isNormalCube(state)) return false
            if (block.isLeaves(state, world, pos)) return false
            if (!block.isOpaqueCube(state)) return false
            if (block.isTranslucent(state)) return false
            if (!material.isSolid) return false
            if (material.isReplaceable) return false
            if (material == Material.ICE) return false
            if (material == Material.PACKED_ICE) return false
            if (material == Material.PLANTS) return false
            if (material == Material.BARRIER) return false
            if (material == Material.CORAL) return false
            if (material == Material.GOURD) return false
            if (material == Material.CRAFTED_SNOW) return false
            if (material == Material.SPONGE) return false
            if (material == Material.TNT) return false

            return true
        }

        fun getPortalWithID(world: World, portalID: UUID?): EntityPortal? {
            if (portalID == null) return null

            for (entity in world.getEntities(EntityPortal::class.java, EntitySelectors.IS_ALIVE)) {
                if (entity.alive != true) continue
                if (entity.portalID == portalID) {
                    return entity
                }
            }

            return null
        }
    }
}
