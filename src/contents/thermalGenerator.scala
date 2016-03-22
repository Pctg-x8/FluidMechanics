package com.cterm2.mcfm1710

import interfaces.ISourceGenerator

package thermalGenerator
{
    import net.minecraft.world.World
    import net.minecraft.tileentity.{TileEntity => TileEntityBase}
    import net.minecraft.nbt.NBTTagCompound

    final object Block extends sourceGenerator.BlockBase
    {
        // Block Traits //
        override def createNewTileEntity(world: World, meta: Int) = new TileEntity
    }

    final class TileEntity extends TileEntityBase with ISourceGenerator
    {
        override def storeSpecificData(tag: NBTTagCompound) =
        {

        }
        override def loadSpecificData(tag: NBTTagCompound) =
        {

        }
    }
}
