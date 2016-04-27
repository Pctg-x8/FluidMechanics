package com.cterm2.mcfm1710

package base
{
	import net.minecraft.tileentity.TileEntity
	import net.minecraft.nbt.NBTTagCompound
	import net.minecraft.network.NetworkManager, net.minecraft.network.play.server.S35PacketUpdateTileEntity

	/// Provides automatic implementation of writeToNBT/readFromNBT/onDataPacket/getDescriptionPacket
	/// via implementing storeNetworkData/loadNetworkData
	abstract class NetworkTileEntity extends TileEntity
	{
		def storePacketData(tag: NBTTagCompound): NBTTagCompound
		def loadPacketData(tag: NBTTagCompound): Unit

		override final def writeToNBT(tag: NBTTagCompound)
		{
			super.writeToNBT(tag)
			this.storePacketData(tag)
		}
		override final def readFromNBT(tag: NBTTagCompound)
		{
			super.readFromNBT(tag)
			this.loadPacketData(tag)
		}
		override final def getDescriptionPacket =
			this.storePacketData _ andThen { new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, _) } apply new NBTTagCompound
		override final def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity)
		{
			{ (_: S35PacketUpdateTileEntity).func_148857_g } andThen this.loadPacketData apply packet
		}
	}
}
