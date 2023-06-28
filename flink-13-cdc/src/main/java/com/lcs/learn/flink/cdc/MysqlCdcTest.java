package com.lcs.learn.flink.cdc;

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.catalog.ObjectPath;

/**
 * Created by lcs on 2023/6/13.
 *
 * @author lcs
 */
public class MysqlCdcTest {

    public static void main(String[] args) throws Exception {
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .startupOptions(StartupOptions.initial())
                .hostname("10.1.4.139")
                .port(3306)
                .databaseList("test")
                //.tableList("test.t_user_info")
                .tableList("test.t_user_no_primary")
                //.tableList("test.t_user_info.*")
                .username("biapp")
                .password("biapp")
                .chunkKeyColumn(new ObjectPath("test","t_user_no_primary"),"name")
                .includeSchemaChanges(true)
                .deserializer(new JsonDebeziumDeserializationSchema())
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment();

        }

        env.enableCheckpointing(3000);

        env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
                .setParallelism(1)
                .print().setParallelism(1);

        env.execute("Print MySQL Snapshot + Binlog");
    }
}
