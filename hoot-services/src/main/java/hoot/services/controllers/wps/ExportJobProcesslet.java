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
 * @copyright Copyright (C) 2014 DigitalGlobe (http://www.digitalglobe.com/)
 */
package hoot.services.controllers.wps;

import hoot.services.HootProperties;

import org.deegree.services.wps.ProcessletException;
import org.deegree.services.wps.ProcessletExecutionInfo;
import org.deegree.services.wps.ProcessletInputs;
import org.deegree.services.wps.ProcessletOutputs;
import org.deegree.services.wps.output.LiteralOutput;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExportJobProcesslet extends JobProcesslet {
	
  private static final Logger log = LoggerFactory.getLogger(ExportJobProcesslet.class);
  private String makefileName = null;
  private String wfsStoreDb = null;
	/**
	 * Constructor.
	 * Configures the makefile name through hoot-services.conf so it can modified
	 * externally.
	 */
	public ExportJobProcesslet() throws Exception {
		try {		
			
			this.jobIdStr = "ex_" + this.jobIdStr.replace("-", "");
			wfsStoreDb = 
	        HootProperties.getInstance().getProperty("wfsStoreDb");
			String tempOutputPath = HootProperties.getProperty("tempOutputPath");
			JSONObject customParam = new JSONObject();
			customParam.put("outputfolder", tempOutputPath + "/" + this.jobIdStr);
			customParam.put("output", this.jobIdStr);
			customParam.put("outputname", this.jobIdStr);
			customParam.put("wfsoutputdb", wfsStoreDb);
			this.setCustomParams(customParam);
	  	makefileName = HootProperties.getProperty("ExportScript");
	  	this.setProcessScriptName(makefileName);
	  	
		} catch (Exception e) {
			log.error(e.getMessage());
		}	
	}
	

	@Override
	public void process(ProcessletInputs in, ProcessletOutputs out, ProcessletExecutionInfo info) 
			throws ProcessletException {

		JSONArray args = parseRequestParams(in);

		try {		
			
			String type = getParameterValue("outputtype", args);
			if(type != null && type.equalsIgnoreCase("wfs"))
			{
				
				JSONObject osm2orgCommand = _createPostBody(args);
				
				JSONArray params = (JSONArray)osm2orgCommand.get("params");
				String outname = getParameterValue("outputname", params);
				
				JSONArray wfsArgs = new JSONArray();
				JSONObject param = new JSONObject();
				param.put("value", outname);
				param.put("paramtype", String.class.getName());
				param.put("isprimitivetype", "false");
				wfsArgs.add(param);
				
				
				JSONObject prepareItemsForReviewCommand = _createReflectionSycJobReq(wfsArgs, "hoot.services.controllers.wfs.WfsManager",
						"createWfsResource");
				
				JSONArray jobArgs = new JSONArray();
				jobArgs.add(osm2orgCommand);
				jobArgs.add(prepareItemsForReviewCommand);
				
				
				postChainJobRquest( jobIdStr,  jobArgs.toJSONString());	
			}
			else
			{
				String commandStr = createPostBody(args);
				postJobRquest(jobIdStr, commandStr);
			}
			
			
			
	
			
			((LiteralOutput)out.getParameter("jobId")).setValue(jobIdStr);
		} catch (Exception e) {
			log.error(e.getMessage());
		}


	}
	
	
		
}
