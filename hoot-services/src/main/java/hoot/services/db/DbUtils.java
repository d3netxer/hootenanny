/*
 * This file is part of Hootenanny.
 *
 * Hootenanny is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * --------------------------------------------------------------------
 *
 * The following copyright notices are generated automatically. If you
 * have a new notice to add, please use the format:
 * " * @copyright Copyright ..."
 * This will properly maintain the copyright information. DigitalGlobe
 * copyrights will be updated automatically.
 *
 * @copyright Copyright (C) 2013, 2014, 2015 DigitalGlobe (http://www.digitalglobe.com/)
 */
package hoot.services.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hoot.services.HootProperties;
import hoot.services.db2.CurrentNodes;
import hoot.services.db2.CurrentRelations;
import hoot.services.db2.CurrentWays;
import hoot.services.db2.JobStatus;
import hoot.services.db2.Maps;
import hoot.services.db2.QChangesets;
import hoot.services.db2.QCurrentNodes;
import hoot.services.db2.QCurrentRelationMembers;
import hoot.services.db2.QCurrentRelations;
import hoot.services.db2.QCurrentWayNodes;
import hoot.services.db2.QCurrentWays;
import hoot.services.db2.QElementIdMappings;
import hoot.services.db2.QJobStatus;
import hoot.services.db2.QMaps;
import hoot.services.db2.QReviewItems;
import hoot.services.db2.QReviewMap;
import hoot.services.db2.QUsers;
import hoot.services.geo.GeoUtils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.math.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.PostgresTemplates;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.sql.types.EnumAsObjectType;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.NumberExpression;
import com.mysema.query.types.query.ListSubQuery;
import com.mysema.query.types.template.NumberTemplate;

/**
 * General Hoot services database utilities
 */
public class DbUtils
{
  private static final Logger log = LoggerFactory.getLogger(DbUtils.class);

  public static final String TIMESTAMP_DATE_FORMAT = "YYYY-MM-dd HH:mm:ss";

  protected static SQLTemplates templates = null;
  //protected static Configuration configuration = null;

  protected static org.apache.commons.dbcp.BasicDataSource dbcp_datasource = null;
  private static ClassPathXmlApplicationContext appContext = null;

  /**
   * The types of operations that can be performed on an OSM element from a
   * changeset upload request
   */
  public enum EntityChangeType
  {
    CREATE,
    MODIFY,
    DELETE
  }

  /**
   * The types of operations that can be performed when writing OSM data to the
   * services database
   */
  public enum RecordBatchType
  {
    INSERT,
    UPDATE,
    DELETE
  }



	public enum nwr_enum {
		 node,
		 way,
		 relation
	}


	public enum review_status_enum
	{
		unreviewed,
		reviewed
	}
  /**
   * Returns a record batch type given the corresponding entity change type
   *
   * @param entityChangeType an entity change type
   * @return a record batch type
   */
  public static RecordBatchType recordBatchTypeForEntityChangeType(
    final EntityChangeType entityChangeType)
  {
    switch (entityChangeType)
    {
      case CREATE:

        return RecordBatchType.INSERT;

      case MODIFY:

        return RecordBatchType.UPDATE;

      case DELETE:

        return RecordBatchType.DELETE;

      default:

        assert (false);
        return null;
    }
  }


  public static Configuration getConfiguration()
  {
  	return getConfiguration("");
  }


  public static Configuration getConfiguration(final long mapId)
  {
  	return getConfiguration("" + mapId);
  }


  public static Configuration getConfiguration(String mapId)
  {
  	if(templates ==  null)
  	{
  		//templates = new PostgresTemplates();
  		templates = PostgresTemplates.builder()
  				.quote()
  		    .build();
  	}

  	Configuration configuration = new Configuration(templates);
  		configuration.register("current_relation_members", "member_type",  new EnumAsObjectType<nwr_enum>(nwr_enum.class));
    	configuration.register("element_id_mappings", "osm_element_type",  new EnumAsObjectType<nwr_enum>(nwr_enum.class));
    	configuration.register("review_items", "review_status",  new EnumAsObjectType<review_status_enum>(review_status_enum.class));
  	if(mapId != null && mapId.length() > 0)
  	{
  		overrideTable(mapId, configuration);
  	}



  	return configuration;
  }

  public static void overrideTable(String mapId, Configuration config)
  {
  	if(config != null)
  	{
  		config.registerTableOverride("current_relation_members", "current_relation_members_" + mapId);
  		config.registerTableOverride("current_relations", "current_relations_" + mapId);
  		config.registerTableOverride("current_way_nodes", "current_way_nodes_" + mapId);
  		config.registerTableOverride("current_ways", "current_ways_" + mapId);
  		config.registerTableOverride("current_nodes", "current_nodes_" + mapId);
  		config.registerTableOverride("changesets", "changesets_" + mapId);
  	}
  }

  public static BasicDataSource getdbcpdatasource ()
  {
  	if(dbcp_datasource == null)
		{
			appContext = new ClassPathXmlApplicationContext(new String[] { "db/spring-database.xml" });
			dbcp_datasource = appContext.getBean("dataSource", org.apache.commons.dbcp.BasicDataSource.class);
		}

  	return dbcp_datasource;
  }
  public static Connection createConnection()
  {
  	try
  	{
  		if(dbcp_datasource == null)
  		{
  			appContext = new ClassPathXmlApplicationContext(new String[] { "db/spring-database.xml" });
  			dbcp_datasource = appContext.getBean("dataSource", org.apache.commons.dbcp.BasicDataSource.class);
  		}

			return dbcp_datasource.getConnection();
  	}
  	catch (Exception e)
  	{
  		log.error(e.getMessage());
  	}
  	return null;
  }

