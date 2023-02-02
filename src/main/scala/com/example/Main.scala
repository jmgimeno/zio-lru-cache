package com.example

import zio._
import cache._

object UseLRUCacheWithOneFiber extends ZIOAppDefault {
  val program: ZIO[IntLRUCacheEnv, Throwable, Int] =
    for {
      _ <- put(1, 1)
      _ <- put(2, 2)
      _ <- get(1)
      _ <- put(3, 3)
      _ <- get(2)
      _ <- put(4, 4)
      _ <- get(1)
      _ <- get(3)
      _ <- get(4)
    } yield 0

  val run =
    program
      .provide(
        ZLayer.succeed(2),
        IntLRUCacheEnv.Service.zioRefImpl
      )
      .catchAll(ex => Console.printLine(ex.getMessage).orDie *> ZIO.succeed(1))

  private def get(key: Int): RIO[IntLRUCacheEnv, Unit] =
    (for {
      _ <- Console.printLine(s"Getting key: $key").orDie
      v <- getInt(key)
      _ <- Console.printLine(s"Obtained value: $v").orDie
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  private def put(key: Int, value: Int): URIO[IntLRUCacheEnv, Unit] =
    Console.printLine(s"Putting ($key, $value)").orDie *> putInt(key, value)
}

object UseLRUCacheWithMultipleFibers extends ZIOAppDefault {

  val producer: URIO[IntLRUCacheEnv, Unit] =
    for {
      number <- Random.nextIntBounded(100)
      _      <- Console.printLine(s"Producing ($number, $number)").orDie
      _      <- putInt(number, number)
    } yield ()

  val consumer: URIO[IntLRUCacheEnv, Unit] =
    (for {
      key   <- Random.nextIntBounded(100)
      _     <- Console.printLine(s"Consuming key: $key")
      value <- getInt(key)
      _     <- Console.printLine(s"Consumed value: $value")
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  val reporter: ZIO[IntLRUCacheEnv, NoSuchElementException, Unit] =
    for {
      status                          <- getCacheStatus
      (items, optionStart, optionEnd) = status
      _                               <- Console.printLine(s"Items: $items, Start: $optionStart, End: $optionEnd").orDie
    } yield ()

  val program =
    for {
      fiberReporter  <- reporter.forever.fork
      fiberProducers <- ZIO.forkAll(ZIO.replicate(100)(producer.forever))
      fiberConsumers <- ZIO.forkAll(ZIO.replicate(100)(consumer.forever))
      _              <- Console.readLine.orDie *> (fiberReporter <*> fiberProducers <*> fiberConsumers).interrupt
    } yield 0

  val run =
    program
      .provide(
        ZLayer.succeed(3),
        IntLRUCacheEnv.Service.zioRefImpl
      )
      .catchAll(ex => Console.printLine(ex.getMessage).orDie *> ZIO.succeed(1))
}

object UseConcurrentLRUCacheWithMultipleFibers extends ZIOAppDefault {

  val producer: URIO[IntLRUCacheEnv, Unit] =
    for {
      number <- Random.nextIntBounded(100)
      _      <- Console.printLine(s"Producing ($number, $number)").orDie
      _      <- putInt(number, number)
    } yield ()

  val consumer: URIO[IntLRUCacheEnv, Unit] =
    (for {
      key   <- Random.nextIntBounded(100)
      _     <- Console.printLine(s"Consuming key: $key").orDie
      value <- getInt(key)
      _     <- Console.printLine(s"Consumed value: $value").orDie
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  val reporter: ZIO[IntLRUCacheEnv, NoSuchElementException, Unit] =
    for {
      status                          <- getCacheStatus
      (items, optionStart, optionEnd) = status
      _                               <- Console.printLine(s"Items: $items, Start: $optionStart, End: $optionEnd").orDie
    } yield ()

  val program =
    for {
      fiberReporter  <- reporter.forever.fork
      fiberProducers <- ZIO.forkAll(ZIO.replicate(100)(producer.forever))
      fiberConsumers <- ZIO.forkAll(ZIO.replicate(100)(consumer.forever))
      _              <- Console.readLine.orDie *> (fiberReporter <*> fiberProducers <*> fiberConsumers).interrupt
    } yield 0

  val run =
    program
      .provide(
        ZLayer.succeed(3),
        IntLRUCacheEnv.Service.zioStmImpl
      )
      .catchAll(ex => Console.printLine(ex.getMessage) *> ZIO.succeed(1))
}
