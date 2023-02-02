package com.example.cache

import zio._
import zio.test.Assertion._
import zio.test._

object ConcurrentLRUCacheTest extends ZIOSpecDefault {
  def spec = suite("ConcurrentLRUCache")(
    test("can't be created with non-positive capacity") {
      assertZIO(ConcurrentLRUCache.make[Int, Int](0).exit)(
        fails(hasMessage(equalTo("Capacity must be a positive number!")))
      )
    },
    test("works as expected") {
      val expectedOutput = Vector(
        "Putting (1, 1)\n",
        "Putting (2, 2)\n",
        "Getting key: 1\n",
        "Obtained value: 1\n",
        "Putting (3, 3)\n",
        "Getting key: 2\n",
        "Key does not exist: 2\n",
        "Putting (4, 4)\n",
        "Getting key: 1\n",
        "Key does not exist: 1\n",
        "Getting key: 3\n",
        "Obtained value: 3\n",
        "Getting key: 4\n",
        "Obtained value: 4\n"
      )
      for {
        lruCache <- ConcurrentLRUCache.make[Int, Int](2)
        _        <- put(lruCache, 1, 1)
        _        <- put(lruCache, 2, 2)
        _        <- get(lruCache, 1)
        _        <- put(lruCache, 3, 3)
        _        <- get(lruCache, 2)
        _        <- put(lruCache, 4, 4)
        _        <- get(lruCache, 1)
        _        <- get(lruCache, 3)
        _        <- get(lruCache, 4)
        output   <- TestConsole.output
      } yield {
        assert(output)(equalTo(expectedOutput))

      }
    }
  )

  private def get[K, V](concurrentLruCache: ConcurrentLRUCache[K, V], key: K): ZIO[Any, Nothing, Unit] =
    (for {
      _ <- Console.printLine(s"Getting key: $key")
      v <- concurrentLruCache.get(key)
      _ <- Console.printLine(s"Obtained value: $v")
    } yield ()).catchAll(ex => Console.printLine(ex.getMessage).orDie)

  private def put[K, V](concurrentLruCache: ConcurrentLRUCache[K, V], key: K, value: V): ZIO[Any, Nothing, Unit] =
    Console.printLine(s"Putting ($key, $value)").orDie *> concurrentLruCache.put(key, value)
}
