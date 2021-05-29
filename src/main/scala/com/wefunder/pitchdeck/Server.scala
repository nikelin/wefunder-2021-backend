package com.wefunder.pitchdeck

import cats.effect.{ Async, Blocker, ConcurrentEffect, ContextShift, Resource, Sync, Timer }
import com.wefunder.pitchdeck.routes.{ PresentationsRoutes, UploadRoutes }
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import com.wefunder.pitchdeck.config.{ Config, DatabaseConfig, RendererConfig, ServerConfig }
import com.wefunder.pitchdeck.db.schema.setupSchema
import com.wefunder.pitchdeck.db.{ DbService, DbServiceImpl }
import com.wefunder.pitchdeck.services.{ DocumentService, DocumentServices }
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import doobie.{ ExecutionContexts, Transactor }
import doobie.hikari.HikariTransactor
import doobie.util.log.LogHandler
import org.http4s.HttpRoutes
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{ Router, Server => BlazeServer }
import pureconfig.module.catseffect.loadConfigF

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Server extends LazyLogging {
  implicit val logHandler: LogHandler = db.doobieLogHandler

  Thread.setDefaultUncaughtExceptionHandler { (t: Thread, e: Throwable) =>
    System.err.print(s"Uncaught ${e.getClass.getSimpleName} in thread ${t.getName}:")
    e.printStackTrace(System.err)
  }

  def run[F[_]: ConcurrentEffect: ContextShift: Timer]: Resource[F, BlazeServer[F]] = {
    def routes(dbService: DbService, documentServices: DocumentServices)(implicit
        transactor: Transactor[F],
        blocker: Blocker
    ) =
      UploadRoutes[F](dbService, documentServices) <+>
          PresentationsRoutes[F](dbService)

    for {
      conf                     <- config[F]
      tx                       <- Resource.eval(transactor[F](conf.db))
      _                        <- Resource.eval(runSchemaSetup[F](conf.db, tx))
      dbService                 = new DbServiceImpl
      documentServices         <- Resource.eval(documentServices[F](conf.renderers))
      staticContentBlockingPool = Executors.newFixedThreadPool(4)
      staticContentBlocker      = Blocker.liftExecutorService(staticContentBlockingPool)
      rts                       = Router("api" -> routes(dbService, documentServices)(tx, staticContentBlocker))
      svr                      <- server[F](conf.server, CORS(rts))
    } yield svr
  }

  private[this] def config[F[_]: ContextShift: Async]: Resource[F, Config] = {
    import pureconfig.generic.auto._

    for {
      blocker <- Blocker[F]
      config  <- Resource.eval(loadConfigF[F, Config](blocker))
      _       <- Resource.eval(Sync[F].delay(logger.info(config.show())))
    } yield config
  }

  def runSchemaSetup[F[_]: Async](config: DatabaseConfig, transactor: Transactor[F]): F[Unit] =
    if (config.enableSchemaSetup)
      transactor.trans
        .apply(setupSchema)
        .flatMap(_ => Async[F].delay(logger.info("[schema] OK - Schema setup complete.")))
    else Async[F].delay(logger.info("[schema] Skipping schema setup..."))

  private[this] def documentServices[F[_]: Async](
      configs: Seq[RendererConfig]
  ): F[DocumentServices] = {
    val services = configs
      .map(documentService[F])
      .foldRight(Sync[F].pure(List.empty[DocumentService])) { (result, list) =>
        list.flatMap { xs =>
          Async[F].map(result)(x => x :: xs)
        }
      }

    Async[F].map(services)(DocumentServices)
  }

  private[this] def documentService[F[_]: Async](config: RendererConfig): F[DocumentService] =
    Async[F].delay(
      getClass.getClassLoader
        .loadClass(config.implementationClassName)
        .getConstructor(classOf[RendererConfig])
        .newInstance(config)
        .asInstanceOf[DocumentService]
    )

  private def dataSourceConfig(name: String, config: DatabaseConfig): HikariConfig = {
    val dataSourceConfig = new HikariConfig
    dataSourceConfig.setAutoCommit(false)
    dataSourceConfig.setDriverClassName(config.driverClassName)
    dataSourceConfig.setJdbcUrl(config.jdbcUri)
    dataSourceConfig.setUsername(config.userName)
    dataSourceConfig.setPassword(config.password.value)
    dataSourceConfig.setMaximumPoolSize(config.maxPoolSize)
    dataSourceConfig.setPoolName(s"Pool Name $name")
    dataSourceConfig.setMinimumIdle(config.minPoolSize)
    dataSourceConfig
  }

  private[this] def transactor[F[_]: Async: ContextShift](config: DatabaseConfig): F[HikariTransactor[F]] =
    Async[F].delay {
      val dbTransactExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

      val dbExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(config.maxPoolSize)
      )

      HikariTransactor.apply(
        new HikariDataSource(dataSourceConfig("Main", config)),
        dbExecutionContext,
        Blocker.liftExecutionContext(dbTransactExecutionContext)
      )
    }

  private[this] def server[F[_]: ConcurrentEffect: Timer](
      config: ServerConfig,
      routes: HttpRoutes[F]
  ): Resource[F, BlazeServer[F]] = {
    import org.http4s.implicits._

    BlazeServerBuilder[F](ExecutionContext.global)
      .bindHttp(config.port, config.host)
      .withHttpApp(routes.orNotFound)
      .resource
  }

}
