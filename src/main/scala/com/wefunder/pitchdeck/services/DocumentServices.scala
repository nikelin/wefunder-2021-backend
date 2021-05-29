package com.wefunder.pitchdeck.services

import cats.effect.Sync
import com.typesafe.scalalogging.LazyLogging
import org.apache.tika.Tika
import org.apache.tika.detect.Detector

import java.io.InputStream

case class DocumentServices(services: Seq[DocumentService]) extends LazyLogging {

  def selectService[F[_]: Sync](name: Option[String], content: InputStream): F[Either[String, DocumentService]] =
    Sync[F].delay {
      val tika     = new Tika()
      val mimeType = name.fold(tika.detect(content))(v => tika.detect(content, v))

      services.find(_.isSupportedMimeType(mimeType)) match {
        case Some(service) => Right(service)
        case None          => Left(s"unsupported mime-type: ${mimeType}")
      }
    }

}
