package com.wefunder.pitchdeck.services

import cats.effect.{ Async, ConcurrentEffect, Sync }
import com.wefunder.pitchdeck.config.RendererConfig

import java.io.InputStream
import fs2.Stream

trait DocumentService {

  def isSupportedMimeType(mimeType: String): Boolean

  def extractPages[F[_]: ConcurrentEffect](
      fileName: String,
      document: InputStream
  ): Stream[F, DocumentPage]

}
