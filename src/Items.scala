package com.cterm2.mcfm1710

import com.cterm2.mcfm1710.items._
import net.minecraft.creativetab.CreativeTabs
import cpw.mods.fml.common.registry.GameRegistry

object Items
{
	lazy val asmTable = new ItemAssemblyTable
	
	def init(ctab: CreativeTabs) =
	{
		System.out.println("Init Items...")
		
		GameRegistry.registerItem(this.asmTable.setCreativeTab(ctab), "asmTable")
	}
}
