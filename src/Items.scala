package com.cterm2.mcfm1710

import net.minecraft.creativetab.CreativeTabs
import cpw.mods.fml.common.registry.GameRegistry

object Items
{
	def init(ctab: CreativeTabs) =
	{
		System.out.println("Init Items...")

		GameRegistry.registerItem(assemblyTable.Item.setCreativeTab(ctab), "asmTable")
	}
}
