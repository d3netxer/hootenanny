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
package hoot.services.models.osm;


import hoot.services.db.DbUtils;
import hoot.services.db2.QChangesets;
import hoot.services.db2.QMaps;
import hoot.services.db2.Users;

import java.sql.Connection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mysema.query.sql.SQLQuery;

/**
 * Represents the model for an OSM user
 */
public class User extends Users
{
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 4395123526768281005L;
  protected static final QChangesets changesets = QChangesets.changesets;
  protected static final QMaps maps = QMaps.maps;

  private Connection conn;

  public User(final Users user, Connection conn)
  {
    super();

    setDisplayName(user.getDisplayName());
    setEmail(user.getEmail());
    setId(user.getId());

    this.conn = conn;
  }

  /**
   * Returns an XML representation of the user's details
   *
   * @param parentXml XML node this node should be attached under
   * @return an XML node
   * @see http://wiki.openstreetmap.org/wiki/API_0.6
   */
  public Element detailsToXml(final Element parentXml, final long changesetsCount)
  {
    Document doc = parentXml.getOwnerDocument();

    Element userElement = doc.createElement("user");
    userElement.setAttribute("id", String.valueOf(getId()));
    userElement.setAttribute("display_name", getDisplayName());
    //account_created attribute not supported yet

    //img element not supported
    //roles element not supported
    Element changesetsElement = doc.createElement("changesets");
    changesetsElement.setAttribute("count", String.valueOf(changesetsCount));
    //traces element not supported
    //blocks element not supported
    //home element not supported
    //description element not supported
    //languages element not supported
    //messages element not supported

    userElement.appendChild(changesetsElement);

    return userElement;
  }

  /**
   * Returns an XML representation of the user
   *
   * @param parentXml XML node this node should be attached under
   * @return an XML node
   * @see http://wiki.openstreetmap.org/wiki/API_0.6
   */
  public Element toXml(final Element parentXml, final long changesetsCount)
  {
    return detailsToXml(parentXml, changesetsCount);
  }

  /**
   * Returns the number of changesets modified by this user
   *
   * @return number of changesets
   */
  public long numChangesetsModified()
  {

  	SQLQuery query = new SQLQuery(conn, DbUtils.getConfiguration());
  	Long mapid = query.from(maps).where(maps.userId.eq(getId())).singleResult(maps.id);

  	query = new SQLQuery(conn, DbUtils.getConfiguration(mapid));
  	return
  	    query.from(changesets)
  			.where( changesets.userId.eq(getId()))
  			.count();
  }
}
