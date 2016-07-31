package com.interop.ipawsui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.interop.api.TurtleAPICore;
import com.interop.api.TurtleAPI.TurtleAPIException;

public class IpawsAPI {
    private TurtleAPICore core = null;
    private String serverURL = null;
    private String username = null;
    private String password = null;
    
	public IpawsAPI(String url, String username, String password) {
		this.serverURL = url;
		this.username = username;
		this.password = password;
		
		core = new IpawsAPIPC(this.serverURL, this.username, this.password, null);
		
	}
	
	public boolean login(String ipawsUser, String ipawsPassword) throws TurtleAPIException {
		String uri = null;
		uri = "userAction=login&user=" + ipawsUser + "&ipaws_password=" + ipawsPassword;
        JsonNode node = callAPI(uri);
        JsonNode valueNode = node.get("status");
       
        return valueNode != null && valueNode.textValue().equals("OK");
	}
	
	private JsonNode callAPI(String parm) throws TurtleAPIException {

		return core.callAPI(parm);
    	
    }
}
