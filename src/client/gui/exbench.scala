package com.cterm2.mcfm1710.client.gui

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import com.cterm2.mcfm1710.containers.ContainerAssemblyTable
import com.cterm2.mcfm1710.tiles.TEAssemblyTable
import net.minecraft.util.ResourceLocation
import com.cterm2.mcfm1710.FMEntry
import com.cterm2.mcfm1710.utils.LocalTranslationUtils._

class GuiAssemblyTable(val con: ContainerAssemblyTable) extends GuiContainer(con)
{
	val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/asmTable.png")

	// initializer
	override def initGui() =
	{
		super.initGui()

		this.xSize = 252; this.ySize = 170
		this.guiLeft = (this.width - this.xSize) / 2
		this.guiTop = (this.height - this.ySize) / 2
	}

	// Draw GUI Foreground Layer(caption layer)
	override protected def drawGuiContainerForegroundLayer(p1: Int, p2: Int) =
	{
		this.fontRendererObj.drawString(t"container.crafting", 8, 10, 0x404040)
		this.fontRendererObj.drawString(t"container.inventory", 45, 80, 0x404040)
		this.fontRendererObj.drawString(t"container.assembling", 140, 6, 0x404040)
	}

	// Draw GUI Background Layer(backimage layer)
	override protected def drawGuiContainerBackgroundLayer(p1: Float, p2: Int, p3: Int) =
	{
		import org.lwjgl.opengl.GL11._

		this.mc.getTextureManager.bindTexture(this.backImage)
		glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
	}
}
