ThisBuild / scalaVersion := "3.5.1"

lazy val commonSettings = Seq(
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.18",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % "test",
  libraryDependencies += "io.github.mkpaz" % "atlantafx-base" % "2.0.1",
  libraryDependencies += "org.scalafx" %% "scalafx" % "22.0.0-R33",
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
  libraryDependencies += "org.playframework" %% "play-json" % "3.1.0-M1",
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
  },
  libraryDependencies += guice,
  coverageEnabled := true,
  coverageFailOnMinimum := true,
  coverageMinimumStmtTotal := 85,
  coverageMinimumBranchTotal := 100
)

lazy val knockoutwhist = project.in(file("knockoutwhist"))
  .settings(
    commonSettings,
    mainClass := Some("de.knockoutwhist.KnockOutWhist"),
    coverageExcludedPackages := "de.knockoutwhist.ui.*;de.knockoutwhist.utils.gui.*"
  )

lazy val knockoutwhistweb = project.in(file("knockoutwhistweb"))
  .enablePlugins(PlayScala)
  .dependsOn(knockoutwhist % "compile->compile;test->test")
  .settings(
    commonSettings,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
    libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.12",
    libraryDependencies += "com.auth0" % "java-jwt" % "4.3.0",
    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "3.2.2"
  )

lazy val root = (project in file("."))
  .aggregate(knockoutwhist, knockoutwhistweb)
  .settings(
    name := "KnockOutWhistWeb"
  )
