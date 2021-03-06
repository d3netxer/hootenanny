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
 * @copyright Copyright (C) 2015 DigitalGlobe (http://www.digitalglobe.com/)
 */
package hoot.services.controllers.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hoot.services.HootProperties;
import hoot.services.nodeJs.ServerControllerBase;
import hoot.services.utils.ResourceErrorHandler;

public class P2PResource extends ServerControllerBase {
	private static final Logger log = LoggerFactory.getLogger(P2PResource.class);
	
	private static String homeFolder = null;
  private static String P2PServerPort = null;
  private static String P2PServerThreadCount = null;
  private static String P2PServerScript = null;
  
  private static final Object procLock = new Object();
  private static final Object portLock = new Object();
  
  private static String currentPort = null;
  private static Process _P2PProc = null;
  
  public P2PResource() 
  {
  	try
  	{
  		homeFolder = HootProperties.getProperty("homeFolder");
  		P2PServerPort = HootProperties.getProperty("P2PServerPort");
  		P2PServerThreadCount = HootProperties.getProperty("P2PServerThreadCount");
  		P2PServerScript = HootProperties.getProperty("P2PServerScript");
  	}
  	catch (Exception e)
  	{
  		log.error(e.getMessage());
  	}
  }
  
  /**
	 * <NAME>POI to POI Service Node Server Start</NAME>
	 * <DESCRIPTION>
	 * This rest end point start P2P node js server. It first searches for process with "P2PServer.js"
	 * and kills them to clean out previously running processes. It also combines stdout and stderr from node server output
	 * and then prints out to service stdout. 
	 * </DESCRIPTION>
	 * <PARAMETERS>
	 * <port>
	 * 	The port node server should be listening. If none supplied then it will use default port specified in hoot-services.conf
	 * 	P2PServerPort variable.
	 * </port>
	 * <threadcount>
	 * 	The number of processes used by P2P server. If 0 then it will use numbers of CPU. If none specified then the default
	 * 	value in hoot-services.conf P2PServerThreadCount gets used.
	 * </threadcount>
	 * </PARAMETERS>
	 * <OUTPUT>
	 * 	JSON containing port and threadcount
	 * </OUTPUT>
	 * <EXAMPLE>
	 * 	<URL>http://localhost:8080/hoot-services/services/p2pserver/start?port=8096&threadcount=0</URL>
	 * 	<REQUEST_TYPE>GET</REQUEST_TYPE>
	 * 	<INPUT>
	 *	</INPUT>
	 * <OUTPUT>{"port":"8096", "threadcount","0"}</OUTPUT>
	 * </EXAMPLE>
   * @param port
   * @param nThread
   * @return
   */
  @GET
  @Path("/p2pserver/start")
  @Produces(MediaType.TEXT_PLAIN)
  public Response startP2PService(@QueryParam("port") final String port, 
  		@QueryParam("threadcount") final String nThread) {

  	// set default default port and threadcount
  	String currPort = P2PServerPort;
  	String currThreadCnt = P2PServerThreadCount;
		try
		{
			// Make sure to wipe out previosuly running servers.
			stopServer(homeFolder + "/scripts/" + P2PServerScript);
			
			// override with user specified values if available
			if(port != null)
			{
				try
				{
					Integer.parseInt(port);
					currPort = port;
				}
				catch (Exception pe)
				{
					log.warn("Supplied port number is invalid. Using " + currPort);
				}
				
			}
			
		// override with user specified values if available
			if(nThread != null)
			{
				try
				{
					Integer.parseInt(nThread);
					currThreadCnt = nThread;
				}
				catch (Exception pe)
				{
					log.warn("Supplied thread count is invalid. Using " + currThreadCnt);
				}
				
			}
			
			// Probably an overkill but just in-case using synch lock
			synchronized(portLock)
			{
				currentPort = currPort;
			}

			synchronized(procLock)
			{
				_P2PProc = startServer(currPort, currThreadCnt, homeFolder + "/scripts/" + P2PServerScript);
			}

		}
		catch (Exception ex)
		{
		  ResourceErrorHandler.handleError(
			"Error starting P2P service request: " + ex.toString(),
		    Status.INTERNAL_SERVER_ERROR,
			log);
		}
  	
  	JSONObject res = new JSONObject();
		res.put("port", currPort);
		res.put("threadcount", currThreadCnt);
		return Response.ok(res.toJSONString(), MediaType.APPLICATION_JSON).build();
  }

 
  
  
  
  /**
	 * <NAME>POI to POI Service Node Server Stop</NAME>
	 * <DESCRIPTION>
	 *  Destroys all POI to POI server process where it effectively shutting them down.
	 * </DESCRIPTION>
	 * <PARAMETERS>
	 * </PARAMETERS>
	 * <OUTPUT>
	 * 	JSON containing state
	 * </OUTPUT>
	 * <EXAMPLE>
	 * 	<URL>http://localhost:8080/hoot-services/services/p2pserver/stop</URL>
	 * 	<REQUEST_TYPE>GET</REQUEST_TYPE>
	 * 	<INPUT>
	 *	</INPUT>
	 * <OUTPUT>{"isRunning":"false"}</OUTPUT>
	 * </EXAMPLE>
   * @return
   */
  @GET
  @Path("/p2pserver/stop")
  @Produces(MediaType.TEXT_PLAIN)
  public Response stopP2PService() {
  	// This also gets called automatically from HootServletContext when service exits but
  	// should not be reliable since there are many path where it will not be invoked.
		try
		{  
			stopServer(homeFolder + "/scripts/" + P2PServerScript);
		}
		catch (Exception ex)
		{
		  ResourceErrorHandler.handleError(
			"Error starting P2P service request: " + ex.toString(),
		    Status.INTERNAL_SERVER_ERROR,
			log);
		}
  	
  	JSONObject res = new JSONObject();
		res.put("isRunning", "false");
		return Response.ok(res.toJSONString(), MediaType.APPLICATION_JSON).build();
  }
  
  
  
  /**
	 * <NAME>POI to POI Service Node Server status</NAME>
	 * <DESCRIPTION>
	 *  Gets current status of P2P server.
	 * </DESCRIPTION>
	 * <PARAMETERS>
	 * </PARAMETERS>
	 * <OUTPUT>
	 * 	JSON containing state and port it is running
	 * </OUTPUT>
	 * <EXAMPLE>
	 * 	<URL>http://localhost:8080/hoot-services/services/p2pserver/status</URL>
	 * 	<REQUEST_TYPE>GET</REQUEST_TYPE>
	 * 	<INPUT>
	 *	</INPUT>
	 * <OUTPUT>{"isRunning":"true","port":"8096"}</OUTPUT>
	 * </EXAMPLE>
   * @return
   */  
  @GET
  @Path("/p2pserver/status")
  @Produces(MediaType.TEXT_PLAIN)
  public Response isP2PServiceRunning() {
  	boolean isRunning = false;
  	
  	
		try
		{
			isRunning = getStatus(_P2PProc);
		}
		catch (Exception ex)
		{
		  ResourceErrorHandler.handleError(
			"Error starting P2P service request: " + ex.toString(),
		    Status.INTERNAL_SERVER_ERROR,
			log);
		}
  	
  	JSONObject res = new JSONObject();
		res.put("isRunning", isRunning);
		res.put("port", currentPort);
		return Response.ok(res.toJSONString(), MediaType.APPLICATION_JSON).build();
  }



}
