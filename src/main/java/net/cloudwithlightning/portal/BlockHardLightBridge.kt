package net.cloudwithlightning.portal

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.particle.ParticleManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.pathfinding.PathNodeType
import net.minecraft.util.*
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

class BlockHardLightBridge(): Block(MaterialHardLightBridge(true)) {
    init {
        unlocalizedName = "hard_light_bridge"
        setRegistryName("hard_light_bridge")
        blockResistance = 1e100f
        setBlockUnbreakable()
        setLightLevel(0.5f)
        translucent = true
        setLightOpacity(1)
        blockSoundType = hardLightBridgeSoundType

        defaultState = this.blockState.baseState.withProperty(DIRECTION, EnumFacing.SOUTH)
    }

    fun copy(state: IBlockState, from: IBlockState): IBlockState {
        return state
                .withProperty(DIRECTION, from.getValue(DIRECTION))
    }

    fun createStateWith(direction: EnumFacing): IBlockState {
        return defaultState.withProperty(DIRECTION, direction)
    }

    fun isEqualState(state: IBlockState, other: IBlockState): Boolean {
        if (state.block !is BlockHardLightBridge || other.block !is BlockHardLightBridge) return false
        return state.getValue(DIRECTION) == other.getValue(DIRECTION)
    }

    fun propagateOne(world: World, pos: BlockPos, overrideSource: Boolean?): Pair<Boolean, Boolean> {
        val state = world.getBlockState(pos)
        if (state.block !is BlockHardLightBridge) return Pair(false, false)

        val onePropDir = state.getValue(DIRECTION)

        val next = pos.offset(onePropDir)
        val needsNext = world.isAirBlock(next)
        val prev = pos.offset(onePropDir, -1)
        val prevState = world.getBlockState(prev)

        val hardLightBridgeGenerator = Block.getBlockFromName("clportal:hard_light_bridge_generator") as BlockHardLightBridgeGenerator

        val prevIsSource = if (overrideSource == null) {
            hardLightBridgeGenerator.isPowered(prevState)
        } else {
            overrideSource
        }
        val hasPrev = prevIsSource || isEqualState(state, prevState)

        if (!hasPrev) {
            world.setBlockToAir(pos)
            return Pair(true, false)
        } else if (needsNext) {
            world.setBlockState(next, copy(defaultState, state))
            return Pair(true, true)
        }

        return Pair(false, false)
    }

    fun propagate(world: World, pos: BlockPos, overrideSource: Boolean?) {
        var blocks = 1
        var first = true
        var position = pos
        var overrideVal = overrideSource
        val direction = world.getBlockState(pos).getValue(DIRECTION)
        while (blocks > 0) {
            val (doNext, positive) = propagateOne(world, position, overrideVal)
            overrideVal = null
            blocks -= 1
            if (first) {
                first = false
                if (!positive) blocks += 3 // remove three per tick
            }
            position = position.offset(direction)

            if (!doNext) {
                break
            } else if (blocks == 0) {
                world.scheduleUpdate(position, this, 1)
            }
        }
    }

    fun propagate(world: World, pos: BlockPos) {
        propagate(world, pos, null)
    }

    override fun onEntityCollidedWithBlock(worldIn: World?, pos: BlockPos?, state: IBlockState?, entityIn: Entity?) {
        if (!worldIn!!.isRemote) propagate(worldIn!!, pos!!)
    }

    override fun updateTick(worldIn: World?, pos: BlockPos?, state: IBlockState?, rand: Random?) {
        if (!worldIn!!.isRemote) propagate(worldIn!!, pos!!)
    }

    override fun neighborChanged(state: IBlockState?, worldIn: World?, pos: BlockPos?, blockIn: Block?, fromPos: BlockPos?) {
        if (!worldIn!!.isRemote) {
            worldIn.scheduleUpdate(pos, this, 1)
        }
    }

    override fun isReplaceable(worldIn: IBlockAccess?, pos: BlockPos?): Boolean {
        return true
    }

    override fun canHarvestBlock(world: IBlockAccess?, pos: BlockPos?, player: EntityPlayer?): Boolean {
        return false
    }

