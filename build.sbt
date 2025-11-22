ThisBuild / scalaVersion := "3.5.1"

lazy val commonSettings = Seq(
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.19",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  libraryDependencies += "io.github.mkpaz" % "atlantafx-base" % "2.1.0",
  libraryDependencies += "org.scalafx" %% "scalafx" % "24.0.2-R36",
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.4.0",
  libraryDependencies += "org.playframework" %% "play-json" % "3.1.0-M9",
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
    libraryDependencies += "com.auth0" % "java-jwt" % "4.5.0",
    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "3.2.3",
    libraryDependencies += "tools.jackson.module" %% "jackson-module-scala" % "3.0.2",
    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
  )

lazy val root = (project in file("."))
  .aggregate(knockoutwhist, knockoutwhistweb)
  .settings(
    name := "KnockOutWhistWeb"
  )
