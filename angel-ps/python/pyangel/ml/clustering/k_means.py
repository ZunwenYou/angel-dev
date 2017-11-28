#
# Tencent is pleased to support the open source community by making Angel available.
#
# Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
#
# Licensed under the BSD 3-Clause License (the "License") you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
#
# https:#opensource.org/licenses/BSD-3-Clause
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is
# distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. See the License for the specific language governing permissions and
#

from pyangel.conf import AngelConf
from pyangel.context import Configuration
from pyangel.ml.conf import MLConf
from pyangel.ml.client.angel_client_factory import AngelClientFactory
from pyangel.ml_runner import MLRunner

class KMeansRunner(MLRunner):
    """
    Training job to obtain a model
    """
    def train(self, conf):
        conf.set(AngelConf.ANGEL_TASK_USER_TASKCLASS, 'com.tencent.angel.ml.clustering.kmeans.KMeansTrainTask')

        # Create an angel job client
        client = AngelClientFactory.get(conf)

        # Submit this application
        client.startPSServer()

        # Create a KMeans model
        kmeansModel = conf._jvm.com.tencent.angel.ml.clustering.kmeans.KMeansModel(conf._jconf, None)

        # Load model meta to client
        client.loadModel(kmeansModel)

        # Start
        client.runTask('com.tencent.angel.ml.clustering.kmeans.KMeansTrainTask')

        # Run user task and wait for completion,
        # User task is set in AngelConf.ANGEL_TASK_USER_TASKCLASS
        client.waitForCompletion()

        # Save the trained model to HDFS
        client.saveModel(kmeansModel)

        # Stop
        client.stop()

    def predict(self, conf):
        """
        Using a model to predict with unobserved samples
        """
        conf.set_int("angel.worker.matrix.transfer.request.timeout.ms", 60000)
        conf.set(AngelConf.ANGEL_TASK_USER_TASKCLASS, 'com.tencent.angel.ml.clustering.kmeans.KMeansPredictTask')

        # Create an angel job client
        client = AngelClientFactory.get(conf)

        # Submit this application
        client.startPSServer()

        # Create KMeans model
        model = conf._jvm.com.tencent.angel.ml.clustering.kmeans.KMeansModel(conf._jconf, None)

        # Add the model meta to client
        client.loadModel(model)

        # Start
        client.runTask('com.tencent.angel.ml.clustering.kmeans.KMeansPredictTask')

        # Run user task and wait for completion,
        # User task is set in AngelConf.ANGEL_TASK_USER_TASKCLASS
        client.waitForCompletion()

        # Stop
        client.stop()
