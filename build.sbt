name := """KnockOutWhist-Web"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  Compile / unmanagedSourceDirectories += baseDirectory.value / "knockoutwhist",
  Test / unmanagedSourceDirectories += baseDirectory.value / "knockoutwhist"
)


scalaVersion := "3.5.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
)

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.18"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % "test"
libraryDependencies +="io.github.mkpaz" % "atlantafx-base" % "2.0.1"
libraryDependencies += "org.scalafx" %% "scalafx" % "22.0.0-R33"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.3.0"
libraryDependencies += "org.playframework" %% "play-json" % "3.1.0-M1"

libraryDependencies ++= {
  // Determine OS version of JavaFX binaries
  lazy val osName = System.getProperty("os.name") match {
    case n if n.startsWith("Linux") => "linux"
    case n if n.startsWith("Mac") => "mac"
    case n if n.startsWith("Windows") => "win"
    case _ => throw new Exception("Unknown platform!")
  }
  Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
    .map(m => "org.openjfx" % s"javafx-$m" % "21" classifier osName)
}

libraryDependencies += "net.codingwell" %% "scala-guice" % "7.0.0"

Test / testOptions += Tests.Filter(_.equals("de.knockoutwhist.TestSequence"))

coverageEnabled := true
coverageFailOnMinimum := true
coverageMinimumStmtTotal := 85
coverageMinimumBranchTotal := 100

