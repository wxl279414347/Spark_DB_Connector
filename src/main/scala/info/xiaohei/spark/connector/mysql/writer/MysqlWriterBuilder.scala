package info.xiaohei.spark.connector.mysql.writer

import java.sql.{DriverManager, PreparedStatement}

/**
  * Author: xiaohei
  * Date: 2017/3/26
  * Email: yuande.jiang@fugetech.com
  * Host: xiaohei.info
  */
case class MysqlWriterBuilder[C](
                                            //todo:从conf中读取
                                            private[mysql] val connectStr: String,
                                            private[mysql] val username: String,
                                            private[mysql] val password: String,
                                            private[mysql] val collectionData: Iterable[C],
                                            //todo:t.productIterator.foreach{ i =>println("Value = " + i )}
                                            private[mysql] val fitStatement: (PreparedStatement, C) => Unit,
                                            private[mysql] val columns: Iterable[String] = Seq.empty,
                                            private[mysql] val tableName: Option[String] = None
                                          ) {
  def insert(cols: String*) = {
    require(this.columns.isEmpty, "Columns haven't been set")
    require(cols.nonEmpty, "Columns must by set,at least one")

    this.copy(columns = cols)
  }

  def toTable(table: String) = {
    require(this.tableName.isEmpty, "Default table hasn't been set")
    require(table.nonEmpty, "Table must provided")

    this.copy(tableName = Some(table))
  }

  //todo:where
}

private[mysql] class MysqlWriterBuildMaker[C](collectionData: Iterable[C]) extends Serializable {
  def toMysql(connectStr: String,
              username: String,
              password: String,
              fitStatement: (PreparedStatement, C) => Unit) =
    MysqlWriterBuilder[C](connectStr, username, password, collectionData, fitStatement)
}

private[mysql] class MysqlWriter[C](builder: MysqlWriterBuilder[C])
  extends Serializable {
  def save(): Unit = {
    val conn = DriverManager.getConnection(builder.connectStr, builder.username, builder.password)

    var placeholder = ""
    //todo:改进
    for (i <- 0 until builder.columns.size) placeholder += "?,"
    val sql = s"insert into ${builder.tableName}(${builder.columns.mkString(",")}) values(${placeholder.substring(0, placeholder.length)})"
    println(sql)
    val ps = conn.prepareStatement(sql)
    Class.forName("com.mysql.jdbc.Driver")
    try {
      builder.collectionData.foreach(x => builder.fitStatement(ps, x))
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      if (ps != null) {
        ps.close()
      }
      if (conn != null) {
        conn.close()
      }
    }
  }
}

trait MysqlWriterBuilderConversions extends Serializable {
  implicit def mysqlCollectionToBuildMaker[C](collectionData: Iterable[C])
  : MysqlWriterBuildMaker[C] = new MysqlWriterBuildMaker[C](collectionData)

  implicit def mysqlCollectionBuilderToWriter[C](builder: MysqlWriterBuilder[C])
  : MysqlWriter[C] = new MysqlWriter[C](builder)
}