package com.cterm2.mcfm1710.blocks

import com.cterm2.mcfm1710.FMEntry
import java.util.Random
import net.minecraft.block.{Block, BlockContainer}, net.minecraft.item.Item
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import com.cterm2.mcfm1710.utils.{BlockPlacementInfo, Vector3i}
import com.cterm2.mcfm1710.utils.ForgeDirectionExtensions._
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.item.ItemStack, net.minecraft.world.World
import com.cterm2.mcfm1710.Items, com.cterm2.mcfm1710.tiles.TEAssemblyTable
import cpw.mods.fml.relauncher.{SideOnly, Side}

// companion
object BlockAssemblyTablePart
{
	// MetaBits //
	final val SlaveBit = 0x08

	// Utilities //
	final def canPlaceBlock(placeinfo: BlockPlacementInfo, side: ForgeDirection, stack: ItemStack) =
		placeinfo.player.canPlayerEdit(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z, side.ordinal(), stack) &&
		placeinfo.world.isAirBlock(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z) &&
		World.doesBlockHaveSolidTopSurface(placeinfo.world, placeinfo.pos.x, placeinfo.pos.y - 1, placeinfo.pos.z)
}
final class BlockAssemblyTablePart extends BlockContainer(Material.rock)
{
	setHardness(1.0f)
	applyBlockBounds()

	// Setup Block Bounds(Size)
	private def applyBlockBounds() = this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.5f, 1.0f)

	// Render Settings //
	// render as normal(solid cube) block flag
	@SideOnly(Side.CLIENT)
	override val renderAsNormalBlock = false
	// render as opaque(no transparency) block flag
	override val isOpaqueCube = false

	// Block Actions //
	// Called when block is right-clicked(activated)
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
		if(world.isRemote) true else
		{
			val meta = world.getBlockMetadata(x, y, z)
			if(!this.isMaster(meta))
			{
				// Slave: Delegate process to master
				val masterPos = Vector3i(x, y, z) withOffset this.getDirectionOffset(meta).opposite
				val masterMeta = world.getBlockMetadata(masterPos.x, masterPos.y, masterPos.z)
				this.onBlockActivated(world, masterPos.x, masterPos.y, masterPos.z, player, masterMeta, xo, yo, zo)
			}
			else
			{
				// Master: Open Table's GUI
				System.out.println(s"Open Table's GUI on Coordinate (${x}, ${y}, ${z})")
				val entity = world.getTileEntity(x, y, z).asInstanceOf[TEAssemblyTable]
				System.out.println(s"TileEntity associated coordinate: ${entity}")
				if(entity != null) player.openGui(FMEntry, 0, world, x, y, z)
				true
			}
		}
	// Called when neighbor block changed(broken, placed new or pushed by pistons, passed coordinate is mine)
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: Block) =
	{
		val meta = world.getBlockMetadata(x, y, z)
		val dir = this.getDirectionOffset(meta)

		if(this.isMaster(meta))
		{
			// this is master block
			if(world.getBlock(x + dir.x, y, z + dir.z) != this)
			{
				// slave changed -> break master too
				world.setBlockToAir(x, y, z)
			}
		}
		else if(world.getBlock(x - dir.x, y, z - dir.z) != this)
		{
			// master changed -> break slave too and drop item if this is client
			world.setBlockToAir(x, y, z)
			if(!world.isRemote) this.dropBlockAsItem(world, x, y, z, meta, 0)
		}
	}
	// Called when the block is attempted to be harvested
	override def onBlockHarvested(world: World, x: Int, y: Int, z: Int, meta: Int, player: EntityPlayer) =
		if(player.capabilities.isCreativeMode && this.isMaster(meta))
		{
			// on master
			val dir = this.getDirectionOffset(meta)
			val slavePos = Vector3i(x, y, z) withOffset dir
			if(world.getBlock(slavePos.x, slavePos.y, slavePos.z) == this)
			{
				// break slave too
				world.setBlockToAir(slavePos.x, slavePos.y, slavePos.z)
			}
		}
	// Called when the block is broken
	override def breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, meta: Int) =
	{
		val entity = world.getTileEntity(x, y, z).asInstanceOf[TEAssemblyTable]
		if(entity != null) entity.dropItemsAtRandom(world, x, y, z)
	}

	// Configures //
	// Configure dropped item
	override def getItemDropped(meta: Int, randomizer: Random, quantity: Int) =
		if(this.isMaster(meta)) Item.getItemById(0) else Items.asmTable
	// Drops the block items with a specified chance of dropping the specified items(only from slave block)
	override def dropBlockAsItemWithChance(world: World, x: Int, y: Int, z: Int, meta: Int, f1: Float, i4: Int) =
		if(!this.isMaster(meta)) super.dropBlockAsItemWithChance(world, x, y, z, meta, f1, 0)
	// Set as immobility
	override val getMobilityFlag = 2
	// Gets an item for the block being called on
	@SideOnly(Side.CLIENT)
	override def getItem(world: World, x: Int, y: Int, z: Int) = Items.asmTable
	// TileEntity associated to block(have only in master)
	override def createNewTileEntity(world: World, meta: Int) = if(this.isMaster(meta)) new TEAssemblyTable else null

	// Miscs //
	// Determine this block is master(primary) block
	private final def isMaster(meta: Int) = (meta & 0x08) == 0
	// Get direction from metadata
	private final def getDirectionOffset(meta: Int) = ForgeDirection.values()(meta & 0x07).offset
}
