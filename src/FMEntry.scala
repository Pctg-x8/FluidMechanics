package com.cterm2.mcfm1710

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event._

import net.minecraft.item.Item
import net.minecraft.init.{Blocks => VanillaBlocks}
import net.minecraft.creativetab.CreativeTabs
import cpw.mods.fml.relauncher.{Side, SideOnly}
import cpw.mods.fml.common.network.NetworkRegistry

@Mod(modid=FMEntry.ID, name=FMEntry.Name, version=FMEntry.Version, modLanguage="scala")
object FMEntry
{
	final val ID = "mcfm1710"
	final val Name = "Fluid Mechanics"
	final val Version = "1.0-alpha"

	val ctab = new CreativeTabs("tabFluidMechanics")
	{
		@SideOnly(Side.CLIENT)
		override def getTabIconItem() = Item.getItemFromBlock(VanillaBlocks.anvil)
	}

	@EventHandler
	def preInit(e: FMLPreInitializationEvent) =
	{
		System.out.println(s"${Name} version ${Version}.")
	}
	@EventHandler
	def init(e: FMLInitializationEvent) =
	{
		Blocks.init(this.ctab)
		Items.init(this.ctab)
		Tiles.init()

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler())
	}
	@EventHandler
	def postInit(e: FMLPostInitializationEvent) =
	{

	}
}
