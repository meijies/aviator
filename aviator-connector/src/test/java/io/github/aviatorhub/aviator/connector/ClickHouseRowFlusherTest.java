package io.github.aviatorhub.aviator.connector;

import io.github.aviatorhub.aviator.connector.clickhouse.ClickHouseRowFlusher;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.types.AtomicDataType;
import org.apache.flink.table.types.logical.IntType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.ClickHouseStatement;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

/**
 * @author meijie
 * @since 0.0.1
 */
public class ClickHouseRowFlusherTest {

  @Rule
  protected ClickHouseContainer clickhouse = new ClickHouseContainer(
      DockerImageName.parse("yandex/clickhouse-server").withTag("20.8.19.4"));

  private ClickHouseDataSource dataSource;
  private ClickHouseRowFlusher flusher;

  @Before
  public void before() throws Exception {
    // create table
    ClickHouseProperties properties = new ClickHouseProperties();
    properties.setUser(clickhouse.getUsername());
    properties.setPassword(clickhouse.getPassword());
    dataSource = new ClickHouseDataSource(clickhouse.getJdbcUrl(), properties);
    createTable(generateSql());

    ConnectorConf conf = new ConnectorConf();
    conf.setPasswd(clickhouse.getPassword());
    conf.setUser(clickhouse.getUsername());
    conf.setAddress(clickhouse.getJdbcUrl());
    flusher = new ClickHouseRowFlusher(conf, generateColList());
  }

  private void createTable(String sql) throws SQLException {
    try (ClickHouseConnection conn = dataSource.getConnection();
        ClickHouseStatement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  private static final List<String> TYPE_LIST = Arrays.asList(
      "UInt8", "UInt16", "UInt32", "UInt64", "Int8", "Int16", "Int32", "Int64",
      "Float32", "Float64", "DateTime", "Date", "String"
  );

  private String generateSql() {
    StringBuilder tableBuilder = new StringBuilder("CREATE TABLE ck_flusher_test (");
    List<String> columnList = TYPE_LIST.stream()
        .map(type -> "col_" + type + " " + type + ", \n")
        .collect(Collectors.toList());
    List<String> nullColumnList = TYPE_LIST.stream()
        .map(type -> "col_NULL_" + type + " Nullable(" + type + "), \n")
        .collect(Collectors.toList());
    columnList.addAll(nullColumnList);
    tableBuilder.append(StringUtils.join(columnList.toArray()))
        .append(") ENGINE = MergeTree() \n")
        .append(" PARTITION BY (col_UInt8) \n")
        .append(" ORDER BY (col_UInt16)");
    return tableBuilder.toString();
  }

  private List<Column> generateColList() {
    List<Column> colList = new LinkedList<>();
    colList.addAll(TYPE_LIST.stream()
        .map(type -> typeToColumn(type, false))
        .collect(Collectors.toList()));
    colList.addAll(TYPE_LIST.stream()
        .map(type -> typeToColumn(type, true))
        .collect(Collectors.toList()));
    return colList;
  }

  private Column typeToColumn(String type, boolean nullable) {
    String colName = nullable ? "COL_" + type : "COL_null_" + type;
    switch (type) {
      case "UInt8":
        return Column.physical(colName,new AtomicDataType(new IntType()));
    }
    // TODO 重构 ClickHouse schema 验证。
    return null;
  }

  @Test
  public void testValidate() throws SQLException {

  }


  @Test
  public void testFlush() throws SQLException {

  }

}
