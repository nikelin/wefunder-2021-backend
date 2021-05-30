package com.wefunder.pitchdeck.services

import cats.effect.{ Async, ConcurrentEffect, Resource, Sync }
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import com.wefunder.pitchdeck.config.RendererConfig
import com.wefunder.pitchdeck.services.PPTDocumentService.supportedMimeTypes

import java.io.{ FileOutputStream, InputStream }
import fs2.Stream
import org.apache.poi.xslf.usermodel.XMLSlideShow

import java.awt.geom.Rectangle2D
import java.awt.{ Color, RenderingHints }
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object PPTDocumentService {

  val supportedMimeTypes = Set(
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
  )

}

class PPTDocumentService(override val config: RendererConfig) extends AbstractDocumentService with LazyLogging {

  override def isSupportedMimeType(mimeType: String): Boolean =
    supportedMimeTypes.contains(mimeType)

  override def extractPages[F[_]: ConcurrentEffect](
      fileName: String,
      document: InputStream
  ): fs2.Stream[F, DocumentPage] =
    Stream
      .bracket(
        Async[F].delay(new XMLSlideShow(document))
      )(v => Async[F].delay(v.close()))
      .flatMap { doc =>
        Stream
          .emits[F, Int](0 until doc.getSlides.size())
          .parEvalMapUnordered(config.parallelismLevel) { pageNum =>
            Async[F].delay {
              val img      = new BufferedImage(doc.getPageSize.width, doc.getPageSize.height, BufferedImage.TYPE_INT_RGB)
              val graphics = img.createGraphics()

              graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
              graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
              graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
              graphics.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
              )

              graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
              graphics.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, 150)

              graphics.setPaint(Color.white)
              graphics.fill(new Rectangle2D.Float(0, 0, doc.getPageSize.width, doc.getPageSize.height))

              doc.getSlides.get(pageNum).draw(graphics)

              (pageNum, img)
            }
          }
          .evalMap {
            case (pageNum, image) =>
              val resourceName = resolveResourcePath(pageNum, fileName)

              Resource
                .make(Async[F].delay(new FileOutputStream(resourceName.toString)))(v => Async[F].delay(v.close()))
                .use { os =>
                  Async[F].delay(ImageIO.write(image, config.outputFormat, os)) >>
                    Async[F]
                      .delay(logger.info("PPT has been converted successfully"))
                      .flatMap(_ => Async[F].delay(resourceName.toFile.length()))
                      .map { size =>
                        DocumentPage(pageNum, size, resourceName)
                      }
                }
          }
      }
}
