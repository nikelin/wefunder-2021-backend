package com.wefunder.pitchdeck

package object config {
  case class Password(value: String) extends AnyVal {
    override def toString = "PASSWORD"
  }
}
