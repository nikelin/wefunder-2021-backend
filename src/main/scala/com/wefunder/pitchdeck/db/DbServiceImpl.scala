package com.wefunder.pitchdeck.db
import com.wefunder.pitchdeck.domain
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import doobie.implicits._

import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

class DbServiceImpl extends DbService {
  import com.wefunder.pitchdeck.domain._
  import com.wefunder.pitchdeck.db.schema.Tables._
  import com.wefunder.pitchdeck.db.schema.Tables.dc._

  override def findPresentation(presentation: Id[Presentation])(implicit
      lh: LogHandler
  ): ConnectionIO[Option[Record[Presentation]]] =
    run {
      quote {
        presentationMeta.entity.filter(_.id.value == lift(presentation.value)).take(1)
      }
    }.map(_.headOption)

  override def findPresentations(page: Int, pageSize: Int)(implicit
      lh: LogHandler
  ): fs2.Stream[ConnectionIO, Record[Presentation]] =
    stream {
      quote {
        presentationMeta.entity
      }
    }

  override def findPresentationPagesFrom(presentation: Id[Presentation], pageNum: Int)(implicit
      lh: LogHandler
  ): fs2.Stream[ConnectionIO, Record[PresentationPage]] =
    stream {
      quote {
        presentationPageMeta.entity
          .filter { resource =>
            resource.entity.presentation.value == lift(presentation.value) &&
            resource.entity.pageNum >= lift(pageNum)
          }
          .take(1)
      }
    }

  override def findPresentationPages(presentation: Id[Presentation])(implicit
      lh: LogHandler
  ): fs2.Stream[ConnectionIO, Record[PresentationPage]] =
    stream {
      quote {
        presentationPageMeta.entity
          .filter { resource =>
            resource.entity.presentation.value == lift(presentation.value)
          }
      }
    }

  override def createPresentation(presentation: domain.Presentation)(implicit
      logger: LogHandler
  ): ConnectionIO[Record[Presentation]] =
    run {
      quote {
        presentationMeta.entity
          .insert(
            lift(
              Record(
                Id[Presentation](0),
                presentation
              )
            )
          )
          .returning(_.id)
      }
    }.map(id => Record[Presentation](id, presentation))

  override def createPresentationPage(page: PresentationPage)(implicit
      lh: LogHandler
  ): ConnectionIO[Record[PresentationPage]] =
    run {
      quote {
        presentationPageMeta.entity
          .insert(
            lift(
              Record(
                Id[PresentationPage](0),
                page
              )
            )
          )
          .returning(_.id)
      }
    }.map(id => Record[PresentationPage](id, page))
}
