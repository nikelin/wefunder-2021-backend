package com.wefunder.pitchdeck.services

import cats.effect.{ Async, ConcurrentEffect, Resource, Sync }
import cats.implicits._
import com.wefunder.pitchdeck.config.RendererConfig
import com.wefunder.pitchdeck.services.PDFDocumentService.supportedMimeTypes

import java.io.{ FileOutputStream, InputStream }
import fs2.Stream
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.{ ImageType, PDFRenderer }

import java.nio.file.FileSystems
import javax.imageio.ImageIO

object PDFDocumentService {
  val supportedMimeTypes = Set(
    "application/pdf"
  )
}

class PDFDocumentService(override val config: RendererConfig) extends AbstractDocumentService {

  override def isSupportedMimeType(mimeType: String): Boolean =
    supportedMimeTypes.contains(mimeType)

  override def extractPages[F[_]: ConcurrentEffect](
      fileName: String,
      document: InputStream
  ): Stream[F, DocumentPage] =
    Stream
      .bracket(
        Sync[F].delay(PDDocument.load(document))
      )(v => Sync[F].delay(v.close()))
      .flatMap { document =>
        val numPages = document.getNumberOfPages
        val renderer = new PDFRenderer(document)

        Stream
          .emits[F, Int](0 until numPages)
          .parEvalMap(config.parallelismLevel) { pageNum =>
            Async[F].delay((pageNum, renderer.renderImageWithDPI(pageNum, config.dpi, ImageType.ARGB)))
          }
          .evalMap {
            case (pageNum, image) =>
              val resourceName = resolveResourcePath(pageNum, fileName)

              Resource
                .make(Async[F].delay(new FileOutputStream(resourceName.toString)))(v => Async[F].delay(v.close()))
                .use { outputStream =>
                  Async[F]
                    .delay(FileSystems.getDefault.getPath(config.storagePath).toFile.mkdirs())
                    .flatMap(_ => Async[F].delay(ImageIO.write(image, config.outputFormat, outputStream)))
                    .flatMap(_ => Async[F].delay(resourceName.toFile.length()))
                    .map(size => DocumentPage(pageNum, size, resourceName))
                }
          }
      }
}
