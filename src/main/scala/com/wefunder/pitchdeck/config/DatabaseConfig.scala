package com.wefunder.pitchdeck.config

import scala.concurrent.duration.FiniteDuration

case class DatabaseConfig(
    enableSchemaSetup: Boolean,
    driverClassName: String,
    jdbcUri: String,
    userName: String,
    password: Password,
    maxPoolSize: Int,
    minPoolSize: Int,
    leakCheckInterval: FiniteDuration,
    connectionTimeout: FiniteDuration,
    connectionMaxLifetime: FiniteDuration
)
