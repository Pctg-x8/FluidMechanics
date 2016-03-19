package com.cterm2.mcfm1710.tiles

import net.minecraft.tileentity.TileEntity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.network.NetworkManager

object EnergyInjectorSynchronizeDataKeys
{
	final val amountKey = "amount"
}
// Energy Injector Tile Entities
final class TEEnergyInjector(val maxFluidAmount: Int = 0) extends TileEntity
{
	import EnergyInjectorSynchronizeDataKeys._

	private var currentFluidAmount = 0

	// Data Synchronizations //
	private def writeSynchronizeDataToNBT(tag: NBTTagCompound) =
	{
		tag.setInteger(amountKey, this.currentFluidAmount)
	}
	private def readSynchronizeDataFromNBT(tag: NBTTagCompound) =
	{
		this.currentFluidAmount = tag.getInteger(amountKey)
	}
	override def writeToNBT(tag: NBTTagCompound) =
	{
		super.writeToNBT(tag)
		this.writeSynchronizeDataToNBT(tag)
	}
	override def readFromNBT(tag: NBTTagCompound) =
	{
		super.readFromNBT(tag)
		this.readSynchronizeDataFromNBT(tag)
	}
	override def getDescriptionPacket() =
	{
		val tag = new NBTTagCompound
		this.writeSynchronizeDataToNBT(tag)
		new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag)
	}
	override def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity) =
		{ _: Unit => packet.func_148857_g() } andThen { this.readSynchronizeDataFromNBT(_) }
}
