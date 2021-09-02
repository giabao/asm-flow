def asm(n: String) = "org.ow2.asm" % s"asm-$n" % "9.2"
def zio(n: String) = "dev.zio" %% s"zio-$n" % "2.0.0-M2"

lazy val `asm-flow` = project.in(file("."))
  .settings(
    organization := "com.sandinh",
    version := "0.1.0",
    scalaVersion := "3.0.1",
    libraryDependencies ++= Seq(
      asm("util"), asm("tree"), asm("analysis"),
      zio("streams"),
      zio("test") % Test,
      zio("test-sbt") % Test,
      zio("test-junit") % Test,
//      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
    ),
//    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
