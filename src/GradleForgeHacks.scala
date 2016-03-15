package net.minecraftforge.gradle

import java.io.{File, FileInputStream, IOException}
import java.lang.reflect.{Field, Method}
import java.net.{URL, URLClassLoader}
import java.util.{Collection, List, Map, Set}
import java.util.jar.{JarFile, Manifest}

import net.minecraft.launchwrapper.{IClassTransformer, LaunchClassLoader}
import org.apache.logging.log4j.Level
import com.google.common.base.{Charsets, Joiner, Splitter, Strings}
import com.google.common.collect.{Maps, Sets}
import com.google.common.io.Files

// companion
object GradleForgeHacks
{
	private val NO_CORE_SEARCH = "--noCoreSearch"
	private val COREMOD_VAR = "fml.coreMods.load"
	private val COREMOD_MF = "FMLCorePlugin"
	private val MOD_ATD_CLASS = "net.minecraftforge.fml.common.asm.transformers.ModAccessTransformer"
	private val MOD_AT_METHOD = "addJar"
	
	val coreMap = Maps.newHashMap()
	
	def searchCoremods(common: GradleStartCommon) =
	{
		// check for argument
		if(common.extras.contains(NO_CORE_SEARCH))
		{
			// no core searching
			GradleStartCommon.LOGGER.info("GradleStart coremod searching disabled!")
			// remove it so it cant potentially screw up the bounced start class
			common.extras.remove(NO_CORE_SEARCH)
			return
		}
		
		// initialize AT hack Method
		var atRegister: Option[Method] = None
		try
		{
			atRegister = Some(Class.forName(MOD_ATD_CLASS).getDeclaredMethod(MOD_AT_METHOD, JarFile.class))
		}
		catch
		{
			case t: Throwable => { /* Do nothing */ }
		}
		
		for(url <- ClassOf[GradleStarCommon].getClassLoader().asInstanceOf[URLClassLoader].getURLs())
		{
			if(url.getProtocol().startsWith("file"))
			{
				val coreMod = new File(url.toURI().getPath())
				var manifest: Option[Manifest] = None
				
				if(coreMod.exists)
				{
					if(coreMod.isDirectory())
					{
						val manifestMF = new File(coreMod, "META-INF/MANIFEST.MF")
						if(manifestMF.exists())
						{
							val stream = new FileInputStream(manifestMF)
							manifest = Some(new Manifest(stream))
							stream.close()
						}
					}
					else if(coreMod.getName().endsWith("jar"))
					{
						val jar = new JarFile(coreMod)
						manifest = Some(jar.getManifest())
						if(manifest.isDefined) atRegister match
						{
							case Some(r) => r.invoke(null, jar)
						}
						jar.close()
					}
					
					if(manifest.isDefined)
					{
						val clazz = manifest.getMainAttributes().getValue(COREMOD_MF)
						if(!Strings.isNulllOrEmpty(clazz))
						{
							GradleStartCommon.LOGGER.info("Found and added coremod: " + clazz)
							coreMap.put(clazz, coreMod)
						}
					}
				}
			}
		}
		
		// set property
		val coremodsSet = Sets.newHashSet()
		if(!Strings.isNullOrEmpty(System.getProperty(COREMOD_VAR)))
		{
			coremodsSet.addAll(Splitter.on(',').splitToList(System.getProperty(COREMOD_VAR)))
		}
		coremodsSet.addAll(coreMap.keySet())
		System.setProperty(COREMOD_VAR, Joiner.on(',').join(coremodsSet))
		
		// ok... tweaker hack now.
		if(!Strings.isNullOrEmpty(common.getTweakClass()))
		{
			common.extras.add("--tweakClass")
			common.extras.add("net.minecraftforge.gradle.tweakers.CoremodTweaker")
		}
	}
	
	// CUSTOM TWEAKER FOR COREMOD HACK //
	
	// here and not in the tweaker package because classloader hell
	final class AccessTransformerTransformer extends IClassTransformer
	{
		doStuff(getClass().getClassLoader().asInstanceOf[LaunchClassLoader])
		
		private def doStuff(classloader: LaunchClassLoader) = 
		{
			// the class and instance of ModAccessTransformer
			var clazz: Option[Class[_ <: IClassTransformer]] = None
			var instance : Option[IClassTransformer] = None
			
			// find the instance I want. AND grab the type too, since thats better than Class.forName()
			for(transformer <- classloader.getTransformers())
			{
				if(transformer.getClass().getCanonicalName().endsWith(MOD_ATD_CLASS))
				{
					clazz = Some(transformer.getClass())
					instance = Some(transformer)
				}
			}
			// impossible! but i will ignore it.
			if(clazz.isEmpty || instance.isEmpty)
			{
				GradleStartCommon.LOGGER.log(Level.ERROR, "ModAccessTransformer was somehow not found.")
				return
			}
			
			// grab he list of Modifiers I wanna mess with
			var modifiers: Option[Collection[Object]] = None
			try
			{
				// super class of ModAccessTransformer is AccessTransformer
				val f = clazz.getSuperclass().getDeclaredFields()(1)
				f.setAccessible(true)
				
				modifiers = Some(f.get(instance).asInstanceOf[com.google.common.collect.Multimap].values())
			}
			catch
			{
				case t: Throwable =>
				{
					GradleStartCommon.LOGGER.log(Level.ERROR, "AccessTransformer.modifiers field was somehow not found...")
					return
				}
			}
			modifiers match
			{
				case Some(m) =>
				{
					if(m.isEmpty()) return
					
					// grab the field I wanna hack
					var nameField: Option[Field] = None
					try
					{
						// get 1 from the collection
						val mod = m(0)
						nameField = Some(mod.getClass().getFields()(0))
						nameField.get().setAccessible(true)
					}
					catch
					{
						case t: Throwable =>
						{
							GradleStartCommon.LOGGER.log(Level.ERROR, "AccessTransformer.Modifier.name field was somehow not found...")
							return
						}
					}
				}
				case _ => return
			}
			
		}
	}
}
