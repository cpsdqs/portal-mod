package net.cloudwithlightning.portal

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

class BlockHardLightBridgeGenerator(): Block(MaterialHardLightBridge(false)) {
    init {
        unlocalizedName = "hard_light_bridge_generator"
        setRegistryName("hard_light_bridge_generator")
        setHardness(0.8f)
        setLightOpacity(1)
        translucent = true
        blockSoundType = SoundType.METAL

        setHarvestLevel("pickaxe", 1)

        defaultState = this.blockState.baseState
                .withProperty(DIRECTION, EnumFacing.SOUTH)
                .withProperty(POWERED, false)
    }

    fun updateBridge(worldIn: World, pos: BlockPos, state: IBlockState): Boolean {
        if (worldIn!!.isRemote) return true
        val direction = state!!.getValue(DIRECTION)
        val powered = state.getValue(POWERED)

        val nextBlockPos = pos.offset(direction)
        val hardLightBridge = Block.getBlockFromName("clportal:hard_light_bridge") as BlockHardLightBridge

        if (powered && worldIn.isAirBlock(nextBlockPos)) {
            worldIn.setBlockState(nextBlockPos, hardLightBridge.createStateWith(direction))
            hardLightBridge.propagate(worldIn, nextBlockPos, powered)
            return true
        } else if (!powered && worldIn.getBlockState(nextBlockPos).block is BlockHardLightBridge) {
            hardLightBridge.propagate(worldIn, nextBlockPos, powered)
            return true
        }
        return false
    }

    fun isPowered(state: IBlockState): Boolean {
        if (state.block !is BlockHardLightBridgeGenerator) return false
        return state.getValue(POWERED)
    }

    private fun updatePowered(worldIn: World, pos: BlockPos, state: IBlockState): Boolean {
        if (worldIn.isRemote) return false
        val powered = state.getValue(POWERED)
        val sidePowered = worldIn.isBlockPowered(pos)
        if (powered && !sidePowered) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, false))
        } else if (!powered && sidePowered) {
            worldIn.setBlockState(pos, state.withProperty(POWERED, true))
            return true
        }
        return false
    }

    override fun onBlockAdded(worldIn: World?, pos: BlockPos?, state: IBlockState?) {
        if (updatePowered(worldIn!!, pos!!, state!!)) {
            worldIn.scheduleUpdate(pos, this, 1)
        }
    }

    override fun neighborChanged(state: IBlockState?, worldIn: World?, pos: BlockPos?, blockIn: Block?, fromPos: BlockPos?) {
        updatePowered(worldIn!!, pos!!, state!!)
        worldIn.scheduleUpdate(pos, this, 1)
    }

    override fun updateTick(worldIn: World?, pos: BlockPos?, state: IBlockState?, rand: Random?) {
        if (worldIn!!.isRemote) return
        updateBridge(worldIn, pos!!, state!!)
    }

    override fun createBlockState(): BlockStateContainer {
        return BlockStateContainer(this, DIRECTION, POWERED)
    }

    override fun getMetaFromState(state: IBlockState?): Int {
        return state!!.getValue(DIRECTION).index or if (state!!.getValue(POWERED)) { 8 } else { 0 }
    }

    override fun getStateFromMeta(meta: Int): IBlockState {
        return defaultState
                .withProperty(DIRECTION, EnumFacing.values()[meta and 0x7])
                .withProperty(POWERED, (meta and 0x8) > 0)
    }

    override fun getBoundingBox(state: IBlockState?, source: IBlockAccess?, pos: BlockPos?): AxisAlignedBB {
        state!!
        if (!state.getValue(POWERED)) {
            return when (state.getValue(DIRECTION)) {
                EnumFacing.SOUTH -> BB_IDLE_SOUTH
                EnumFacing.NORTH -> BB_IDLE_NORTH
                EnumFacing.EAST -> BB_IDLE_EAST
                EnumFacing.WEST -> BB_IDLE_WEST
                EnumFacing.UP -> BB_IDLE_UP
                EnumFacing.DOWN -> BB_IDLE_DOWN
            }
        }
        return when (state.getValue(DIRECTION)) {
            EnumFacing.DOWN -> BB_NORTH
            EnumFacing.UP -> BB_SOUTH
            else -> BB_DOWN
        }
    }

    override fun isNormalCube(state: IBlockState?, world: IBlockAccess?, pos: BlockPos?): Boolean {
        return false
    }

    override fun isOpaqueCube(state: IBlockState?): Boolean {
        return false
    }

    @SideOnly(Side.CLIENT)
    override fun getBlockLayer(): BlockRenderLayer {
        return BlockRenderLayer.TRANSLUCENT
    }

    override fun canConnectRedstone(state: IBlockState?, world: IBlockAccess?, pos: BlockPos?, side: EnumFacing?): Boolean {
        val direction = state!!.getValue(DIRECTION)
        return side == direction.opposite
    }

    override fun getStateForPlacement(world: World?, pos: BlockPos?, facing: EnumFacing?, hitX: Float, hitY: Float, hitZ: Float, meta: Int, placer: EntityLivingBase?, hand: EnumHand?): IBlockState {
        return defaultState
                .withProperty(DIRECTION, facing)
    }

    override fun getCreativeTabToDisplayOn(): CreativeTabs {
        return CreativeTabs.REDSTONE
    }

    companion object {
        val BB_DOWN = BlockHardLightBridge.BB_DOWN
        val BB_SOUTH = BlockHardLightBridge.BB_SOUTH
        val BB_NORTH = BlockHardLightBridge.BB_NORTH
        val BB_IDLE_SOUTH = AxisAlignedBB(-1.0 / 16, 1.0 / 16, 0.0, 17.0 / 16, 6.0 / 16, 6.0 / 16)
        val BB_IDLE_EAST = AxisAlignedBB(0.0, 1.0 / 16, -1.0 / 16, 6.0 / 16, 6.0 / 16, 17.0 / 16)
        val BB_IDLE_WEST = AxisAlignedBB(10.0 / 16, 1.0 / 16, -1.0 / 16, 1.0, 6.0 / 16, 17.0 / 16)
        val BB_IDLE_NORTH = AxisAlignedBB(-1.0 / 16, 1.0 / 16, 10.0 / 16, 17.0 / 16, 6.0 / 16, 1.0)
        val BB_IDLE_DOWN = AxisAlignedBB(-1.0 / 16, 10.0 / 16, 1.0 / 16, 17.0 / 16, 1.0, 6.0 / 16)
        val BB_IDLE_UP = AxisAlignedBB(-1.0 / 16, 0.0, 10.0 / 16, 17.0 / 16, 6.0 / 16, 15.0 / 16)

        val DIRECTION: PropertyDirection = PropertyDirection.create("direction")
        val POWERED: PropertyBool = PropertyBool.create("powered")
    }
}
