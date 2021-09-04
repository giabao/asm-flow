package com.sandinh.asmflow

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.*
import zio.{Chunk, Task, ZIO, ZManaged}
import zio.stream.{ZStream, ZTransducer}

import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.util.jar.JarFile
import java.util.List as JList
import scala.util.Using
import scala.jdk.CollectionConverters.*
import AsmFlow.*

import scala.annotation.switch

type Source[+A] = ZStream[Any, Throwable, A]
val Source = ZStream

type Flow[-A, +B] = ZTransducer[Any, Throwable, A, B]
val Flow = ZTransducer

enum RefType:
  case Anot, Insn //instruction

case class Ref(fromCls: String, tpe: RefType, target: String)

case class FindRefIn(jars: Seq[File]):
  def classes: Source[ClassNode] = Source.from(jars).flatMap(classReaderStream)

  def all: Source[Ref] =
    for {
      c <- classes
      ref <- c.allRefs
    } yield ref

end FindRefIn

object AsmFlow:
  private def managed[A <: AutoCloseable](effect: => A): Source[A] =
    ZStream.managed {
      ZManaged.fromAutoCloseable(ZIO.attemptBlocking(effect))
    }

  def classReaderStream(f: File): Source[ClassNode] =
    for {
      jarFile <- managed(new JarFile(f))
      entry <- Source.from(
        jarFile.entries.asScala
      ) if entry.getName.endsWith(".class")
      inputStream <- managed(jarFile.getInputStream(entry))
    } yield {
      val classNode = new ClassNode()
      val classReader = new ClassReader(inputStream)
      classReader.accept(classNode, 0)
      classNode
    }

end AsmFlow

extension [A](l: JList[A])
  def asScalaSafe: collection.Seq[A] = Option(l.asScala).getOrElse(Nil)
  def asSource: Source[A] = Source.from(l.asScalaSafe)

extension (c: ClassNode)
  def classAnotRefs: Iterable[String] =
    for {
      annot <- c.anots
      ref <- annot.names
    } yield ref

  def fieldAnotRefs: Iterable[String] =
    for {
      field <- c.fields.asScala
      annot <- field.anots
      ref <- annot.names
    } yield ref

  def insnRefs: Iterable[String] =
    for {
      method <- c.methods.asScala
      insn <- method.instructions.asScala
      ref <- insn.ref
    } yield ref

  def allRefs: Source[Ref] =
    def anots = ZStream.repeat(RefType.Anot) zip ZStream.from(
      c.fieldAnotRefs ++ c.classAnotRefs
    )

    def insns = ZStream.repeat(RefType.Insn) zip Source.from(c.insnRefs)

    (anots ++ insns).map(Ref(c.name, _, _))

end extension

extension (f: FieldNode)
  def anots: collection.Seq[AnnotationNode] =
    f.visibleAnnotations.asScalaSafe ++
      f.invisibleAnnotations.asScalaSafe ++
      f.visibleTypeAnnotations.asScalaSafe ++
      f.invisibleTypeAnnotations.asScalaSafe

extension (f: ClassNode)
  def anots: collection.Seq[AnnotationNode] =
    f.visibleAnnotations.asScalaSafe ++
      f.invisibleAnnotations.asScalaSafe ++
      f.visibleTypeAnnotations.asScalaSafe ++
      f.invisibleTypeAnnotations.asScalaSafe

extension (insn: AbstractInsnNode)
  def ref: Option[String] = insn match {
    case x: MethodInsnNode =>
      Some(x.owner + "/" + x.name)
    case x: FieldInsnNode =>
      Some(x.owner + "/" + x.name)
    case x: InvokeDynamicInsnNode =>
      Some(x.bsm.getOwner + "/" + x.name)
    case _ => None
  }

extension (t: org.objectweb.asm.Type)
  def refs: Iterator[String] =
    import org.objectweb.asm.Type._
    (t.getSort: @switch) match {
      case OBJECT => Iterator(t.getInternalName)
      case ARRAY => t.getElementType.refs
      case METHOD => t.getReturnType.refs ++ t.getArgumentTypes.flatMap(_.refs)
      case _ => Iterator.empty
    }

extension (a: AnnotationNode)
  def names: Iterator[String] =
    a.values.asScalaSafe
      .grouped(2)
      .flatMap {
        case name +: value +: Nil =>
          Iterator(name.asInstanceOf[String]) ++
            namesFromAnotValue(value)
      } ++ Iterator(descToRef(a.desc))

  /** @see [[org.objectweb.asm.tree.AnnotationNode#values]] */
  private def namesFromAnotValue(v: AnyRef): Iterator[String] = v match {
    case _: (Number | Boolean | Character)=> Iterator.empty
    case s: String => Iterator(s)
    case t: org.objectweb.asm.Type => t.refs
    // @see org.objectweb.asm.tree.AnnotationNode.visitEnum
    case a: Array[String] => a.iterator.map(descToRef)
    case a: AnnotationNode => a.names
    case l: JList[_] => l.asScala.flatMap(namesFromAnotValue).iterator
    case _ => throw new IllegalArgumentException(v.toString)
  }

  private def descToRef(desc: String) = desc.stripPrefix("L").stripSuffix(";")
