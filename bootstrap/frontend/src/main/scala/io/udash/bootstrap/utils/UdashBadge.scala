package io.udash.bootstrap
package utils

import io.udash._
import org.scalajs.dom

import scalatags.JsDom.all._

class UdashBadge(mds: Modifier*) extends UdashBootstrapComponent {
  override lazy val render: dom.Element =
    span(BootstrapStyles.Label.badge)(mds).render
}

object UdashBadge {
  def apply(content: Property[_]): UdashBadge =
    new UdashBadge(bind(content))

  def apply(mds: Modifier*): UdashBadge =
    new UdashBadge(mds)
}