  public static boolean closeConnection(Connection conn) throws Exception
  {
  	try
  	{
	  	if(conn != null)
	  	{

	  		conn.close();
	  		return true;
	  	}
  	}
  	catch (Exception e)
  	{
  		throw e;
  	}

  	return false;
  }




  public static void clearTable(com.mysema.query.sql.RelationalPathBase<?> t, Connection conn) throws Exception
  {

  	try
  	{
			Configuration configuration = getConfiguration();

			new SQLDeleteClause(conn, configuration, t)
	    	.execute() ;
  	}
  	catch(Exception e)
  	{
  		log.error(e.getCause().getMessage());
  	}

  }

  /**
   * Determines whether records exist in the services database
   *
   * @param conn JDBC Connection
   * @return true if any records exist in the services database; false otherwise
   * @throws Exception
   */
  public static boolean recordsExistInServicesDb(Connection conn) throws Exception
  {
    long recordCount = 0;

    QChangesets changesets = QChangesets.changesets;
    QMaps maps = QMaps.maps;
    QUsers user = QUsers.users;

  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

  	recordCount += query.from(changesets).count();
  	recordCount += query.from(maps).count();
  	recordCount += query.from(user).count();

    return elementDataExistsInServicesDb(conn) || recordCount > 0;
  }

  /**
   * Determines whether any OSM element records exist in the services database
   *
   * @param conn JDBC Connection
   * @return true if any OSM element records exist in the services database; false otherwise
   * @throws Exception
   */
  public static boolean elementDataExistsInServicesDb(Connection conn) throws Exception
  {
    long recordCount = 0;

    QCurrentNodes currentNodes = QCurrentNodes.currentNodes;
    QCurrentWayNodes currentWayNodes = QCurrentWayNodes.currentWayNodes;
    QCurrentWays currentWays = QCurrentWays.currentWays;
    QCurrentRelationMembers currentRelationMembers = QCurrentRelationMembers.currentRelationMembers;
    QCurrentRelations currentRelations = QCurrentRelations.currentRelations;

    SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

  	recordCount += query.from(currentNodes).count();
  	recordCount += query.from(currentWayNodes).count();
  	recordCount += query.from(currentWays).count();
  	recordCount += query.from(currentRelationMembers).count();
  	recordCount += query.from(currentRelations).count();

    return recordCount > 0;
  }


  /**
   * Gets the map id list from map name
   *
   * @param conn
   * @param mapName
   * @return List of map ids
   * @throws Exception
   */
  public static List<Long> getMapIdsByName(Connection conn, String  mapName) throws Exception
  {
  	QMaps maps = QMaps.maps;
    SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

    final List<Long> mapIds = query.from(maps).where(maps.displayName.eq(mapName)).list(maps.id);

    return mapIds;
  }

