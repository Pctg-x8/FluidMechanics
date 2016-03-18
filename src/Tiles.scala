package com.cterm2.mcfm1710

import cpw.mods.fml.common.registry.GameRegistry
import com.cterm2.mcfm1710.tiles.{TEAssemblyTable, TEEnergyInjector}

object Tiles
{
	def init() =
	{
		GameRegistry.registerTileEntity(classOf[TEAssemblyTable], "TEAssemblyTable")
		GameRegistry.registerTileEntity(classOf[TEEnergyInjector], "TEEnergyInjector")
	}
}
