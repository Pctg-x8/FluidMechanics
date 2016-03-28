package com.cterm2.mcfm1710.utils

// Active Record for Minecraft NBT

object ActiveNBTRecord
{
	import net.minecraft.nbt.NBTTagCompound

	implicit class Record(val tag: NBTTagCompound) extends AnyVal
	{
		// Setters
		def update(key: String, value: Byte) = tag.setByte(key, value)
		def update(key: String, value: Short) = tag.setShort(key, value)
		def update(key: String, value: Int) = tag.setInteger(key, value)
		def update(key: String, value: NBTTagCompound) = tag.setTag(key, value)
		// Getters
		def apply[T](key: String)(implicit getterImpl: GetterImpl[T]) = getterImpl.get(tag, key)
	}

	sealed trait GetterImpl[@specialized T]
	{
		def get(tag: NBTTagCompound, key: String): Option[T]
	}
	implicit object ByteGetter extends GetterImpl[Byte]
	{
		override def get(tag: NBTTagCompound, key: String) = if(tag.hasKey(key)) Some(tag.getByte(key)) else None
	}
	implicit object ShortGetter extends GetterImpl[Short]
	{
		override def get(tag: NBTTagCompound, key: String) = if(tag.hasKey(key)) Some(tag.getShort(key)) else None
	}
	implicit object IntGetter extends GetterImpl[Int]
	{
		override def get(tag: NBTTagCompound, key: String) = if(tag.hasKey(key)) Some(tag.getInteger(key)) else None
	}
	implicit object TagGetter extends GetterImpl[NBTTagCompound]
	{
		override def get(tag: NBTTagCompound, key: String) = Option(tag.getTag(key).asInstanceOf[NBTTagCompound])
	}
}
