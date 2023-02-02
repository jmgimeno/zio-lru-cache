package com.example

import zio._

package object cache {

  type IntLRUCacheEnv = IntLRUCacheEnv.Service

  object IntLRUCacheEnv {

    trait Service {
      def getInt(key: Int): IO[NoSuchElementException, Int]
      def putInt(key: Int, value: Int): UIO[Unit]
      val getCacheStatus: UIO[(Map[Int, CacheItem[Int, Int]], Option[Int], Option[Int])]
    }

    object Service {

      val zioRefImpl: ZLayer[Int, IllegalArgumentException, IntLRUCacheEnv] =
        ZLayer.fromZIO {
          ZIO.service[Int].flatMap { n =>
            LRUCache
              .make[Int, Int](n)
              .map { lruCache =>
                new Service {
                  def getInt(key: Int): IO[NoSuchElementException, Int] =
                    lruCache.get(key)
                  def putInt(key: Int, value: Int): UIO[Unit] =
                    lruCache.put(key, value)
                  val getCacheStatus: UIO[(Map[Int, CacheItem[Int, Int]], Option[Int], Option[Int])] =
                    lruCache.getStatus
                }
              }
          }
        }

      val zioStmImpl: ZLayer[Int, IllegalArgumentException, IntLRUCacheEnv] =
        ZLayer.fromZIO {
          for {
            n                  <- ZIO.service[Int]
            concurrentLruCache <- ConcurrentLRUCache.make[Int, Int](n)
          } yield new Service {
            def getInt(key: Int): IO[NoSuchElementException, Int] =
              concurrentLruCache.get(key)
            def putInt(key: Int, value: Int): UIO[Unit] =
              concurrentLruCache.put(key, value)
            val getCacheStatus: UIO[(Map[Int, CacheItem[Int, Int]], Option[Int], Option[Int])] =
              concurrentLruCache.getStatus
          }
        }
    }
  }

  def getInt(key: Int): ZIO[IntLRUCacheEnv, NoSuchElementException, Int] =
    ZIO.serviceWithZIO(_.getInt(key))

  def putInt(key: Int, value: Int): ZIO[IntLRUCacheEnv, Nothing, Unit] =
    ZIO.serviceWithZIO(_.putInt(key, value))

  val getCacheStatus: ZIO[IntLRUCacheEnv, Nothing, (Map[Int, CacheItem[Int, Int]], Option[Int], Option[Int])] =
    ZIO.serviceWithZIO(_.getCacheStatus)
}
