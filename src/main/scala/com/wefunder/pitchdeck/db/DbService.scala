package com.wefunder.pitchdeck.db

import com.wefunder.pitchdeck.domain.{ Id, Presentation, PresentationPage, Record }
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import fs2.Stream

trait DbService {

  def findPresentations(page: Int, pageSize: Int = 25)(implicit
      lh: LogHandler
  ): Stream[ConnectionIO, Record[Presentation]]

  def findPresentation(presentation: Id[Presentation])(implicit
      lh: LogHandler
  ): ConnectionIO[Option[Record[Presentation]]]

  def findPresentationPages(presentation: Id[Presentation])(implicit
      lh: LogHandler
  ): fs2.Stream[ConnectionIO, Record[PresentationPage]]

  def findPresentationPagesFrom(
      presentation: Id[Presentation],
      pageNum: Int
  )(implicit lh: LogHandler): Stream[ConnectionIO, Record[PresentationPage]]

  def createPresentation(presentation: Presentation)(implicit lh: LogHandler): ConnectionIO[Record[Presentation]]

  def createPresentationPage(page: PresentationPage)(implicit lh: LogHandler): ConnectionIO[Record[PresentationPage]]
}
