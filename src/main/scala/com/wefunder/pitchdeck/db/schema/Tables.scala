package com.wefunder.pitchdeck.db.schema

import com.wefunder.pitchdeck.domain.{ Presentation, PresentationPage, Record }
import doobie.quill.DoobieContext
import io.getquill.Literal
import java.sql.Types

object Tables {
  val dc = new DoobieContext.Postgres(Literal)
  import dc._
  import io.getquill.{ idiom => _, _ }

  implicit val presentationInsertMeta =
    dc.insertMeta[Record[Presentation]](_.id)

  implicit val presentationMeta =
    dc.schemaMeta[Record[Presentation]](
      "presentation",
      _.id.value           -> "id",
      _.entity.title       -> "title",
      _.entity.author      -> "author",
      _.entity.description -> "description"
    )

  implicit val presentationPageInsertMeta =
    dc.insertMeta[Record[PresentationPage]](_.id)

  implicit val presentationPageMeta =
    dc.schemaMeta[Record[PresentationPage]](
      "presentation_page",
      _.id.value                  -> "id",
      _.entity.path               -> "path",
      _.entity.size               -> "size",
      _.entity.createdAt          -> "created_at",
      _.entity.filename           -> "name",
      _.entity.presentation.value -> "presentation_id",
      _.entity.pageNum            -> "page_num"
    )

}
