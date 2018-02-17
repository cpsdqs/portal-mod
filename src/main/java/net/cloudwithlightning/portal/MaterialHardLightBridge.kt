package net.cloudwithlightning.portal

import net.minecraft.block.material.MapColor
import net.minecraft.block.material.Material

class MaterialHardLightBridge(replaceable: Boolean): Material(MapColor.LIGHT_BLUE) {
    init {
        setNoPushMobility()
        if (replaceable) setReplaceable()
        setRequiresTool()
    }

    override fun blocksLight(): Boolean {
        return false
    }

    override fun blocksMovement(): Boolean {
        return false
    }

    override fun isSolid(): Boolean {
        return false
    }
}

