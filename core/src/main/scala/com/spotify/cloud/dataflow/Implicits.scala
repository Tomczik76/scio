package com.spotify.cloud.dataflow

import java.lang.{Float => JFloat}

import com.google.cloud.dataflow.sdk.coders._
import com.google.cloud.dataflow.sdk.values.{TypeDescriptor, KV}
import com.spotify.cloud.dataflow.coders.KryoAtomicCoder

import scala.language.implicitConversions
import scala.reflect.ClassTag

private[dataflow] object Implicits {

  private[dataflow] implicit class RichCoderRegistry(val r: CoderRegistry) extends AnyVal {

    def registerScalaCoders(): Unit = {
      // Missing Coders from DataFlowJavaSDK
      r.registerCoder(classOf[JFloat], classOf[FloatCoder])

      r.registerCoder(classOf[Int], classOf[VarIntCoder])
      r.registerCoder(classOf[Long], classOf[VarLongCoder])
      r.registerCoder(classOf[Float], classOf[FloatCoder])
      r.registerCoder(classOf[Double], classOf[DoubleCoder])

      // Fall back to Kryo
      r.setFallbackCoderProvider(new CoderProvider {
        override def getCoder[T](`type`: TypeDescriptor[T]): Coder[T] = new KryoAtomicCoder().asInstanceOf[Coder[T]]
      })
    }

    def getScalaCoder[T: ClassTag]: Coder[T] = {
      val ct = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
      val tt = TypeDescriptor.of(ct)
      val coder = r.getDefaultCoder(tt)

      // For classes not registered in CoderRegistry, it returns
      // SerializableCoder if the class extends Serializable or null otherwise.
      // Override both cases with KryoAtomicCoder.
      if (coder == null || coder.getClass == classOf[SerializableCoder[T]]) {
        new KryoAtomicCoder().asInstanceOf[Coder[T]]
      } else {
        coder
      }
    }

    def getScalaKvCoder[K: ClassTag, V: ClassTag]: Coder[KV[K, V]] = KvCoder.of(getScalaCoder[K], getScalaCoder[V])

  }

}
