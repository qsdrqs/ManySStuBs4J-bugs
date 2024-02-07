/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.raptor.metadata;

import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public interface SchemaDao
{
    @SqlUpdate("CREATE TABLE IF NOT EXISTS tables (\n" +
            "  table_id bigint PRIMARY KEY AUTO_INCREMENT,\n" +
            "  schema_name varchar(255) NOT NULL,\n" +
            "  table_name varchar(255) NOT NULL,\n" +
            "  temporal_column_id bigint DEFAULT NULL,\n" +
            "  compaction_enabled boolean NOT NULL,\n" +
            "  UNIQUE (schema_name, table_name)\n" +
            ")")
    void createTableTables();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS columns (\n" +
            "  table_id bigint NOT NULL,\n" +
            "  column_id bigint NOT NULL,\n" +
            "  column_name varchar(255) NOT NULL,\n" +
            "  ordinal_position int NOT NULL,\n" +
            "  data_type varchar(255) NOT NULL,\n" +
            "  sort_ordinal_position int DEFAULT NULL,\n" +
            "  PRIMARY KEY (table_id, column_id),\n" +
            "  UNIQUE (table_id, column_name),\n" +
            "  UNIQUE (table_id, ordinal_position),\n" +
            "  FOREIGN KEY (table_id) REFERENCES tables (table_id)\n" +
            ")")
    void createTableColumns();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS views (\n" +
            "  schema_name varchar(255) NOT NULL,\n" +
            "  table_name varchar(255) NOT NULL,\n" +
            "  data text NOT NULL,\n" +
            "  PRIMARY KEY (schema_name, table_name)\n" +
            ")")
    void createTableViews();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS nodes (\n" +
            "  node_id INT PRIMARY KEY AUTO_INCREMENT,\n" +
            "  node_identifier VARCHAR(255) NOT NULL,\n" +
            "  UNIQUE (node_identifier)\n" +
            ")")
    void createTableNodes();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS shards (\n" +
            "  shard_id BIGINT PRIMARY KEY AUTO_INCREMENT,\n" +
            "  shard_uuid BINARY(16) NOT NULL,\n" +
            "  table_id BIGINT NOT NULL,\n" +
            "  create_time DATETIME NOT NULL,\n" +
            "  row_count BIGINT NOT NULL,\n" +
            "  compressed_size BIGINT NOT NULL,\n" +
            "  uncompressed_size BIGINT NOT NULL,\n" +
            "  UNIQUE (shard_uuid),\n" +
            "  FOREIGN KEY (table_id) REFERENCES tables (table_id)\n" +
            ")")
    void createTableShards();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS shard_nodes (\n" +
            "  shard_id BIGINT NOT NULL,\n" +
            "  node_id INT NOT NULL,\n" +
            "  PRIMARY KEY (shard_id, node_id),\n" +
            "  FOREIGN KEY (shard_id) REFERENCES shards (shard_id),\n" +
            "  FOREIGN KEY (node_id) REFERENCES nodes (node_id)\n" +
            ")")
    void createTableShardNodes();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS external_batches (\n" +
            "  external_batch_id VARCHAR(255) PRIMARY KEY,\n" +
            "  successful BOOLEAN NOT NULL\n" +
            ")")
    void createTableExternalBatches();
}
