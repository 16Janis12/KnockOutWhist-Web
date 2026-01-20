ThisBuild / scalaVersion := "3.5.1"

credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  sys.env.getOrElse("GITHUB_USER", sys.error("GITHUB_USER not set")),
  sys.env.getOrElse("GITHUB_TOKEN", sys.error("GITHUB_TOKEN not set"))
)

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

    resolvers += "GitHub Packages" at "https://maven.pkg.github.com/16Janis12/KnockOutWhist-Web",

    commonSettings,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
    libraryDependencies += "de.mkammerer" % "argon2-jvm" % "2.12",
    libraryDependencies += "com.auth0" % "java-jwt" % "4.5.0",
    libraryDependencies += "com.github.ben-manes.caffeine" % "caffeine" % "3.2.3",
    libraryDependencies += "tools.jackson.module" %% "jackson-module-scala" % "3.0.2",
    libraryDependencies += "de.janis" % "knockoutwhist-data" % "1.0-SNAPSHOT",
    libraryDependencies += "org.hibernate.orm" % "hibernate-core" % "6.4.4.Final",
    libraryDependencies += "jakarta.persistence" % "jakarta.persistence-api" % "3.1.0",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.7.4",
    libraryDependencies += "org.playframework" %% "play-jdbc" % "3.0.6",
    libraryDependencies += "org.playframework" %% "play-java-jpa" % "3.0.6",
    libraryDependencies += "com.nimbusds" % "oauth2-oidc-sdk" % "11.31.1",
    libraryDependencies += "org.playframework" %% "play-ws" % "3.0.6",
    libraryDependencies += ws,
    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,

    PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml"
  )

lazy val root = (project in file("."))
  .aggregate(knockoutwhist, knockoutwhistweb)
  .settings(
    name := "KnockOutWhistWeb"
  )
