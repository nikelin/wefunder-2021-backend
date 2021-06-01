package com.wefunder.pitchdeck.routes

import io.circe.fs2._
import io.circe.syntax._
import cats.effect.{ Async, ConcurrentEffect, Sync }
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import com.wefunder.pitchdeck.db.DbService
import com.wefunder.pitchdeck.domain.{ Id, Presentation, PresentationPage }
import com.wefunder.pitchdeck.services.DocumentServices
import org.http4s.{ EntityDecoder, HttpRoutes, Response }
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart
import org.http4s.circe._
import io.circe.generic.extras.semiauto._
import com.wefunder.pitchdeck.utils._
import doobie.Transactor
import doobie.implicits._
import doobie.util.log.LogHandler
import io.circe.{ Decoder, Encoder }

import java.io.BufferedInputStream
import java.net.URLEncoder
import java.time.LocalDateTime

object UploadRoutes extends LazyLogging {

  sealed trait RequestPayload
  object RequestPayload {
    object PresentationMetadataRequestPayload {
      implicit val decoder: Decoder[PresentationMetadataRequestPayload] =
        deriveConfiguredDecoder[PresentationMetadataRequestPayload]
    }

    case class PresentationMetadataRequestPayload(title: String, author: String, description: String)
        extends RequestPayload
  }

  sealed trait ResponsePayload
  object ResponsePayload {

    object PresentationCreatedResponsePayload {
      implicit val encoder: Encoder[PresentationCreatedResponsePayload] =
        deriveConfiguredEncoder[PresentationCreatedResponsePayload]
    }
    case class PresentationCreatedResponsePayload(presentationId: Id[Presentation]) extends ResponsePayload

  }

  import RequestPayload._

  def apply[F[_]: Async: ConcurrentEffect](dbService: DbService, documentServices: DocumentServices)(implicit
      tx: Transactor[F],
      lh: LogHandler
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "upload" =>
        EntityDecoder[F, Multipart[F]].decode(req, strict = true).value flatMap {
            case Left(error) =>
              BadRequest(error.toString)

            case Right(m)    =>
              val metadataPart   = m.parts.find(_.name.contains("metadata"))
              val binaryDataPart = m.parts.find(_.name.contains("data"))

              metadataPart.zip(binaryDataPart) match {
                case Some((metadataRaw, binary)) =>
                  val stream = binary.body
                    .through(fs2.io.toInputStream)
                    .map(new BufferedInputStream(_))
                    .flatMap { contentStream =>
                      metadataRaw.body
                        .through(byteStreamParser)
                        .through(decoder[F, PresentationMetadataRequestPayload])
                        .evalMap { metadata =>
                          val filename = convertFileName(binary.filename.getOrElse(metadata.title))
                          Async[F]
                            .delay(filename)
                            .flatMap { filename =>
                              Async[F].delay(contentStream.mark(Int.MaxValue)) >>
                                documentServices.selectService[F](binary.filename, contentStream) flatMap {
                                case Right(service) =>
                                  val convertedPages = Async[F].delay(contentStream.reset()) >> service
                                          .extractPages(filename, contentStream)
                                          .compile
                                          .toList
                                          .attempt

                                  convertedPages.flatMap {
                                    case Right(pages) =>
                                      val dbio = for {
                                        presentation <- dbService.createPresentation(
                                                          Presentation(
                                                            metadata.title,
                                                            metadata.author,
                                                            metadata.description,
                                                            LocalDateTime.now
                                                          )
                                                        )
                                        _            <- pages.map { page =>
                                               dbService
                                                 .createPresentationPage(
                                                   PresentationPage(
                                                     page.pageNum,
                                                     presentation.id,
                                                     filename,
                                                     page.path.toString,
                                                     page.size,
                                                     LocalDateTime.now
                                                   )
                                                 )
                                                 .map(res => (res, page))
                                             }.sequence
                                      } yield presentation

                                      dbio
                                        .transact(tx)
                                        .attempt
                                        .flatMap {
                                          case Right(presentationId) =>
                                            Ok(presentationId.asJson)
                                          case Left(error)           =>
                                            Async[F]
                                              .delay(
                                                logger.error("Failed to persist the extraction results", error)
                                              )
                                              .map(_ => Response[F](status = InternalServerError))
                                        }
                                    case Left(error)  =>
                                      Async[F]
                                        .delay(
                                          logger.error("Extraction failed", error)
                                        )
                                        .map(_ => Response[F](status = InternalServerError))
                                  }
                                case Left(message)  =>
                                  BadRequest(message)
                              }
                            }
                        }
                    }

                  stream.compile.last.map {
                    case Some(response) => response
                    case None           => Response[F](status = InternalServerError)
                  }
                case None                        =>
                  BadRequest("metadata and/or binary part of the request are missing")
              }
          }
    }
  }

  protected def convertFileName(value: String): String =
    URLEncoder.encode(value.split("\\.").head, "UTF-8")
}
