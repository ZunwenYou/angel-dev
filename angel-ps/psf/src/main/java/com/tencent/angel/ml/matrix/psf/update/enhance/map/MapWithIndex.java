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

package com.tencent.angel.ml.matrix.psf.update.enhance.map;

import com.tencent.angel.common.Serialize;
import com.tencent.angel.ml.matrix.psf.update.enhance.MFUpdateFunc;
import com.tencent.angel.ps.impl.matrix.ServerDenseDoubleRow;
import com.tencent.angel.ps.impl.matrix.ServerSparseDoubleLongKeyRow;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.nio.DoubleBuffer;

/**
 * It is a MapWithIndex function which applies `MapWithIndexFunc` to `fromId` row and saves the
 * result to `toId` row.
 */
public class MapWithIndex extends MFUpdateFunc {

  public MapWithIndex(int matrixId, int fromId, int toId, MapWithIndexFunc func) {
    super(matrixId, new int[]{fromId, toId}, func);
  }

  public MapWithIndex() {
    super();
  }

  @Override
  protected void doUpdate(ServerDenseDoubleRow[] rows, Serialize func) {

    MapWithIndexFunc mapper = (MapWithIndexFunc) func;
    DoubleBuffer from = rows[0].getData();
    DoubleBuffer to = rows[1].getData();
    int size = rows[0].size();
    int startCol = (int)rows[0].getStartCol();
    for (int i = 0; i < size; i++) {
      to.put(i, mapper.call(startCol + i, from.get(i)));
    }
  }

  @Override
  protected void doUpdate(ServerSparseDoubleLongKeyRow[] rows, Serialize func) {
    MapWithIndexFunc mapper = (MapWithIndexFunc) func;
    Long2DoubleOpenHashMap data1 = rows[0].getData();

    Long2DoubleOpenHashMap data2 = data1.clone();
    // TODO: a better way is needed to deal with defaultValue
    assert (data2.defaultReturnValue() == 0.0);

    for (java.util.Map.Entry<Long, Double> entry: data2.long2DoubleEntrySet()) {
      entry.setValue(mapper.call(entry.getKey().intValue(), entry.getValue()));
    }
    rows[1].setIndex2ValueMap(data2);
  }
}
