package com.wefunder.pitchdeck.config

import pprint.PPrinter

import java.util.TimeZone

final case class Config(server: ServerConfig, db: DatabaseConfig, renderers: Seq[RendererConfig]) {
  def show(): String = {
    val appPprint: PPrinter = pprint.copy(
      additionalHandlers = {
        case p: Password => pprint.Tree.Literal("***")
      }
    )

    val sb = new StringBuilder

    sb.append("=== App Configuration")

    sb.append("Current Time Zone: " + TimeZone.getDefault.getDisplayName)
    sb.append("HTTP Config:\n" + appPprint(server))
    sb.append("Database Config:\n" + appPprint(db))
    sb.append("Renderer Config:\n" + appPprint(renderers))

    sb.toString()
  }
}
