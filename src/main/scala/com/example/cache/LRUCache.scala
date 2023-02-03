package com.example.cache

import zio._

trait LRUCache[K, V] {
  def get(key: K): IO[NoSuchElementException, V] 
  def put(key: K, value: V): UIO[Unit] 
  def getStatus: UIO[(Map[K, CacheItem[K, V]], Option[K], Option[K])]
}
