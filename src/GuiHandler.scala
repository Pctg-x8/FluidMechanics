package com.cterm2.mcfm1710

import cpw.mods.fml.common.network.IGuiHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

final class GuiHandler extends IGuiHandler
{
	// Returns an instance of the Container you made earlier
	override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
		world.getTileEntity(x, y, z) match
		{
			case t: assemblyTable.TileEntity => new assemblyTable.Container(world, t, player.inventory)
		}
	// Returns and instance of the Gui you made earlier
	override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
		world.getTileEntity(x, y, z) match
		{
			case t: assemblyTable.TileEntity => new assemblyTable.Gui(new assemblyTable.Container(world, t, player.inventory))
		}
}
