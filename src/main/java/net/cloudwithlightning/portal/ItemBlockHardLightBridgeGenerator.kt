package net.cloudwithlightning.portal

import net.minecraft.item.ItemBlock
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly


class ItemBlockHardLightBridgeGenerator(type: BlockHardLightBridgeGenerator): ItemBlock(type) {
    init {
        unlocalizedName = "hard_light_bridge_generator"
        setRegistryName("hard_light_bridge_generator")
    }

    @SideOnly(Side.CLIENT)
    fun registerModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!,
                "inventory"))
    }
}
