package com.lcs.learn.debezium.cdc;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lcs on 2023/6/16.
 *
 * @author lcs
 */
public class DebeziumCdcTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Define the configuration for the Debezium Engine with MySQL connector...
        final Properties props = new Properties();
        props.setProperty("name", "engine");
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        // windows下相当于是代码所在盘的起始路径
        props.setProperty("offset.storage.file.filename", "/tmp/offsets.dat");
        props.setProperty("offset.flush.interval.ms", "10000");
        /* begin connector properties */
        props.setProperty("database.hostname", "10.1.4.139");
        props.setProperty("database.port", "3306");
        props.setProperty("database.user", "biapp");
        props.setProperty("database.password", "biapp");
        props.setProperty("database.server.id", "1");
        props.setProperty("database.server.name", "binlog-test-lcs2");
        // 多个用英文逗号分隔
        //props.setProperty("database.include.list", "test");
        props.setProperty("table.include.list", "test.order_rel");
        props.setProperty("include.schema.change", "true");
        props.setProperty("database.history.kafka.bootstrap.servers", "edc01.com:49091,edc03.com:49091");
        props.setProperty("database.history.kafka.topic", "binlogtest");
        props.setProperty("snapshot.mode", "initial");

        props.setProperty("database.history", "io.debezium.relational.history.KafkaDatabaseHistory");
        props.setProperty("database.history.file.filename", "/path/to/storage/schemahistory2.dat");

        DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying((record, committer) -> {
                    for (ChangeEvent<String, String> r : record) {
                        System.out.println("Key = '" + r.key() + "' value = '" + r.value() + "'");
                        // 有commit就会提交offset文件
                        committer.markProcessed(r);
                        committer.markBatchFinished();
                    }
                })
                .build();
        // Run the engine asynchronously ...
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(engine);

        // 这里休眠下，避免抛出Interrupted while emitting initial DROP TABLE events异常
        //
        // Do something else or wait for a signal or an event
        // Thread.sleep(10000);
        // 最后如果出问题时需要关闭Executors
        // Engine is stopped when the main code is finished
    }
}
