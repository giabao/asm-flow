package com.sandinh.asmflow

import org.junit.runner.RunWith
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.environment.*

import java.io.File
import zio.Console.ConsoleLive.*
import zio.stream.*
import zio.test.junit.JUnitRunnableSpec

@RunWith(classOf[zio.test.junit.ZTestJUnitRunner])
class AsmFlowSpec extends DefaultRunnableSpec {
  val f = "???-util-shadow.jar"
  val from = "com/smartf???"
//  val to = "scala211/reflect"
  val find = FindRefIn(Seq(new File(f)))

  def spec = suite("AsmFlow")(
    test("findRef") {
      val found = for {
        ref <- find.all if ref.fromCls.startsWith(from) && ref.target.contains(to)
      } yield ref
      for {
        _ <- found.foreach(printLine)
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("Hello, World!\n")))
    }
  )
}
