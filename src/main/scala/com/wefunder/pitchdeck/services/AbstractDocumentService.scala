package com.wefunder.pitchdeck.services

import com.wefunder.pitchdeck.config.RendererConfig

import java.nio.file.{ Path, Paths }
import java.util.UUID

trait AbstractDocumentService extends DocumentService {

  val config: RendererConfig

  protected def resolvePageName(fileName: String, pageNum: Int, outputFormat: String): String =
    s"f_${fileName}_p_${pageNum}_${UUID.randomUUID().toString}.${outputFormat}"

  protected def resolveResourcePath(pageNum: Int, fileName: String): Path =
    Paths.get(config.storagePath, resolvePageName(fileName, pageNum, config.outputFormat))

}
