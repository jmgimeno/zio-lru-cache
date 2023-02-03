package com.example

import zio._
import cache._

type IntLRUCache = LRUCache[Int, Int]

def getInt(key: Int): ZIO[IntLRUCache, NoSuchElementException, Int] =
  ZIO.serviceWithZIO(_.get(key))

def putInt(key: Int, value: Int): ZIO[IntLRUCache, Nothing, Unit] =
  ZIO.serviceWithZIO(_.put(key, value))

val getCacheStatus: ZIO[IntLRUCache, Nothing, (Map[Int, CacheItem[Int, Int]], Option[Int], Option[Int])] =
  ZIO.serviceWithZIO(_.getStatus)

object UseLRUCacheWithOneFiber extends ZIOAppDefault {
  val program: ZIO[IntLRUCache, Throwable, Int] =
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
        ZLayer.fromZIO {
          RefsLRUCache.make[Int, Int](2)
        }
      )
      .catchAll(ex => Console.printLine(ex.getMessage).orDie *> ZIO.succeed(1))

  private def get(key: Int): RIO[IntLRUCache, Unit] =
    (for {
      _ <- Console.printLine(s"Getting key: $key").orDie
      v <- getInt(key)
      _ <- Console.printLine(s"Obtained value: $v").orDie
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  private def put(key: Int, value: Int): URIO[IntLRUCache, Unit] =
    Console.printLine(s"Putting ($key, $value)").orDie *> putInt(key, value)
}

object UseLRUCacheWithMultipleFibers extends ZIOAppDefault {

  val producer: URIO[IntLRUCache, Unit] =
    for {
      number <- Random.nextIntBounded(100)
      _      <- Console.printLine(s"Producing ($number, $number)").orDie
      _      <- putInt(number, number)
    } yield ()

  val consumer: URIO[IntLRUCache, Unit] =
    (for {
      key   <- Random.nextIntBounded(100)
      _     <- Console.printLine(s"Consuming key: $key")
      value <- getInt(key)
      _     <- Console.printLine(s"Consumed value: $value")
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  val reporter: ZIO[IntLRUCache, NoSuchElementException, Unit] =
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
        ZLayer.fromZIO {
          RefsLRUCache.make[Int, Int](3)
        }
      )
      .catchAll(ex => Console.printLine(ex.getMessage).orDie *> ZIO.succeed(1))
}

object UseConcurrentLRUCacheWithMultipleFibers extends ZIOAppDefault {

  val producer: URIO[IntLRUCache, Unit] =
    for {
      number <- Random.nextIntBounded(100)
      _      <- Console.printLine(s"Producing ($number, $number)").orDie
      _      <- putInt(number, number)
    } yield ()

  val consumer: URIO[IntLRUCache, Unit] =
    (for {
      key   <- Random.nextIntBounded(100)
      _     <- Console.printLine(s"Consuming key: $key").orDie
      value <- getInt(key)
      _     <- Console.printLine(s"Consumed value: $value").orDie
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  val reporter: ZIO[IntLRUCache, NoSuchElementException, Unit] =
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
        ZLayer.fromZIO {
          STMLRUCache.make[Int, Int](3)
        }
      )
      .catchAll(ex => Console.printLine(ex.getMessage) *> ZIO.succeed(1))
}
