/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.models.vector.enhanced

import com.tencent.angel.spark.client.PSClient
import com.tencent.angel.spark.models.vector.PSVector

/**
 * CachedPSVector implements a more efficient Vector, which can benefit multi tasks on one executor
 */

private[spark] class CachedPSVector(component: PSVector) extends PSVectorDecorator(component) {

  override val dimension = component.dimension
  override val id  = component.id
  override val poolId  = component.poolId

  def pullFromCache(): Array[Double] = {
    PullMan.pullFromCache(this)
  }

  /**
    * Increment a local dense Double array to PSVector
    * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
    */
  def incrementWithCache(delta: Array[Double]): Unit = {
    val mergedArray = PushMan.getFromIncrementCache(this)
    PSClient.instance().BLAS.daxpy(this.dimension.toInt, 1.0, delta, 1, mergedArray, 1)
  }

  /**
    * Increment a local sparse Double array to PSVector
    * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
    */
  def incrementWithCache(indices: Array[Int], values: Array[Double]): Unit = {
    require(indices.length == values.length && indices.length <= this.dimension)
    val mergedArray = PushMan.getFromIncrementCache(this)
    var i = 0
    for(indict <- indices){
      mergedArray(indict) += values(i)
      i += 1
    }
  }

  def flushIncrement(): Unit = PushMan.flush(poolId, id, MergeType.INCREMENT)
  def flushMax(): Unit = PushMan.flush(poolId, id, MergeType.MAX)
  def flushMin(): Unit = PushMan.flush(poolId, id, MergeType.MIN)

  // =======================================================================
  // Merge Operator
  // =======================================================================

  /**
   * Find the maximum number of each dimension for PSVector and local dense array.
   * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
   */
  def mergeMaxWithCache(other: Array[Double]): Unit = {
    require(other.length == dimension)
    val mergedArray = PushMan.getFromMaxCache(this)
    var i = 0
      while (i < dimension) {
        mergedArray(i) = math.max(mergedArray(i), other(i))
        i += 1
      }
  }

  /**
   * Find the maximum number of each dimension for PSVector and local sparse array.
   * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
   */
  def mergeMaxWithCache(indices: Array[Int], values: Array[Double]): Unit = {
    require(indices.length == values.length && indices.length <= dimension)
    val mergedArray = PushMan.getFromMaxCache(this)
    var i = 0
    while (i < indices.length) {
      val index = indices(i)
      mergedArray(index) = math.max(mergedArray(index), values(i))
      i += 1
    }
  }

  /**
   * Find the maximum number of each dimension for PSVector and local sparse array, and flushOne to
   * PS nodes immediately
   */
  def mergeMax(other: Array[Double]): Unit = {
    PSClient.instance().vectorOps.mergeMax(this, other)
  }

  /**
   * Find the maximum number of each dimension for PSVector and local dense array.
   * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
   */
  def mergeMinWithCache(other: Array[Double]): Unit = {
    require(other.length == dimension)
    val mergedArray = PushMan.getFromMinCache(this)
    var i = 0
    while (i < mergedArray.length) {
      mergedArray(i) = math.min(mergedArray(i), other(i))
      i += 1
    }
  }

  /**
   * Find the minimum number of each dimension for PSVector and local sparse array.
   * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
   */
  def mergeMinWithCache(indices: Array[Int], values: Array[Double]): Unit = {
    require(indices.length == values.length && indices.length <= dimension)
    val mergedArray = PushMan.getFromMinCache(this)
    var i = 0
    while (i < indices.length) {
      val index = indices(i)
      mergedArray(index) = math.min(mergedArray(index), values(i))
      i += 1
    }
  }

  /**
   * Find the minimum number of each dimension for PSVector and local dense array.
   * Notice: You should call `flushOne` to flushOne `mergedArray` result to PS nodes.
   */
  def mergeMin(delta: Array[Double]): Unit = {
    PSClient.instance().vectorOps.mergeMin(this, delta)
  }

  override def delete(): Unit = component.delete()


}

