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
			case t: AssemblyTable.TileEntity => new AssemblyTable.Container(world, t, player.inventory)
			case t: ThermalGenerator.TileEntity => new ThermalGenerator.Container(t, player.inventory)
			case t: ThermalFluidGenerator.TileEntity => new ThermalFluidGenerator.Container(t, player.inventory)
		}
	// Returns and instance of the Gui you made earlier
	override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
		world.getTileEntity(x, y, z) match
		{
			case t: AssemblyTable.TileEntity => new AssemblyTable.Gui(new AssemblyTable.Container(world, t, player.inventory))
			case t: ThermalGenerator.TileEntity => new ThermalGenerator.Gui(new ThermalGenerator.Container(t, player.inventory))
			case t: ThermalFluidGenerator.TileEntity => new ThermalFluidGenerator.Gui(new ThermalFluidGenerator.Container(t, player.inventory))
		}
}
