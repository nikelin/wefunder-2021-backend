package com.wefunder.pitchdeck

import java.nio.file.Path

package object services {

  case class DocumentPage(pageNum: Int, size: Long, path: Path)

}
