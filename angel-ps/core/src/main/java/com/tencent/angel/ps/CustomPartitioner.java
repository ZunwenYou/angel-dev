package com.tencent.angel.ps;

/**
 * Created by tcheng on 29/11/2017.
 */

import com.tencent.angel.conf.AngelConf;
import com.tencent.angel.ml.matrix.MatrixContext;
import com.tencent.angel.protobuf.generated.MLProtos.Partition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.util.ArrayList;
import java.util.List;

public class CustomPartitioner implements Partitioner{
    private static final Log LOG = LogFactory.getLog(CustomPartitioner.class);
    private static final long DEFAULT_PARTITION_SIZE_DENSE = 500000;
    private static final long DEFAULT_PARTITION_SIZE_SPARSE_100000000 = 500000;
    private static final long DEFAULT_PARTITION_SIZE_SPARSE_1000000000 = 5000000;
    private static final long DEFAULT_PARTITION_SIZE_SPARSE_10000000000 = 50000000;
    protected MatrixContext mContext;
    protected Configuration conf;

    @Override
    public void init(MatrixContext mtx, Configuration conf) {
        this.mContext = mtx;
        this.conf = conf;
    }

    @Override
    public List<Partition> getPartitions() {
        List<Partition> array = new ArrayList<Partition>();
        int id = 0;
        int matrixId = mContext.getId();
        int row = mContext.getRowNum();
        long col = mContext.getColNum();

        long defaultPartSize = getDefaultPartSize();

        int blockRow = mContext.getMaxRowNumInBlock();
        long blockCol = mContext.getMaxColNumInBlock();
        if(blockRow == -1 || blockCol == -1) {
            int serverNum = conf.getInt(AngelConf.ANGEL_PS_NUMBER, AngelConf.DEFAULT_ANGEL_PS_NUMBER);
            if(row >= serverNum) {
                blockRow = (int) Math.min(row / serverNum, Math.max(1, defaultPartSize / col));
                blockCol = Math.min(defaultPartSize / blockRow, col);
            } else {
                blockRow = row;
                blockCol = Math.min(defaultPartSize / blockRow, Math.max(100, col / serverNum));
            }
        }

        LOG.info("blockRow = " + blockRow + ", blockCol=" + blockCol);

        Partition.Builder partition = Partition.newBuilder();
        for (int i = 0; i < row; ) {
            for (long j = 0; j < col; ) {
                int startRow = i;
                long startCol = j;
                int endRow = (i <= (row - blockRow)) ? (i + blockRow) : row;
                long endCol = (j <= (col - blockCol)) ? (j + blockCol) : col;
                partition.setMatrixId(matrixId);
                partition.setPartitionId(id++);
                partition.setStartRow(startRow);
                partition.setStartCol(startCol);
                partition.setEndRow(endRow);
                partition.setEndCol(endCol);
                array.add(partition.build());

                j = (j <= (col - blockCol)) ? (j + blockCol) : col;
            }
            i = (i <= (row - blockRow)) ? (i + blockRow) : row;
        }
        LOG.debug("partition count: " + array.size());
        return array;
    }

    private long getDefaultPartSize() {
        long maxSize = mContext.getColNum() * mContext.getRowNum();
        switch (mContext.getRowType()) {
            case T_DOUBLE_DENSE:
            case T_FLOAT_DENSE:
            case T_INT_DENSE:
                return DEFAULT_PARTITION_SIZE_DENSE;

            default:{
                if(maxSize < 100000000) {
                    return DEFAULT_PARTITION_SIZE_SPARSE_100000000;
                } else if(maxSize >= 100000000 && maxSize < 1000000000) {
                    return DEFAULT_PARTITION_SIZE_SPARSE_1000000000;
                } else if(maxSize >= 1000000000 && maxSize < 10000000000L){
                    return DEFAULT_PARTITION_SIZE_SPARSE_10000000000;
                } else {
                    int psNum = conf.getInt(AngelConf.ANGEL_PS_NUMBER, AngelConf.DEFAULT_ANGEL_PS_NUMBER);
                    return maxSize / psNum / 10;
                }
            }
        }
    }

    @Override
    public int assignPartToServer(int partId) {
        int serverNum = conf.getInt(AngelConf.ANGEL_PS_NUMBER, AngelConf.DEFAULT_ANGEL_PS_NUMBER);
        return partId % serverNum;
    }

    protected MatrixContext getMatrixContext() {
        return mContext;
    }
}
