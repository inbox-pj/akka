/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.ddata

import akka.cluster.Cluster
import akka.cluster.UniqueAddress
import java.math.BigInteger

object PNCounterMap {
  def empty[A]: PNCounterMap[A] = new PNCounterMap(ORMap.empty)
  def apply[A](): PNCounterMap[A] = empty
  /**
   * Java API
   */
  def create[A](): PNCounterMap[A] = empty

  /**
   * Extract the [[PNCounterMap#entries]].
   */
  def unapply[A](m: PNCounterMap[A]): Option[Map[A, BigInt]] = Some(m.entries)
}

/**
 * Map of named counters. Specialized [[ORMap]] with [[PNCounter]] values.
 *
 * This class is immutable, i.e. "modifying" methods return a new instance.
 */
@SerialVersionUID(1L)
final class PNCounterMap[A] private[akka] (
  private[akka] val underlying: ORMap[A, PNCounter])
  extends ReplicatedData with ReplicatedDataSerialization with RemovedNodePruning {

  type T = PNCounterMap[A]

  /** Scala API */
  def entries: Map[A, BigInt] = underlying.entries.map { case (k, c) ⇒ k → c.value }

  /** Java API */
  def getEntries: java.util.Map[A, BigInteger] = {
    import scala.collection.JavaConverters._
    underlying.entries.map { case (k, c) ⇒ k → c.value.bigInteger }.asJava
  }

  /**
   *  Scala API: The count for a key
   */
  def get(key: A): Option[BigInt] = underlying.get(key).map(_.value)

  /**
   * Java API: The count for a key, or `null` if it doesn't exist
   */
  def getValue(key: A): BigInteger = underlying.get(key).map(_.value.bigInteger).orNull

  def contains(key: A): Boolean = underlying.contains(key)

  def isEmpty: Boolean = underlying.isEmpty

  def size: Int = underlying.size

  /**
   * Increment the counter with the delta specified.
   * If the delta is negative then it will decrement instead of increment.
   */
  def increment(key: A, delta: Long = 1)(implicit node: Cluster): PNCounterMap[A] =
    increment(node, key, delta)

  /**
   * Increment the counter with the delta specified.
   * If the delta is negative then it will decrement instead of increment.
   */
  def increment(node: Cluster, key: A, delta: Long): PNCounterMap[A] =
    increment(node.selfUniqueAddress, key, delta)

  /**
   * INTERNAL API
   */
  private[akka] def increment(node: UniqueAddress, key: A, delta: Long): PNCounterMap[A] =
    new PNCounterMap(underlying.updated(node, key, PNCounter())(_.increment(node, delta)))

  /**
   * Decrement the counter with the delta specified.
   * If the delta is negative then it will increment instead of decrement.
   */
  def decrement(key: A, delta: Long = 1)(implicit node: Cluster): PNCounterMap[A] =
    decrement(node, key, delta)

  /**
   * Decrement the counter with the delta specified.
   * If the delta is negative then it will increment instead of decrement.
   */
  def decrement(node: Cluster, key: A, delta: Long): PNCounterMap[A] =
    decrement(node.selfUniqueAddress, key, delta)

  /**
   * INTERNAL API
   */
  private[akka] def decrement(node: UniqueAddress, key: A, delta: Long): PNCounterMap[A] = {
    new PNCounterMap(underlying.updated(node, key, PNCounter())(_.decrement(node, delta)))
  }

  /**
   * Removes an entry from the map.
   * Note that if there is a conflicting update on another node the entry will
   * not be removed after merge.
   */
  def -(key: A)(implicit node: Cluster): PNCounterMap[A] = remove(node, key)

  /**
   * Removes an entry from the map.
   * Note that if there is a conflicting update on another node the entry will
   * not be removed after merge.
   */
  def remove(node: Cluster, key: A): PNCounterMap[A] =
    remove(node.selfUniqueAddress, key)

  /**
   * INTERNAL API
   */
  private[akka] def remove(node: UniqueAddress, key: A): PNCounterMap[A] =
    new PNCounterMap(underlying.remove(node, key))

  override def merge(that: PNCounterMap[A]): PNCounterMap[A] =
    new PNCounterMap(underlying.merge(that.underlying))

  override def modifiedByNodes: Set[UniqueAddress] =
    underlying.modifiedByNodes

  override def needPruningFrom(removedNode: UniqueAddress): Boolean =
    underlying.needPruningFrom(removedNode)

  override def prune(removedNode: UniqueAddress, collapseInto: UniqueAddress): PNCounterMap[A] =
    new PNCounterMap(underlying.prune(removedNode, collapseInto))

  override def pruningCleanup(removedNode: UniqueAddress): PNCounterMap[A] =
    new PNCounterMap(underlying.pruningCleanup(removedNode))

  // this class cannot be a `case class` because we need different `unapply`

  override def toString: String = s"PNCounter$entries"

  override def equals(o: Any): Boolean = o match {
    case other: PNCounterMap[A] ⇒ underlying == other.underlying
    case _                      ⇒ false
  }

  override def hashCode: Int = underlying.hashCode
}

object PNCounterMapKey {
  def create[A](id: String): Key[PNCounterMap[A]] = PNCounterMapKey[A](id)
}

@SerialVersionUID(1L)
final case class PNCounterMapKey[A](_id: String) extends Key[PNCounterMap[A]](_id) with ReplicatedDataSerialization