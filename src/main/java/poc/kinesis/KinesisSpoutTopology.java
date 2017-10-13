/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package poc.kinesis;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.model.ShardIteratorType;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kinesis.spout.*;
import org.apache.storm.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class KinesisSpoutTopology {
    private static final Logger LOG = LoggerFactory.getLogger(SampleTopology.class);

    public static void main (String args[]) throws InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        String mode = null;
        mode = args[0];
        RecordToTupleMapper recordToTupleMapper = new TestRecordToTupleMapper();
        KinesisConnectionInfo kinesisConnectionInfo = new KinesisConnectionInfo(new CredentialsProviderChain(), new ClientConfiguration(), Regions.US_WEST_2,
                1000);
        ZkInfo zkInfo = new ZkInfo("zookeeper/172.17.0.6:2181", "/kinesisOffsets", 20000, 15000, 10000L, 3, 2000);
        KinesisConfig kinesisConfig = new KinesisConfig("frontend-classroom-events", ShardIteratorType.TRIM_HORIZON,
                recordToTupleMapper, new Date(), new ExponentialBackoffRetrier(), zkInfo, kinesisConnectionInfo, 10000L);
        KinesisSpout kinesisSpout = new KinesisSpout(kinesisConfig);
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setSpout("spout", kinesisSpout, 3);
        topologyBuilder.setBolt("bolt", new KinesisBoltTest(), 1).shuffleGrouping("spout");
        Config topologyConfig = new Config();
        topologyConfig.setDebug(true);
        topologyConfig.setNumWorkers(3);


        if (mode.equals("LocalMode")) {
            LOG.info("Starting sample storm topology in LocalMode ...");
            new LocalCluster().submitTopology("SampleTopology", topologyConfig, topologyBuilder.createTopology());
        }else{
            LOG.info("Starting sample storm topology in RemoteMode ...");
            StormSubmitter.submitTopology("SampleTopology", topologyConfig, topologyBuilder.createTopology());
        }
    }
}
