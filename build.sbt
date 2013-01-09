import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "heyhamont-twitterbot"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq("org.twitter4j" % "twitter4j-core" % "3.0.2", "org.twitter4j" % "twitter4j-stream" % "3.0.2")

mainClass in Compile := Some("com.mrsjstudios.heyhamont.TwitterBotExecutor")