  /**
   * Get current_nodes record count by map name
   *
   * @param conn
   * @param mapName
   * @return count of nodes record
   * @throws Exception
   */
  public static long getNodesCountByName(Connection conn, String  mapName) throws Exception
  {
  	long recordCount = 0;
    try
    {
    	List<Long> mapIds = getMapIdsByName(conn,  mapName);

    	for(int i=0; i<mapIds.size(); i++)
    	{
    		Long mapId = mapIds.get(i);
    	QCurrentNodes currentNodes = QCurrentNodes.currentNodes;
	    	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration(mapId.toString()));

	    	recordCount += query.from(currentNodes).count();
    	}
    }
    catch (Exception e)
    {
      String msg = "Error countin Nodes record.  ";

      throw new Exception(msg);
    }
    return recordCount;
  }


  /**
   * Get current_ways record count by map name
   *
   * @param conn
   * @param mapName
   * @return current_ways record count
   * @throws Exception
   */
  public static long getWayCountByName(Connection conn, String  mapName) throws Exception
  {
  	long recordCount = 0;
    try
    {
    	List<Long> mapIds = getMapIdsByName(conn,  mapName);

    	for(int i=0; i<mapIds.size(); i++)
    	{
    		Long mapId = mapIds.get(i);
    	QCurrentWays currentWays = QCurrentWays.currentWays;
	    	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration(mapId.toString()));

	    	recordCount += query.from(currentWays).count();
    	}


    }
    catch (Exception e)
    {
      String msg = "Error countin Way record.  ";

      throw new Exception(msg);
    }
    return recordCount;
  }


  /**
   * Get current_relations record count by map name
   *
   * @param conn
   * @param mapName
   * @return current_relations record count
   * @throws Exception
   */
  public static long getRelationCountByName(Connection conn, String  mapName) throws Exception
  {
  	long recordCount = 0;
    try
    {
    	List<Long> mapIds = getMapIdsByName(conn,  mapName);

    	for(int i=0; i<mapIds.size(); i++)
    	{
    		Long mapId = mapIds.get(i);
    	QCurrentRelations currentRelations = QCurrentRelations.currentRelations;
	    	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration(mapId.toString()));

	    	recordCount += query.from(currentRelations).count();
    	}

    }
    catch (Exception e)
    {
      String msg = "Error countin Relations record.  ";

      throw new Exception(msg);
    }
    return recordCount;
  }

  /**
   * Determines whether any review records exist in the services database
   *
   * @param conn JDBC Connection
   * @return
   * @throws Exception
   */
  public static boolean reviewDataExistsInServicesDb(Connection conn) throws Exception
  {
    long recordCount = 0;

    QReviewMap reviewMap = QReviewMap.reviewMap;
    QReviewItems reviewItems = QReviewItems.reviewItems;
    QElementIdMappings elementIdMappings = QElementIdMappings.elementIdMappings;


    SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

  	recordCount += query.from(reviewMap).count();
  	recordCount += query.from(reviewItems).count();
  	recordCount += query.from(elementIdMappings).count();

    return recordCount > 0;
  }

  /**
   * Determines whether any job records exist in the services database
   *
   * @param conn JDBC Connection
   * @return
   * @throws Exception
   */
  public static boolean jobDataExistsInServicesDb(Connection conn) throws Exception
  {
  	QJobStatus jobStatus = QJobStatus.jobStatus;
  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());
    return query.from(jobStatus).count() > 0;
  }

  /**
   * Clears all data in all resource related tables in the database
   *
   * @param conn JDBC Connection
   * @throws Exception
   *           if any records still exist in the table after the attempted
   *           deletion
   */


  public static void clearServicesDb(Connection conn) throws Exception
  {
    try
    {
    	deleteMapRelatedTables();
    	conn.setAutoCommit(false);
			Configuration configuration = getConfiguration();

    	SQLDeleteClause delete = new SQLDeleteClause(conn, configuration, QCurrentWayNodes.currentWayNodes);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QCurrentRelationMembers.currentRelationMembers);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QCurrentNodes.currentNodes);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QCurrentWays.currentWays);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QCurrentRelations.currentRelations);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QChangesets.changesets);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QMaps.maps);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QUsers.users);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QReviewItems.reviewItems);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QElementIdMappings.elementIdMappings);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QReviewMap.reviewMap);
    	delete.execute();
    	delete = new SQLDeleteClause(conn, configuration, QJobStatus.jobStatus);
    	delete.execute();
    	conn.commit();
    }
    catch (Exception e)
    {
    	conn.rollback();
      String msg = "Error clearing services database.  ";
      msg += "  " + e.getCause().getMessage();
      throw new Exception(msg);
    }
    finally
    {
    	conn.setAutoCommit(true);
    }
  }


  public static void deleteMapRelatedTables() throws Exception
  {

  	List<String> tables = new ArrayList<String>();
  	String dbname = HootProperties.getProperty("dbName");
  	DataDefinitionManager ddm = new DataDefinitionManager();
  	List<String> childrenTables = ddm.getTablesList(dbname, "current_way_nodes");
  	tables.addAll(childrenTables);
  	childrenTables = ddm.getTablesList(dbname, "current_relation_members");
  	tables.addAll(childrenTables);
  	childrenTables = ddm.getTablesList(dbname, "current_nodes");
  	tables.addAll(childrenTables);
  	childrenTables = ddm.getTablesList(dbname, "current_ways");
  	tables.addAll(childrenTables);
  	childrenTables = ddm.getTablesList(dbname, "current_relations");
  	tables.addAll(childrenTables);
  	childrenTables = ddm.getTablesList(dbname, "changesets");
  	tables.addAll(childrenTables);

  	ddm.deleteTables(tables, dbname);
  }


  public static void deleteMapRelatedTablesByMapId(final long mapId) throws Exception
  {

  	String dbname = HootProperties.getProperty("dbName");
		DataDefinitionManager ddm = new DataDefinitionManager();
		List<String> tables = new ArrayList<String>();
		tables.add("current_way_nodes_" + mapId);
		tables.add("current_relation_members_" + mapId);
		tables.add("current_nodes_" + mapId);
		tables.add("current_ways_" + mapId);
		tables.add("current_relations_" + mapId);
		tables.add("changesets_" + mapId);

		ddm.deleteTables(tables, dbname);
  }

  // remove this. replace by calling hoot core layer delete native command

  public static void deleteOSMRecord(Connection conn, Long mapId) throws Exception
  {
    try
    {
    	deleteMapRelatedTablesByMapId(mapId);

    	conn.setAutoCommit(false);
			Configuration configuration = getConfiguration();



    	QMaps maps = QMaps.maps;
    	new SQLDeleteClause(conn, configuration, maps)
    	.where(maps.id.eq(mapId))
    	.execute();

    	QReviewItems reviewItems = QReviewItems.reviewItems;
    	new SQLDeleteClause(conn, configuration, reviewItems)
    	.where(reviewItems.mapId.eq(mapId))
    	.execute();

    	QElementIdMappings elementIdMappings = QElementIdMappings.elementIdMappings;
    	new SQLDeleteClause(conn, configuration, elementIdMappings)
    	.where(elementIdMappings.mapId.eq(mapId))
    	.execute();

    	QReviewMap reviewMap = QReviewMap.reviewMap;
    	new SQLDeleteClause(conn, configuration, reviewMap)
    	.where(reviewMap.mapId.eq(mapId))
    	.execute();


    	conn.commit();
    }
    catch (Exception e)
    {
      String msg = "Error deleting OSM record.  ";
      msg += "  " + e.getCause().getMessage();

      throw new Exception(msg);
    }
    finally
    {
    	conn.setAutoCommit(true);
    }
  }


