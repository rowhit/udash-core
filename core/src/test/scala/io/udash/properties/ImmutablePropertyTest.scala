package io.udash.properties

import io.udash.properties.model.ReadableModelProperty
import io.udash.properties.seq.ReadableSeqProperty
import io.udash.properties.single.{Property, ReadableProperty}
import io.udash.testing.UdashCoreTest

class ImmutablePropertyTest extends UdashCoreTest {
  import ImmutablePropertyTest._

  "ImmutableProperty" should {
    "handle standard operations of ReadableProperty" in {
      val p: ReadableProperty[Int] = new ImmutableProperty[Int](7)

      p.get should be(7)

      var counter = 0
      p.listen(_ => counter += 1)
      counter should be(0)
      p.listen(_ => counter += 1, initUpdate = true)
      counter should be(1)

      val t = Property(0)
      p.streamTo(t, initUpdate = false)(identity)
      t.get should be(0)
      p.streamTo(t, initUpdate = true)(identity)
      t.get should be(7)
    }
  }

  "ImmutableModelProperty" should {
    "handle standard operations of ReadableModelProperty" in {
      val e = ModelEntity("a", Seq(1), ModelEntity("b", Seq(2), ModelEntity("c", Seq(3,4), null)))
      val p: ReadableModelProperty[ModelEntity] = new ImmutableModelProperty[ModelEntity](e)

      p.get should be(e)
      p.roSubProp(_.i).get should be(Seq(1))
      p.roSubProp(_.s).get should be("a")
      p.roSubProp(_.m).get should be(e.m)
      p.roSubProp(_.m.i).get should be(Seq(2))
      p.roSubProp(_.m.s).get should be("b")
      p.roSubSeq(_.m.m.i).elemProperties.head.get should be(3)
    }
  }

  "ImmutableSeqProperty" should {
    "handle standard operations of ReadableSeqProperty" in {
      val p: ReadableSeqProperty[Int, ReadableProperty[Int]] = new ImmutableSeqProperty[Int](Seq(1,2,3))

      p.get should be(Seq(1,2,3))
      p.elemProperties.map(_.get) should be(Seq(1,2,3))
      p.transform((v: Int) => v + 1).get should be(Seq(2,3,4))

      var counter = 0
      p.listen(_ => counter += 1)
      counter should be(0)
      p.listen(_ => counter += 1, initUpdate = true)
      counter should be(1)
      p.listenStructure(_ => counter += 1)
      counter should be(1)
    }
  }
}

object ImmutablePropertyTest {
  case class ModelEntity(s: String, i: Seq[Int], m: ModelEntity)
  object ModelEntity extends HasModelPropertyCreator[ModelEntity]
}