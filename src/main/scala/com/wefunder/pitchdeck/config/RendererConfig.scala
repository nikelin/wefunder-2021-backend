package com.wefunder.pitchdeck.config

case class RendererConfig(
    implementationClassName: String,
    storagePath: String,
    outputFormat: String,
    parallelismLevel: Int = 1,
    dpi: Int = 96
)