    override fun removedByPlayer(state: IBlockState?, world: World?, pos: BlockPos?, player: EntityPlayer?, willHarvest: Boolean): Boolean {
        return false
    }

    override fun getFlammability(world: IBlockAccess?, pos: BlockPos?, face: EnumFacing?): Int {
        return 0
    }

    override fun canBeReplacedByLeaves(state: IBlockState?, world: IBlockAccess?, pos: BlockPos?): Boolean {
        return true
    }

    override fun addLandingEffects(state: IBlockState?, world: WorldServer?, blockPosition: BlockPos?, iblockstate: IBlockState?, entity: EntityLivingBase?, numberOfParticles: Int): Boolean {
        super.addLandingEffects(state, world, blockPosition, iblockstate, entity, numberOfParticles)
        // TODO: flash
        return true
    }

    override fun onLanded(worldIn: World?, entityIn: Entity?) {
        if (entityIn!!.motionY < -.3f) {
            val volume = Math.min(1f, entityIn.height / 2f) // rough estimate of entity size
            worldIn!!.playSound(null, entityIn.posX, entityIn.posY, entityIn.posZ, hardLightLand,
                    SoundCategory.NEUTRAL, volume, 1f)
        }
        super.onLanded(worldIn, entityIn)
    }

    override fun addHitEffects(state: IBlockState?, worldObj: World?, target: RayTraceResult?, manager: ParticleManager?): Boolean {
        return true
    }

    override fun addDestroyEffects(world: World?, pos: BlockPos?, manager: ParticleManager?): Boolean {
        return true
    }

    override fun canEntityDestroy(state: IBlockState?, world: IBlockAccess?, pos: BlockPos?, entity: Entity?): Boolean {
        return false
    }

    override fun getBoundingBox(state: IBlockState?, source: IBlockAccess?, pos: BlockPos?): AxisAlignedBB {
        return when (state!!.getValue(DIRECTION)) {
            EnumFacing.DOWN -> BB_NORTH
            EnumFacing.UP -> BB_SOUTH
            else -> BB_DOWN
        }
    }

    override fun createBlockState(): BlockStateContainer {
        return BlockStateContainer(this, DIRECTION)
    }

    override fun getMetaFromState(state: IBlockState?): Int {
        return state!!.getValue(DIRECTION).index
    }

    override fun getStateFromMeta(meta: Int): IBlockState {
        return defaultState
                .withProperty(DIRECTION, EnumFacing.values()[meta and 0x7])
    }

    override fun isOpaqueCube(state: IBlockState?): Boolean {
        return false
    }

    @SideOnly(Side.CLIENT)
    override fun getBlockLayer(): BlockRenderLayer {
        return BlockRenderLayer.TRANSLUCENT
    }

    override fun getAiPathNodeType(state: IBlockState?, world: IBlockAccess?, pos: BlockPos?): PathNodeType? {
        return PathNodeType.WALKABLE
    }

    companion object {
        val BB_DOWN = AxisAlignedBB(0.0, 3.0 / 16, 0.0, 1.0, 4.0 / 16, 1.0)
        val BB_SOUTH = AxisAlignedBB(0.0, 0.0, 1.0 - 4.0 / 16, 1.0, 1.0, 1.0 - 3.0 / 16)
        val BB_NORTH = AxisAlignedBB(0.0, 0.0, 3.0 / 16, 1.0, 1.0, 4.0 / 16)

        val DIRECTION: PropertyDirection = PropertyDirection.create("direction")

        val nullSound = SoundEvent(ResourceLocation(PortalMod.MODID, "null_sound"))
        val hardLightStep = SoundEvent(ResourceLocation(PortalMod.MODID, "light_bridge_step"))
        val hardLightLand = SoundEvent(ResourceLocation(PortalMod.MODID, "light_bridge_land"))

        val hardLightBridgeSoundType = SoundType(
                1f,
                1f,
                nullSound,
                hardLightStep,
                nullSound,
                nullSound,
                hardLightLand
        )
    }
}

