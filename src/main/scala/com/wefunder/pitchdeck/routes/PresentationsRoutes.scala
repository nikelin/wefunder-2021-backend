package com.wefunder.pitchdeck.routes

import fs2.Stream
import io.circe._
import io.circe.syntax._
import cats.implicits._
import cats.effect.{ Async, Blocker, ContextShift }
import com.wefunder.pitchdeck.db.DbService
import com.wefunder.pitchdeck.domain._
import com.wefunder.pitchdeck.utils._
import doobie.implicits._
import doobie.{ FC, Transactor }
import doobie.util.log.LogHandler
import io.circe.Codec
import io.circe.generic.extras.semiauto._
import org.http4s.{ HttpRoutes, Response, StaticFile }
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

import java.io.File

object PresentationsRoutes {

  object PresentationData {
    def apply(presentation: Record[Presentation], pages: Seq[Record[PresentationPage]]): PresentationData =
      PresentationData(
        presentation.id,
        presentation.entity,
        pages.map(_.entity)
      )

    implicit val codec: Codec[PresentationData] = deriveConfiguredCodec[PresentationData]
  }
  case class PresentationData(id: Id[Presentation], presentation: Presentation, pages: Seq[PresentationPage])

  def apply[F[_]: Async: ContextShift](
      dbService: DbService
  )(implicit tx: Transactor[F], blocker: Blocker, lh: LogHandler): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]; import dsl._

    HttpRoutes.of[F] {
      case req @ GET -> Root / "presentation" / LongVar(presentationId) / "page" / IntVar(pageNum) / "view" =>
        val page = dbService
          .findPresentationPagesFrom(Id[Presentation](presentationId), pageNum)
          .take(1)
          .compile
          .last
          .transact(tx)

        page flatMap {
          case None       =>
            Async[F].delay(Response[F](status = NotFound))
          case Some(page) =>
            StaticFile
              .fromFile(new File(page.entity.path), blocker, Some(req))
              .getOrElseF(NotFound())
        }

      case GET -> Root / "presentation" / LongVar(id)                                                       =>
        val dbIO = dbService
          .findPresentation(Id[Presentation](id))
          .flatMap {
            case Some(presentation) =>
              dbService.findPresentationPages(presentation.id).compile.toList.map { pages =>
                Option.apply[PresentationData](PresentationData(presentation, pages))
              }
            case None               =>
              FC.pure(Option.empty[PresentationData])
          }

        dbIO
          .transact(tx)
          .flatMap {
            case Some(results) =>
              Ok(results.asJson)
            case None          =>
              Async[F].delay(Response[F](status = NotFound))
          }
      case GET -> Root / "presentations"                                                                    =>
        val dbIO = dbService.findPresentations(0).flatMap { presentation =>
          Stream.eval(dbService.findPresentationPagesFrom(presentation.id, 0).compile.toList).map { pages =>
            PresentationData(presentation, pages)
          }
        }

        dbIO
          .transact(tx)
          .compile
          .toList
          .flatMap { results =>
            Ok(results.asJson)
          }

      case req @ GET -> Root / "presentation" / "preview" / LongVar(id)                                     =>
        val presentationPage = dbService
          .findPresentationPagesFrom(Id[Presentation](id), 0)
          .take(1)
          .compile
          .last
          .transact(tx)

        presentationPage flatMap {
          case None       =>
            Async[F].delay(Response[F](status = NotFound))
          case Some(page) =>
            StaticFile
              .fromFile(new File(page.entity.path), blocker, Some(req))
              .getOrElseF(NotFound())
        }

    }
  }
}
