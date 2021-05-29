package com.wefunder.pitchdeck

import io.circe.generic.extras.Configuration

package object utils {
  implicit val configuration: Configuration = Configuration.default
    .withDiscriminator("$type")
}