//remove this. replace by calling hoot core layer delete native command
  public static void deleteOSMRecordByName(Connection conn, String  mapName) throws Exception
  {
    try
    {
			Configuration configuration = getConfiguration();

			QMaps maps = QMaps.maps;
    	List<Long> mapIds = new SQLQuery(conn, configuration).from(maps)
			.where(maps.displayName.equalsIgnoreCase(mapName)).list(maps.id);

    	if(mapIds.size() > 0)
    	{
	    	Long mapId = mapIds.get(0);
				deleteMapRelatedTablesByMapId(mapId);

	    	conn.setAutoCommit(false);

				ListSubQuery<Long> res = new SQLSubQuery().from(maps)
				.where(maps.displayName.equalsIgnoreCase(mapName)).list(maps.id);


    	new SQLDeleteClause(conn, configuration, maps)
    	.where(maps.displayName.eq(mapName))
    	.execute();

    	QReviewItems reviewItems = QReviewItems.reviewItems;
    	new SQLDeleteClause(conn, configuration, reviewItems)
    	.where(reviewItems.mapId.in(res))
    	.execute();

    	QElementIdMappings elementIdMappings = QElementIdMappings.elementIdMappings;
    	new SQLDeleteClause(conn, configuration, elementIdMappings)
    	.where(elementIdMappings.mapId.in(res))
    	.execute();

    	QReviewMap reviewMap = QReviewMap.reviewMap;
    	new SQLDeleteClause(conn, configuration, reviewMap)
    	.where(reviewMap.mapId.in(res))
    	.execute();

    	conn.commit();
    }
    }
    catch (Exception e)
    {
      String msg = "Error deleting OSM record.  ";
      if (e.getCause() instanceof BatchUpdateException)
      {
        BatchUpdateException batchException = (BatchUpdateException) e.getCause();
        msg += "  " + batchException.getNextException().getMessage();
      }
      throw new Exception(msg);
    }
  }

  /**
   * Determines whether any changeset data exists in the services database
   *
   * @param conn JDBC Connection

   * @return true if changeset data exists; false otherwise
   * @throws Exception
   */
  public static boolean changesetDataExistsInServicesDb(Connection conn) throws Exception
  {
    int recordCtr = 0;
    QChangesets changesets = QChangesets.changesets;
    SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

    recordCtr += query.from(changesets).count();

    return recordCtr > 0;
  }

  // TODO: remove
  public static long getTestUserId(Connection conn)
  {
  	QUsers users = QUsers.users;
  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());

    // there is only ever one test user
    return query.from(users).singleResult(users.id);
  }


  public static long insertUser(Connection conn) throws Exception
  {
  	Long newId = (long) -1;
  	NumberExpression<Long> expression = NumberTemplate.create(Long.class, "nextval('users_id_seq')");
   	Configuration configuration = getConfiguration();

  	SQLQuery query = new SQLQuery(conn, configuration);

		List<Long> ids = query.from()
				.list(expression);

		if(ids != null && ids.size() > 0)
		{
			newId = ids.get(0);
			QUsers users = QUsers.users;

			new SQLInsertClause(conn, configuration, users)
	    .columns(users.id, users.displayName, users.email)
	    .values(newId, "user-with-id-" + newId, "user-with-id-" + newId).execute();

		}
    return newId.longValue();
  }


  public static void createMap(final long mapId) throws Exception
  {


  	try {
			String dbname = HootProperties.getProperty("dbName");

			DataDefinitionManager ddm = new DataDefinitionManager();

			// cnagesets
	  	String createTblSql = "CREATE TABLE changesets_" + mapId +
	    "(id bigserial NOT NULL, " +
	    " user_id bigint NOT NULL, " +
	    " created_at timestamp without time zone NOT NULL, " +
	    " min_lat integer NOT NULL, " +
	    " max_lat integer NOT NULL, " +
	    " min_lon integer NOT NULL, " +
	    " max_lon integer NOT NULL, " +
	    " closed_at timestamp without time zone NOT NULL, " +
	    " num_changes integer NOT NULL DEFAULT 0, " +
	    " tags hstore, " +
	    " CONSTRAINT pk_changesets_" + mapId + " PRIMARY KEY (id ), " +
	    " CONSTRAINT changesets_" + mapId + "_user_id_fkey FOREIGN KEY (user_id) " +
	    " REFERENCES users (id) MATCH SIMPLE " +
	    " ON UPDATE NO ACTION ON DELETE NO ACTION " +
	    " ) WITH ( OIDS=FALSE );";

	  	ddm.createTable(createTblSql, dbname);

	  	// current_nodes
	  	createTblSql = "CREATE TABLE current_nodes_" + mapId +
	    "(id bigserial NOT NULL, " +
	    " latitude integer NOT NULL, " +
	    " longitude integer NOT NULL, " +
	    " changeset_id bigint NOT NULL, " +
	    " visible boolean NOT NULL DEFAULT true, " +
	    " \"timestamp\" timestamp without time zone NOT NULL DEFAULT now(), " +
	    " tile bigint NOT NULL, " +
	    " version bigint NOT NULL DEFAULT 1, " +
	    " tags hstore, " +
	    " CONSTRAINT current_nodes_" + mapId + "_pkey" + " PRIMARY KEY (id ), " +
	    " CONSTRAINT current_nodes_" + mapId + "_changeset_id_fkey FOREIGN KEY (changeset_id) " +
	    " REFERENCES changesets_" + mapId +" (id) MATCH SIMPLE " +
	    " ON UPDATE NO ACTION ON DELETE NO ACTION " +
	    " ) WITH ( OIDS=FALSE );";

	  	ddm.createTable(createTblSql, dbname);

	  	//current_relation_members
	  	createTblSql = "CREATE TABLE current_relation_members_" + mapId +
	    "(relation_id bigint NOT NULL, " +
	    " member_type nwr_enum NOT NULL, " +
	    " member_id bigint NOT NULL, " +
	    " member_role character varying(255) NOT NULL, " +
	    " sequence_id integer NOT NULL DEFAULT 0, " +
	    " CONSTRAINT current_relation_members_"+ mapId +"_pkey PRIMARY KEY (relation_id , member_type , member_id , member_role , sequence_id ) " +
	    " ) WITH ( OIDS=FALSE );";

	  	ddm.createTable(createTblSql, dbname);


	  	//current_relations
	  	createTblSql = "CREATE TABLE current_relations_" + mapId +
	  			"(" +
	  			"  id bigserial NOT NULL," +
	  			"  changeset_id bigint NOT NULL," +
	  			"  \"timestamp\" timestamp without time zone NOT NULL DEFAULT now()," +
	  			"  visible boolean NOT NULL DEFAULT true," +
	  			"  version bigint NOT NULL DEFAULT 1," +
	  			"  tags hstore," +
	  			"  CONSTRAINT current_relations_" + mapId + "_pkey PRIMARY KEY (id )," +
	  			"  CONSTRAINT current_relations_" + mapId + "_changeset_id_fkey FOREIGN KEY (changeset_id)" +
	  			"      REFERENCES changesets_" + mapId +" (id) MATCH SIMPLE" +
	  			"      ON UPDATE NO ACTION ON DELETE NO ACTION" +
	  			")" +
	  			"WITH (" +
	  			"  OIDS=FALSE" +
	  			");";
	  	ddm.createTable(createTblSql, dbname);


	  	//current_way_nodes

	  	createTblSql = "CREATE TABLE current_way_nodes_" + mapId +
	  			"(" +
	  			"  way_id bigint NOT NULL," +
	  			"  node_id bigint NOT NULL," +
	  			"  sequence_id bigint NOT NULL," +
	  			"  CONSTRAINT current_way_" + mapId + "_nodes_pkey PRIMARY KEY (way_id , sequence_id )" +
	  			")" +
	  			"WITH (" +
	  			"  OIDS=FALSE" +
	  			");";
	  	ddm.createTable(createTblSql, dbname);


	  	//current_ways
	  	createTblSql = "CREATE TABLE current_ways_" + mapId +
	  			"(" +
	  			"  id bigserial NOT NULL," +
	  			"  changeset_id bigint NOT NULL," +
	  			"  \"timestamp\" timestamp without time zone NOT NULL DEFAULT now()," +
	  			"  visible boolean NOT NULL DEFAULT true," +
	  			"  version bigint NOT NULL DEFAULT 1," +
	  			"  tags hstore," +
	  			"  CONSTRAINT current_ways_" + mapId + "_pkey PRIMARY KEY (id )," +
	  			"  CONSTRAINT current_ways_" + mapId + "_changeset_id_fkey FOREIGN KEY (changeset_id)" +
	  			"      REFERENCES changesets_" + mapId + " (id) MATCH SIMPLE" +
	  			"      ON UPDATE NO ACTION ON DELETE NO ACTION" +
	  			")" +
	  			"WITH (" +
	  			"  OIDS=FALSE" +
	  			");";

	  	ddm.createTable(createTblSql, dbname);

		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}


  }

  public static long insertMap(final long userId, Connection conn) throws Exception
  {
  	Long newId = (long) -1;
  	NumberExpression<Long> expression = NumberTemplate.create(Long.class, "nextval('maps_id_seq')");
   	Configuration configuration = getConfiguration();

  	SQLQuery query = new SQLQuery(conn, configuration);

		List<Long> ids = query.from()
				.list(expression);

		if(ids != null && ids.size() > 0)
		{
			newId = ids.get(0);
			QMaps maps = QMaps.maps;

			final Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());

			new SQLInsertClause(conn, configuration, maps)
	    .columns(maps.id, maps.createdAt, maps.displayName, maps.publicCol, maps.userId)
	    .values(newId, now, "map-with-id-" + newId, true, userId).execute();

		}
		DbUtils.createMap(newId.longValue());
    return newId.longValue();
  }


  public static void insertJobStatus(final String jobId, final int status, Connection conn)
      throws Exception
    {

	  	Configuration configuration = getConfiguration();
			QJobStatus jobStatus = QJobStatus.jobStatus;

			final Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());

			new SQLInsertClause(conn, configuration, jobStatus)
	    .columns(jobStatus.jobId, jobStatus.status, jobStatus.start)
	    .values(jobId,  status, now).execute();


    }

  /**
   * Updates job status. If the record does not exist then creates.
   *
   * @param jobId
   * @param jobStatus
   * @param isComplete
   * @param conn
   * @throws Exception
   */
  public static void updateJobStatus(final String jobId, final int jobStatus, boolean isComplete,
    final String statusDetail, Connection conn) throws Exception
  {
  	Configuration configuration = DbUtils.getConfiguration();

  	QJobStatus jobStatusTbl = QJobStatus.jobStatus;
  	SQLQuery query = new SQLQuery(conn, configuration);
  	JobStatus stat = query.from(jobStatusTbl).where(jobStatusTbl.jobId.eq(jobId)).singleResult(jobStatusTbl);
    if (stat != null)
    {
      if (isComplete == true)
      {
        stat.setPercentComplete(100.0);
        stat.setEnd(new Timestamp(Calendar.getInstance().getTimeInMillis()));
      }
      stat.setStatus(jobStatus);
      if (statusDetail != null)
      {
        stat.setStatusDetail(statusDetail);
      }


      new SQLUpdateClause(conn, configuration, jobStatusTbl)
      .populate(stat)
      .where(jobStatusTbl.jobId.eq(stat.getJobId()))
      .execute();

    }
    else
    {
      stat = new JobStatus();
      stat.setJobId(jobId);
      stat.setStatus(jobStatus);
      Timestamp ts = new Timestamp(Calendar.getInstance().getTimeInMillis());
      stat.setStart(ts);
      if (isComplete == true)
      {
        stat.setEnd(ts);
      }
      //statusDao.insert(stat);
      new SQLInsertClause(conn, configuration, jobStatusTbl)
      .populate(stat).execute();

    }

  }

  /**
   * retrieves job status.
   *
   * @param jobId ID of the job
   * @param conn JDBC Connection
   * @return a numeric job status
   * @throws Exception
   */
  public static int getJobStatus(final String jobId, Connection conn) throws Exception
  {
  	QJobStatus jobStatusTbl = QJobStatus.jobStatus;
  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());
  	JobStatus stat = query.from(jobStatusTbl).where(jobStatusTbl.jobId.eq(jobId)).singleResult(jobStatusTbl);

    return stat.getStatus();
  }

  /**
   * Retrieves a job status as an object
   *
   * @param jobId ID of the job
   * @param conn JDBC Connection
   * @return a job status object
   * @throws Exception
   */
  public static JobStatus getJobStatusObj(final String jobId, Connection conn)
    throws Exception
  {
  	QJobStatus jobStatusTbl = QJobStatus.jobStatus;
  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());
  	JobStatus dbJobStatus = query.from(jobStatusTbl).where(jobStatusTbl.jobId.eq(jobId)).singleResult(jobStatusTbl);

    JobStatus jobStatus = new JobStatus();
    jobStatus.setJobId(dbJobStatus.getJobId());
    jobStatus.setEnd(dbJobStatus.getEnd());
    jobStatus.setStart(dbJobStatus.getEnd());
    jobStatus.setPercentComplete(dbJobStatus.getPercentComplete());
    jobStatus.setStatusDetail(dbJobStatus.getStatusDetail());
    jobStatus.setStatus(dbJobStatus.getStatus());
    return jobStatus;
  }



  public static void batchRecords(final long mapId, final List<?> records, com.mysema.query.sql.RelationalPathBase<?> t,
  		List<List<BooleanExpression>> predicateslist,
      final RecordBatchType recordBatchType, Connection conn, int maxRecordBatchSize) throws Exception
    {
      try
      {
        long execResult = -1;
        Configuration configuration = getConfiguration(mapId);
        //conn.setAutoCommit(false);

        switch (recordBatchType)
        {
          case INSERT:
          	SQLInsertClause insert = new SQLInsertClause(conn, configuration, t);
          	long nBatch = 0;
          	for(int i=0; i<records.size(); i++)
          	{
          		Object oRec = records.get(i);
          		insert.populate(oRec).addBatch();
          		nBatch++;


          		if(maxRecordBatchSize > -1 && i > 0)
          		{
	          		if(i % maxRecordBatchSize == 0) {
	          			insert.execute();
		              //conn.commit();
		              insert = new SQLInsertClause(conn, configuration, t);
		              nBatch = 0;
		      			}
          		}

          	}

          	if(nBatch > 0)
          	{
          		execResult = insert.execute();
          	}

            break;

          case UPDATE:

          	SQLUpdateClause update = new SQLUpdateClause(conn, configuration, t);
          	long nBatchUpdate = 0;
          	for(int i=0; i<records.size(); i++)
          	{
          		Object oRec = records.get(i);

          		List<BooleanExpression> predicates = predicateslist.get(i);

        			BooleanExpression[] params;
        			params = new BooleanExpression[predicates.size()];

        			for(int ii=0; ii<predicates.size(); ii++)
        			{
        				params[ii] = predicates.get(ii);
        			}

          		update.populate(oRec).where(params).addBatch();
          		nBatchUpdate++;

          		if(maxRecordBatchSize > -1 && i > 0)
          		{
	          		if(i % maxRecordBatchSize == 0) {
	          			update.execute();
		              //conn.commit();
		              update = new SQLUpdateClause(conn, configuration, t);
		              nBatchUpdate = 0;
		      			}
          		}

          	}

          	if(nBatchUpdate > 0)
          	{
          		execResult = update.execute();
          	}

            break;

          case DELETE:

          	SQLDeleteClause delete = new SQLDeleteClause(conn, configuration, t);
          	long nBatchDel = 0;
          	for(int i=0; i<records.size(); i++)
          	{
          		Object oRec = records.get(i);

          		List<BooleanExpression> predicates = predicateslist.get(i);

        			BooleanExpression[] params;
        			params = new BooleanExpression[predicates.size()];

        			for(int ii=0; ii<predicates.size(); ii++)
        			{
        				params[ii] = predicates.get(ii);
        			}

        			delete.where(params).addBatch();
        			nBatchDel++;
        			if(maxRecordBatchSize > -1 && i > 0)
        			{
	        			if(i % maxRecordBatchSize == 0) {
	          			delete.execute();
		              //conn.commit();
		              delete = new SQLDeleteClause(conn, configuration, t);
		              nBatchDel = 0;
		      			}
        			}

          	}

          	if(nBatchDel > 0)
          	{
          		execResult = delete.execute();
          	}

            break;

          default:
            throw new Exception("");
        }


     /*   if (execResult != records.size())
        {
        	String msg = "The number of batch records updates execute: " + execResult + " does not " +
              "match the number of records sent to be updated: " + records.size();
          log.warn(msg);
          throw new Exception(msg);
        }*/
        //conn.commit();
      }
      catch (Exception e)
      {
      	//conn.rollback();
        String msg = "Error executing batch query.";
        msg += "  " + e.getMessage();
        msg += " Cause:" + e.getCause().toString();
        throw new Exception(msg);
      }
      finally
      {
      	//conn.setAutoCommit(true);
      }
    }




  public static void batchRecordsDirectNodes(final long mapId, final List<?> records,
      final RecordBatchType recordBatchType, Connection conn, int maxRecordBatchSize) throws Exception
    {
  		PreparedStatement ps = null;
      try
      {
      	String sql = null;
        long execResult = -1;
        //conn.setAutoCommit(false);
        int count = 0;

        switch (recordBatchType)
        {
          case INSERT:


      			sql = "insert into current_nodes_" + mapId + " (id, latitude, " +
      					"longitude, changeset_id, visible, \"timestamp\", tile, version, tags) " +
      					"values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

      			ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentNodes node  = (CurrentNodes)o;


	      			ps.setLong(1, node.getId());
	      			ps.setInt(2, node.getLatitude());
	      			ps.setInt(3, node.getLongitude());
	      			ps.setLong(4, node.getChangesetId());
	      			ps.setBoolean(5, node.getVisible());
	      			ps.setTimestamp(6, node.getTimestamp());
	      			ps.setLong(7, node.getTile());
	      			ps.setLong(8, node.getVersion());

	      			Map<String, String> tags = (Map<String, String>)node.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(9, hstoreStr, Types.OTHER);
	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();

		      			}
	      			}

          	}

            break;

          case UPDATE:

          	sql = "update current_nodes_" + mapId +" set  latitude=?, " +
      					"longitude=?, changeset_id=?, visible=?, \"timestamp\"=?, tile=?, version=?, tags=? " +
      					"where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentNodes node  = (CurrentNodes)o;

	      			ps.setInt(1, node.getLatitude());
	      			ps.setInt(2, node.getLongitude());
	      			ps.setLong(3, node.getChangesetId());
	      			ps.setBoolean(4, node.getVisible());
	      			ps.setTimestamp(5, node.getTimestamp());
	      			ps.setLong(6, node.getTile());
	      			ps.setLong(7, node.getVersion());

	      			Map<String, String> tags = (Map<String, String>)node.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(8, hstoreStr, Types.OTHER);

	      			ps.setLong(9, node.getId());

	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		              ps.clearBatch();
		      			}
	      			}
          	}


            break;

          case DELETE:

          	sql = "delete from current_nodes_" + mapId +
      					" where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentNodes node  = (CurrentNodes)o;

	      			ps.setLong(1, node.getId());

	      			ps.addBatch();
	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		              ps.clearBatch();
		      			}
	      			}
          	}


            break;

          default:
            throw new Exception("");
        }

        ps.executeBatch();
        //conn.commit();
      }
      catch (Exception e)
      {
      	//conn.rollback();
        String msg = "Error executing batch query.";
        msg += "  " + e.getMessage();
        msg += " Cause:" + e.getCause().toString();
        throw new Exception(msg);
      }
      finally
      {
      	if(ps != null)
      	{
      		ps.close();
      	}
      	//conn.setAutoCommit(true);
      }
    }


  public static void batchRecordsDirectWays(final long mapId, final List<?> records,
      final RecordBatchType recordBatchType, Connection conn, int maxRecordBatchSize) throws Exception
    {
  		PreparedStatement ps = null;
      try
      {
      	String sql = null;
        long execResult = -1;
        //conn.setAutoCommit(false);
        int count = 0;

        switch (recordBatchType)
        {
          case INSERT:


      			sql = "insert into current_ways_" + mapId + " (id, changeset_id, \"timestamp\", visible, version, tags) " +
      					"values (?, ?, ?, ?, ?, ?)";

      			ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentWays way  = (CurrentWays)o;


	      			ps.setLong(1, way.getId());
	      			ps.setLong(2, way.getChangesetId());
	      			ps.setTimestamp(3, way.getTimestamp());
	      			ps.setBoolean(4, way.getVisible());
	      			ps.setLong(5, way.getVersion());

	      			Map<String, String> tags = (Map<String, String>)way.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(6, hstoreStr, Types.OTHER);
	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}

          	}

            break;

          case UPDATE:

          	sql = "update current_ways_" + mapId + " set changeset_id=?, visible=?, \"timestamp\"=?, version=?, tags=? " +
      					"where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentWays way  = (CurrentWays)o;

	      			ps.setLong(1, way.getChangesetId());
	      			ps.setBoolean(2, way.getVisible());
	      			ps.setTimestamp(3, way.getTimestamp());
	      			ps.setLong(4, way.getVersion());

	      			Map<String, String> tags = (Map<String, String>)way.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(5, hstoreStr, Types.OTHER);

	      			ps.setLong(6, way.getId());

	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}
          	}


            break;

          case DELETE:

          	sql = "delete from current_ways_" + mapId +
      					" where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentWays way  = (CurrentWays)o;

	      			ps.setLong(1, way.getId());

	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}

          	}


            break;

          default:
            throw new Exception("");
        }

        ps.executeBatch();
        //conn.commit();
      }
      catch (Exception e)
      {
      	conn.rollback();
        String msg = "Error executing batch query.";
        msg += "  " + e.getMessage();
        msg += " Cause:" + e.getCause().toString();
        throw new Exception(msg);
      }
      finally
      {
      	if(ps != null)
      	{
      		ps.close();
      	}
      	//conn.setAutoCommit(true);
      }
    }



  public static void batchRecordsDirectRelations(final long mapId, final List<?> records,
      final RecordBatchType recordBatchType, Connection conn, int maxRecordBatchSize) throws Exception
    {
  		PreparedStatement ps = null;
      try
      {
      	String sql = null;
        long execResult = -1;
        //conn.setAutoCommit(false);
        int count = 0;

        switch (recordBatchType)
        {
          case INSERT:


      			sql = "insert into current_relations_" + mapId + " (id, changeset_id, \"timestamp\", visible, version, tags) " +
      					"values (?, ?, ?, ?, ?, ?)";

      			ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentRelations rel  = (CurrentRelations)o;


	      			ps.setLong(1, rel.getId());
	      			ps.setLong(2, rel.getChangesetId());
	      			ps.setTimestamp(3, rel.getTimestamp());
	      			ps.setBoolean(4, rel.getVisible());
	      			ps.setLong(5, rel.getVersion());

	      			Map<String, String> tags = (Map<String, String>)rel.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(6, hstoreStr, Types.OTHER);
	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}

          	}

            break;

          case UPDATE:

          	sql = "update current_relations_" + mapId + " set changeset_id=?, visible=?, \"timestamp\"=?, version=?, tags=? " +
      					"where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentRelations rel  = (CurrentRelations)o;

	      			ps.setLong(1, rel.getChangesetId());
	      			ps.setBoolean(2, rel.getVisible());
	      			ps.setTimestamp(3, rel.getTimestamp());
	      			ps.setLong(4, rel.getVersion());

	      			Map<String, String> tags = (Map<String, String>)rel.getTags();

	      			String hstoreStr = "";
	      			Iterator it = tags.entrySet().iterator();
	      	    while (it.hasNext()) {
	      	        Map.Entry pairs = (Map.Entry)it.next();
	      	        if(hstoreStr.length() > 0)
	      	        {
	      	        	hstoreStr += ",";
	      	        }
	      	        hstoreStr += "\"" + pairs.getKey() + "\"=>\"" + pairs.getValue() + "\"";
	      	    }
	      			ps.setObject(5, hstoreStr, Types.OTHER);

	      			ps.setLong(6, rel.getId());

	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}
          	}


            break;

          case DELETE:

          	sql = "delete from current_relations_" + mapId +
      					" where id=?";
          	ps = conn.prepareStatement(sql);
          	for( Object o : records)
          	{
          		CurrentRelations rel  = (CurrentRelations)o;

	      			ps.setLong(1, rel.getId());

	      			ps.addBatch();

	      			if(maxRecordBatchSize > -1)
	      			{
		      			if(++count % maxRecordBatchSize == 0) {
		              ps.executeBatch();
		              //conn.commit();
		      			}
	      			}

          	}


            break;

          default:
            throw new Exception("");
        }

        ps.executeBatch();
        //conn.commit();
      }
      catch (Exception e)
      {
      	conn.rollback();
        String msg = "Error executing batch query.";
        msg += "  " + e.getMessage();
        msg += " Cause:" + e.getCause().toString();
        throw new Exception(msg);
      }
      finally
      {
      	if(ps != null)
      	{
      		ps.close();
      	}
      	//conn.setAutoCommit(true);
      }
    }

  
  // Returns table size in byte
	public static long getTableSizeInByte(final String tableName) throws Exception
	{
		long ret = 0;
		Connection conn = null;
	  Statement stmt = null;
		try
		{

			conn = DbUtils.createConnection();
			stmt = conn.createStatement();
			
			String sql = "select pg_total_relation_size('" + tableName + "') as tablesize";
			ResultSet rs = stmt.executeQuery(sql);
      //STEP 5: Extract data from result set
      while(rs.next()){
         //Retrieve by column name
      	ret = rs.getLong("tablesize");
         
      }
      rs.close();
		}
		catch (Exception e)
		{
			log.error(e.getMessage());
			throw e;
		}
		finally{
      //finally block used to close resources
      try{
         if(stmt!=null)
            stmt.close();
      }catch(SQLException se2){
      	log.equals(se2.getMessage());
      }// nothing we can do
      try{
         if(conn!=null)
            DbUtils.closeConnection(conn);
      }catch(SQLException se){
      	log.equals(se.getMessage());
      }//end finally try
		}//end try
		
		return ret;
	}

  
  /**
   * Converts a geo-coordinate value to the database storage format
   *
   * @param coordVal
   *          coordinate value to convert
   * @return a converted coordinate value
   */
  public static int toDbCoordValue(double coordVal)
  {
    return (int)(toDbCoordPrecision(coordVal) * GeoUtils.GEO_RECORD_SCALE);
  }

  /**
   * Converts a geo-coordinate value from the database storage format
   *
   * @param coordVal
   *          coordinate value to convert
   * @return a converted coordinate value
   */
  public static double fromDbCoordValue(int coordVal)
  {
    return coordVal / (double) GeoUtils.GEO_RECORD_SCALE;
  }

  /**
   * Sets a geo-coordinate value to the decimal precision expected by the
   * services database
   *
   * @param coordVal
   *          a coordinate value
   * @return input coordinate value with the correct number of decimal places
   */
  public static double toDbCoordPrecision(double coordVal)
  {
    return MathUtils.round(coordVal, 7);
  }


}
