package com.cterm2.mcfm1710

import cpw.mods.fml.common.registry.GameRegistry

object Tiles
{
	def init() =
	{
		GameRegistry.registerTileEntity(classOf[assemblyTable.TileEntity], "TEAssemblyTable")
		GameRegistry.registerTileEntity(classOf[energyInjector.TEModuled], "TEEnergyInjectorModule")
		GameRegistry.registerTileEntity(classOf[energyInjector.TEStandalone], "TEEnergyInjector")
		GameRegistry.registerTileEntity(classOf[thermalGenerator.TileEntity], "TEThermalGenerator")
	}
}
