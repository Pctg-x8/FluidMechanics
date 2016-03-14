package com.cterm2.mcfm1710

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod._
import cpw.mods.fml.common.event._

@Mod(modid="com.cterm2.mcfm1710", name="Fluid Mechanics", version="1.0-alpha", modLanguage="scala")
object FMEntry
{
	@EventHandler
	def preInit(e: FMLPreInitializationEvent) =
	{
		System.out.println("Fluid Mechanics version 1.0-alpha.")
	}
	@EventHandler
	def init(e: FMLInitializationEvent) =
	{
	
	}
	@EventHandler
	def postInit(e: FMLPostInitializationEvent) =
	{
		
	}
}
