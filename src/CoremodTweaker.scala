package net.minecraftforge.gradle.tweakers

import java.io.File
import java.lang.reflect.{Constructor, Field}
import java.util.{List, Map}

import net.minecraft.launchwrapper.{ITweaker, Launch, LaunchClassLoader}
import net.minecraftforge.gradle.GradleForgeHacks

import org.apache.logging.log4j.{Level, LogManager, Logger}

// companion
object CoremodTweaker
{
	protected val LOGGER = LogManager.getLogger("GradleStart")
	protected val COREMOD_CLASS = "net.minecraftforge.fml.relauncher.CoreModManager"
	protected val TWEAKER_SORT_FIELD = "tweakSorting"
}
class CoremodTweaker extends ITweaker
{
	def injectIntoClassLoader(classLoader: LaunchClassLoader) =
	{
		try
		{
			var coreModList = Class.forName("net.minecraftforge.fml.relauncher.CoreModManager", true, classLoader).getDeclaredField("loadPlugins")
			coreModList.setAccessible(true)
			
			// grab constructor
			val clazz = Class.forName("net.minecraftforge.fml.relauncher.CoreModManager$FMLPluginWrapper", true, classLoader).asInstanceOf[Class[ITweaker]]
			val construct = clazz.getConstructors()(0).asInstanceOf[Constructor[ITweaker]]
			construct.setAccessible(true)
			
			val fields = clazz.getDeclaredFields()
			val pluginField = fields(1)
			val fileField = fields(3)
			val listField = fields(2)
			
			Field.setAccessible(clazz.getConstructors(), true)
			Field.setAccessible(fields, true)
			
			val oldList = coreModList.get(null).asInstanceOf[List[ITweaker]]
			
			for(i <- 0 to oldList.size())
			{
				val tweaker = oldList.get(i)
				
				if(clazz.isInstance(tweaker))
				{
					val coreMod = pluginField.get(tweaker)
					val oldFile = fileField.get(tweaker)
					val newFile = GradleForgeHacks.coreMap.get(coreMod.getClass().getCanonicalName())
					
					LOGGER.info("Injecting location in coremod {}", coreMod.getClass().getCanonicalName())
					
					if(newFile != null && oldFile != null)
					{
						// build new tweaker
						oldList.set(i, construct.newInstance(new Array[Object]( 
							fields(0).get(tweaker).asInstanceOf[String],	// name
							coreMod,										// coremod
							newFile,										// location
							fields(4).getInt(tweaker),						// sort index?
							listField.get(tweaker).asInstanceOf[List[String]].toArray(new Array[String](0))
						)))
					}
				}
			}
		}
		catch
		{
			case t: Throwable =>
			{
				LOGGER.log(Level.ERROR, "Something went wrong with the coremod adding.")
				t.printStackTrace()
			}
		}
		
		// inject the additional AT tweaker
		val atTweaker = "net.minecraftforge.gradle.tweakers.AccessTransformerTweaker";
		Launch.blackboard.get("TweakClass").asInstanceOf[List[String]].add(atTweaker);
		
		try
		{
			val f = Class.forName(COREMOD_CLASS, true, classLoader).getDeclaredField(TWEAKER_SORT_FIELD)
			f.setAccessible(true)
			f.get(null).asInstanceOf[Map[String, Integer]].put(atTweaker, Integer.valueOf(1001))
		}
		catch
		{
			case t: Throwable =>
			{
				LOGGER.log(Level.ERROR, "Something went wrong with the coremod the AT tweaker adding.")
				t.printStackTrace()
			}
		}
	}
	
	// @formatter:off
	override def getLaunchTarget(): String = null
	override def getLaunchArguments() = new Array[String](0);
	override def acceptOptions(args: List[String], gameDir: File, assetsDir: File, profile: String) = {}
}
