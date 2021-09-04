def asm(n: String) = "org.ow2.asm" % s"asm-$n" % "9.2"
def zio(n: String) = "dev.zio" %% s"zio-$n" % "2.0.0-M2"

lazy val `asm-flow` = project.in(file("."))
  .settings(
    organization := "com.sandinh",
    version := "0.1.0",
    scalaVersion := "3.0.1",
    libraryDependencies ++= Seq(
      asm("tree"),
      zio("streams"),
//      zio("test-junit") % Test,
    ),
//    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
