

class JarData(projectName: String, projetVesrion: String, scalaVersion: String)	{

	val shortScalaVersion = scalaVersion.substring(0, scalaVersion.lastIndexOf("."))		
		
	def fileFolder() = {			
		s"target/scala-${shortScalaVersion}"
	}		

	def fileName() = {
		s"${projectName}_${shortScalaVersion}-${projetVesrion}.jar"
	}	
}	