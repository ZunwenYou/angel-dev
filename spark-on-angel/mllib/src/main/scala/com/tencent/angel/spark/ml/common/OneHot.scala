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

package com.tencent.angel.spark.ml.common

import breeze.linalg.{DenseVector => BDV}
import org.apache.spark.mllib.linalg.DenseVector

import com.tencent.angel.spark.ml.common.OneHot.OneHotVector

/**
 * One hot feature is used by sparse and high-dimension learning algorithm.
 */
object OneHot {
  type OneHotVector = Array[Int]

  def axpy(a: Double, x: OneHotVector, y: DenseVector): Unit = {
    // require(x.max < y.size)
    x.foreach(index => y.values(index) = y(index) + a * 1)
  }

  def dot(x: OneHotVector, y: DenseVector): Double = {
    x.map(index => y(index)).sum
  }

  def dot(x: DenseVector, y: OneHotVector): Double = dot(y, x)

  def dot(x: OneHotVector, y: BDV[Double]): Double = {
    x.map(index => y(index)).sum
  }

  def dot(x: OneHotVector, y: Array[Double]): Double = {
    x.map(index => y(index)).sum
  }


}

/**
 * Class used to compute the gradient for a loss function, given a single data point.
 */
trait Gradient extends Serializable {
  def compute(data: OneHotVector, label: Double, weights: DenseVector): Double
  def compute(
    data: OneHotVector,
    label: Double,
    weights: DenseVector,
    gradient: DenseVector): Double
}

/**
 * Compute gradient and loss for a binary logistic loss function. Only Support binary
 * classification, so `numClasses` is 2.
 *
 * @param numClasses the number of possible outcomes for k classes classification problem in
 *                   Multinomial Logistic Regression. By default, it is binary logistic regression
 *                   so numClasses will be set to 2.
 */
class LogisticGradient(numClasses: Int = 2) extends Gradient {

  def this() = this(2)

  override def compute(data: OneHotVector, label: Double, weights: DenseVector): Double = {
    numClasses match {
      case 2 =>
        val margin = -1.0 * OneHot.dot(data, weights)
        if (label > 0) {
          // The following is equivalent to log(1 + exp(margin)) but more numerically stable.
          log1pExp(margin)
        } else {
          log1pExp(margin) - margin
        }
      case _ =>
        throw new Exception("Logistic can not support multiClass")
    }
  }

  override def compute (data: OneHotVector,
                        label: Double,
                        weights: DenseVector,
                        cumGradient: DenseVector): Double = {
    require(weights.size == cumGradient.size)
    numClasses match {
      case 2 =>
        val margin = -1.0 * OneHot.dot(data, weights)
        val multiplier = (1.0 / (1.0 + math.exp(margin))) - label
        OneHot.axpy(multiplier, data, cumGradient)
        if (label > 0) {
          // The following is equivalent to log(1 + exp(margin)) but more numerically stable.
          log1pExp(margin)
        } else {
          log1pExp(margin) - margin
        }
      case _ =>
        throw new Exception("Logistic can not support multiClass")
    }
  }

  def log1pExp(x: Double): Double = {
    if (x > 0) {
      x + math.log1p(math.exp(-x))
    } else {
      math.log1p(math.exp(x))
    }
  }
}



