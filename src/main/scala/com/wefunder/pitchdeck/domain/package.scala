package com.wefunder.pitchdeck

import io.circe.generic.extras.semiauto._
import com.wefunder.pitchdeck.utils._
import io.circe.{ Codec, Decoder, DecodingFailure, Encoder, HCursor, Json }
import io.getquill.Embedded

import java.sql.Timestamp
import java.time.LocalDateTime

package object domain {

  trait Entity

  implicit def idEncoder[T <: Entity]: Encoder[Id[T]] =
    (a: Id[T]) => Json.obj("value" -> Json.fromLong(a.value))

  implicit def idDecoder[T <: Entity]: Decoder[Id[T]] =
    (c: HCursor) =>
      c.downField("value")
        .focus
        .flatMap(json => json.asNumber.flatMap(_.toLong).map(value => Id.apply[T](value)))
        .fold[Either[DecodingFailure, Id[T]]](Left(DecodingFailure("failed", List.empty)))(r => Right(r))

  case class Id[T <: Entity](value: Long) extends Embedded

  object PresentationPage {
    implicit val codec: Codec[PresentationPage] = deriveConfiguredCodec[PresentationPage]
  }

  case class PresentationPage(
      pageNum: Int,
      presentation: Id[Presentation],
      filename: String,
      path: String,
      size: Long,
      createdAt: LocalDateTime
  ) extends Entity
      with Embedded

  object Presentation {
    implicit val codec: Codec[Presentation] = deriveConfiguredCodec[Presentation]
  }

  case class Presentation(title: String, author: String, description: String, createdAt: LocalDateTime)
      extends Entity
      with Embedded

  object Record {
    implicit def encodeResult[T <: Entity](implicit encoder: Encoder[T]): Encoder[Record[T]] =
      (value: Record[T]) =>
        Json.obj(
          "id"     -> idEncoder(value.id),
          "entity" -> encoder(value.entity)
        )
  }

  case class Record[E <: Entity](id: Id[E], entity: E)

}
