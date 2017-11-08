package io.udash.properties.seq

import io.udash.properties.single.{ForwarderProperty, ForwarderReadableProperty, Property, ReadableProperty}
import io.udash.utils.{JsArrayRegistration, Registration}

import scala.scalajs.js

trait ForwarderReadableSeqProperty[A, B, ElemType <: ReadableProperty[B], OrigType <: ReadableProperty[A]]
  extends ForwarderReadableProperty[Seq[B]] with ReadableSeqProperty[B, ElemType] {

  protected def origin: ReadableSeqProperty[A, OrigType]

  protected var originListenerRegistration: Registration = _
  protected var originStructureListenerRegistration: Registration = _

  protected val structureListeners: js.Array[Patch[ElemType] => Any] = js.Array()

  protected def originListener(originValue: Seq[A]): Unit = {}
  protected def originStructureListener(patch: Patch[OrigType]): Unit = {}

  protected def onListenerInit(): Unit = {}

  protected def initOriginListeners(): Unit = {
    if (originListenerRegistration == null || !originListenerRegistration.isActive()) {
      listeners.clear()
      onListenerInit()
      originListenerRegistration = origin.listen(originListener)
    }
    if (originStructureListenerRegistration == null || !originStructureListenerRegistration.isActive()) {
      structureListeners.clear()
      originStructureListenerRegistration = origin.listenStructure(originStructureListener)
    }
  }

  protected def killOriginListeners(): Unit = {
    if (originListenerRegistration != null && listeners.isEmpty) {
      originListenerRegistration.cancel()
      originListenerRegistration = null
    }
    if (originStructureListenerRegistration != null && listeners.isEmpty) {
      originStructureListenerRegistration.cancel()
      originStructureListenerRegistration = null
    }
  }

  override def listenStructure(structureListener: (Patch[ElemType]) => Any): Registration = {
    initOriginListeners()
    structureListeners += structureListener
    wrapListenerRegistration(new JsArrayRegistration(structureListeners, structureListener))
  }

  override def listen(valueListener: (Seq[B]) => Any, initUpdate: Boolean = false): Registration = {
    initOriginListeners()
    wrapListenerRegistration(super.listen(valueListener, initUpdate))
  }

  override def listenOnce(valueListener: (Seq[B]) => Any): Registration = {
    initOriginListeners()
    wrapListenerRegistration(super.listenOnce(valueListener))
  }

  protected def wrapListenerRegistration(reg: Registration): Registration = new Registration {
    override def restart(): Unit = {
      initOriginListeners()
      reg.restart()
    }

    override def cancel(): Unit = {
      reg.cancel()
      killOriginListeners()
    }

    override def isActive(): Boolean =
      reg.isActive()
  }
}

trait ForwarderWithLocalCopy[A, B, ElemType <: ReadableProperty[B], OrigType <: ReadableProperty[A]]
  extends ForwarderReadableSeqProperty[A, B, ElemType, OrigType] {

  import js.JSConverters._

  protected var transformedElements: js.Array[ElemType] = js.Array()

  protected def loadFromOrigin(): Seq[B]
  protected def elementsFromOrigin(): Seq[ElemType]
  protected def transformPatchAndUpdateElements(patch: Patch[OrigType]): Patch[ElemType]

  override def get: Seq[B] = {
    if (originListenerRegistration == null || !originListenerRegistration.isActive()) loadFromOrigin()
    else transformedElements.map(_.get).toSeq
  }

  override def elemProperties: Seq[ElemType] = {
    if (originListenerRegistration == null || !originListenerRegistration.isActive()) elementsFromOrigin()
    else transformedElements.toSeq
  }

  override protected def onListenerInit(): Unit = {
    val fromOrigin = elementsFromOrigin().toJSArray
    if (transformedElements.map(_.id) != fromOrigin.map(_.id)) {
      fireElementsListeners[ElemType](Patch[ElemType](0, transformedElements, fromOrigin, fromOrigin.isEmpty), structureListeners)
      fireValueListeners()
    } else if (transformedElements.map(_.get) != fromOrigin.map(_.get)) {
      fireValueListeners()
    }
    transformedElements = fromOrigin
  }

  override protected def originListener(originValue: Seq[A]) : Unit = {
    fireValueListeners()
  }

  override protected def originStructureListener(patch: Patch[OrigType]) : Unit = {
    val transPatch = transformPatchAndUpdateElements(patch)
    val cpy = structureListeners.jsSlice()
    cpy.foreach(_.apply(transPatch))
    fireValueListeners()
  }
}


trait ForwarderSeqProperty[A, B, ElemType <: Property[B], OrigType <: Property[A]]
  extends ForwarderReadableSeqProperty[A, B, ElemType, OrigType] with ForwarderProperty[Seq[B]] with SeqProperty[B, ElemType] {

  protected def origin: SeqProperty[A, OrigType]

  override def clearListeners(): Unit = {
    structureListeners.clear()
    super.clearListeners()
  }
}
