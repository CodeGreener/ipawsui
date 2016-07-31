package com.interop.ipawsui;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interop.api.TurtleAPI.TurtleAPIException;
import com.interop.api.TurtleAPI;
import com.interop.api.TurtleAPICore;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class IpawsAPIPC  implements TurtleAPICore {
	private Logger logger = Logger.getLogger(IpawsAPIPC.class);
    
	private String _serverURL = null;
    private String _username = null;
    private String _password = null;
    private String _type = null;
    
    private String Servlet_Path = "/operation/mCAPAlerts.do?";
    
	private String _code = null;
	private String _value = null;
	private String _result = null;
	private JsonNode _node = null;
        private Document _document = null;
    private Object _sender = new Object();
    
    private ObjectMapper _mapper = new ObjectMapper();
    private JsonFactory _factory = null;

	private CloseableHttpClient _httpClient = null;
	
	/**
	 * Creates a new instance of TurtleAPI
	 */
	public IpawsAPIPC(String serverURL, String username, String password, String type) {
		_serverURL = serverURL;
		_username = username;
		_password = password;
                _type = type;
		formatURL();
    	_factory = _mapper.getFactory(); 
    	PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	    // Increase max total connection to 200
	    cm.setMaxTotal(200);
	    // Increase default max connection per route to 100
	    cm.setDefaultMaxPerRoute(100);
	
	    RequestConfig requestConfig = RequestConfig.custom()
	            .setConnectTimeout(45000)
	            .setConnectionRequestTimeout(45000)
	            .setSocketTimeout(45000)
	            .build();
	    
	    _httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(requestConfig).build();

	}
	

    public synchronized JsonNode callAPI(final String uri) throws TurtleAPIException {
        _code = null;
        _node = null;
        if (uri == null) {
        	_code = "NU";
        }
        else {
            Thread callThread = new Thread(new Runnable() {
                public void run() {
                    InputStream stream = null;
                    if (_httpClient == null) {
                    	_code = "SC";
                    	return;
                    }
                    synchronized (_sender) {
                    	HttpGet request = null;
                    	CloseableHttpResponse response = null;
                        try {     // Add your data     
                        	request = new HttpGet();
                            request.setURI(new URI(_serverURL + Servlet_Path + uri));
                            response = _httpClient.execute(request);
                            if (response.getStatusLine().getStatusCode() == 200) {
                                if (response.getEntity().getContentLength() != 0) {
                                    stream = response.getEntity().getContent();
                                    long current = 0;
                                    long max = response.getEntity().getContentLength();
                                    StringBuffer buffer = new StringBuffer();
                                    byte[] bytes = new byte[1024];
                                    int bytesRead = 0;
                                    while ((max < 0 && bytesRead >= 0) ||
                                    		(max > 0 && current < max)) {
                                        bytesRead = stream.read(bytes);
                                        if (bytesRead > 0) {
	                                        buffer.append(new String(bytes, 0, bytesRead));
	                                        current = current + bytesRead;
                                        }
                                    }
                                    _result = buffer.toString();
                                    stream.close();
                                    stream = null;
                                    if (!_result.startsWith("{")) {
                                        sendLogin();
                                    }
                                    else {
                                        JsonParser parser = _factory.createParser(_result);
                                        _node = _mapper.readTree(parser);
                                        _code = _node.get("status").textValue();
                                    }
                                }
                                else { //length error
                                	EntityUtils.consume(response.getEntity());
                                    _code = "LE";
                                }
                            }
                            else { //bad status
                            	EntityUtils.consume(response.getEntity());
                            	_code = "SE";
                                _value = Integer.toString(response.getStatusLine().getStatusCode());
                            }
                            
                        } catch (java.net.SocketTimeoutException e) {
                            _code = "TO1";
                            request.abort();
                        } catch (org.apache.http.conn.ConnectTimeoutException e) {
                            _code = "TO2";
                            request.abort();
                        } catch (Exception e) {
                            e.printStackTrace();
                            request.abort();
                            _code = "UE";
                            if (e instanceof HttpHostConnectException) {
                               _value = e.getClass().getName() + ": " + ((HttpHostConnectException)e).getHost() + " " + e.getMessage(); 	
                            }
                            else {
                                _value = e.getClass().getName() + ": " + e.getMessage();
                            }
                        } finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Exception e) {
                                    //ignore
                                } finally {
                                    stream = null;
                                }
                            }
                            if (request != null) {
                                request.releaseConnection();
                            }
                            if (response != null) {
                            	try {
                            		response.close();
                            	} catch (Exception e) {
                            		//ignore
                            	}
                            }
                        }
                    }
                }

                private void sendLogin() {
                    InputStream stream = null;
                	HttpGet request = null;
                	CloseableHttpResponse response = null;
                    try {     // Add your data     
                        request = new HttpGet();
                        request.setURI(new URI(_serverURL + "/j_security_check?j_username=" + URLEncoder.encode( _username, "UTF8") + "&j_password=" + URLEncoder.encode(_password , "UTF8")));
                        response = _httpClient.execute(request);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            if (response.getEntity().getContentLength() > 0) {
                                stream = response.getEntity().getContent();
                                long current = 0;
                                long max = response.getEntity().getContentLength();
                                StringBuffer buffer = new StringBuffer();
                                byte[] bytes = new byte[1024];
                                while (current < max) {
                                    int bytesRead = stream.read(bytes);
                                    buffer.append(new String(bytes, 0, bytesRead));
                                    current = current + bytesRead;
                                }
                                _result = buffer.toString();
                                stream.close();
                                stream = null;
                                if (!_result.startsWith("{")) {
                                    _code = "CE";
                                }
                                else {
                                    JsonParser parser = _factory.createParser(_result);
                                    System.out.println("***API result***" + _result);
                                    _node = _mapper.readTree(parser);
                                    _code = _node.get("status").textValue();
                                }
                            }
                            else { //length error
                            	EntityUtils.consume(response.getEntity());
                                _code = "LE";
                            }
                        }
                        else { //error
                        	EntityUtils.consume(response.getEntity());
                            _code = "SE";
                            _value = Integer.toString(response.getStatusLine().getStatusCode());
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        _code = "TO3";
                        request.abort();
                    } catch (org.apache.http.conn.ConnectTimeoutException e) {
                        _code = "TO4";
                        request.abort();
                    } catch (Exception e) {
                        e.printStackTrace();
                        request.abort();
                        _code = "UE";
                        _value = e.getClass().getName() + ": " + toString();
                    } finally {
                        if (stream != null) {
                            try {
                                    stream.close();
                            } catch (Exception e) {
                                    //ignore
                            } finally {
                                    stream = null;
                            }
                            if (request != null) {
                                request.releaseConnection();
                            }
                            if (response != null) {
                            	try {
                            		response.close();
                            	} catch (Exception e) {
                            		//ignore
                            	}
                            }
                        }
                    }
                }
            });
            callThread.start();
            long time = System.currentTimeMillis();
            while (_code == null && System.currentTimeMillis() - time < 45000) {
                try {
                    Thread.sleep(2);
                } catch (Exception e) {
                }
            }
            if (_code == null) {
                _code = "TO5";
                if (callThread.isAlive()) callThread.interrupt();
            }
        }
        
        checkError(_code);
        return _node;
    }
    
    public Document getDocument(final String uri) throws TurtleAPIException {
            synchronized (_sender) {
                _code = null;
                if (uri == null) {
                    _code = "NU";
                    return null;
                }
                else {
                    Thread callThread = new Thread(new Runnable() {
                        public void run() {
                            InputStream stream = null;
                                HttpGet request = null;
                                CloseableHttpResponse response = null;
                            if (_httpClient == null) {
                                    _code = "SC";
                                    return;
                            }
                        try {     // Add your data     
                            request = new HttpGet();
                            request.setURI(new URI(_serverURL + uri));
                            response = _httpClient.execute(request);
                            if (response.getStatusLine().getStatusCode() == 200) {
                                if (response.getEntity().getContentLength() != 0) {
                                    stream = response.getEntity().getContent();
                                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                                    DocumentBuilder builder = factory.newDocumentBuilder();
                                    _document = builder.parse(stream);
                                    _code = "OK";
                                }
                                else { //length error
                                    EntityUtils.consume(response.getEntity());
                                    _code = "LE";
                                }
                            }
                            else { //bad status
                                EntityUtils.consume(response.getEntity());
                                _code = "SE";
                                _value = Integer.toString(response.getStatusLine().getStatusCode());
                            }
                        } catch (java.net.SocketTimeoutException e) {
                            _code = "TO1";
                        } catch (org.apache.http.conn.ConnectTimeoutException e) {
                            _code = "TO2";
                        } catch (Exception e) {
                            e.printStackTrace();
                            _code = "UE";
                            if (e instanceof HttpHostConnectException) {
                               _value = e.getClass().getName() + ": " + ((HttpHostConnectException)e).getHost() + " " + e.getMessage(); 	
                            }
                            else {
                                _value = e.getClass().getName() + ": " + e.getMessage();
                            }
                        } finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Exception e) {
                                    //ignore
                                } finally {
                                    stream = null;
                                }
                            }
                            if (request != null) {
                                request.releaseConnection();
                            }
                            if (response != null) {
                                try {
                                        response.close();
                                } catch (Exception e) {
                                        //ignore
                                }
                            }
                        }
                        }

                    });
                    callThread.start();
                    long time = System.currentTimeMillis();
                    while (_code == null && System.currentTimeMillis() - time < 45000) {
                        try {
                            Thread.sleep(2);
                        } catch (Exception e) {
                        }
                    }
                    if (_code == null) {
                        _code = "TO5";
                        if (callThread.isAlive()) callThread.interrupt();
                    }
                }

                checkError(_code);
                return _document;
            }
    }

    public void logout() throws TurtleAPIException {
        Thread callThread = new Thread(new Runnable() {
                public void run() {
                    InputStream stream = null;
                	HttpGet request = null;
                	CloseableHttpResponse response = null;
                    //String result = null;
                    try {     // Add your data     
                        request = new HttpGet();
                        request.setURI(new URI(_serverURL + "/logoff.jsp"));
                        response = _httpClient.execute(request);
                        if (response.getStatusLine().getStatusCode() == 200) {
                            if (response.getEntity().getContentLength() > 0) {
                                stream = response.getEntity().getContent();
                                long current = 0;
                                long max = response.getEntity().getContentLength();
                                StringBuffer buffer = new StringBuffer();
                                byte[] bytes = new byte[1024];
                                while (current < max) {
                                    int bytesRead = stream.read(bytes);
                                    buffer.append(new String(bytes, 0, bytesRead));
                                    current = current + bytesRead;
                                }
                                stream.close();
                                stream = null;
                            }
                            else { //length error
                            	EntityUtils.consume(response.getEntity());
                                _code = "LE";
                            }
                        }
                        else { //error
                        	EntityUtils.consume(response.getEntity());
                            _code = "SE";
                            _value = Integer.toString(response.getStatusLine().getStatusCode());
                        }
                    } catch (java.net.SocketTimeoutException e) {
                            _code = "TO6";
                            request.abort();
                    } catch (org.apache.http.conn.ConnectTimeoutException e) {
                            _code = "TO7";
                            request.abort();
                    } catch (Exception e) {
                        e.printStackTrace();
                        request.abort();
                        _code = "UE";
                        _value = e.getClass().getName() + ": " + toString();
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                        if (request != null) {
                            request.releaseConnection();
                        }
                        if (response != null) {
                        	try {
                        		response.close();
                        	} catch (Exception e) {
                        		//ignore
                        	}
                        }
                    }
                }
            });
        callThread.start();

        long time = System.currentTimeMillis();
        while (_code == null && System.currentTimeMillis() - time < 10000) {
            try {
                Thread.sleep(2);
            } catch (Exception e) {
            }
        }
        if (_code == null) {
            _code = "TO8";
            if (callThread.isAlive()) callThread.interrupt();
        }
        checkError(_code);
    }

	@Override
	public void close() throws com.interop.api.TurtleAPI.TurtleAPIException {
		/*
		try {
			_httpClient.close();
		} catch (IOException e) {
			TurtleAPIException te = new TurtleAPIException("Error closing http client connection", "IO");
			throw te;
		}
		*/
	}
	
	private void checkError(String code) throws TurtleAPIException {

	    String msg = null;
	    if (_code.equals("OK")) {
	        return;
	    }
	    else if (_code.startsWith("TO")) {
	        msg = "Timeout calling Paraclete";
	    }
	    else if (_code.equals("LE")) {
	        msg = "Call to Paraclete returned empty response";
	    }
	    else if (_code.equals("CE")) {
	        msg = "Call to Paraclete Failed. Check userid/password";
	    }
	    else if (_code.equals("SE")) {
	    	if (_value != null && _value.equals("404")) {
	    		msg =  "Call to Paraclete encountered invalid http code:" + _value + ". Please check that paraclete URL is correct.";
	    	} else {
	    		msg =  "Call to Paraclete encountered invalid http code: " + _value;
	    	}
	    }
	    else if (_code.equals("UE")) {
	        msg = "Unknown Parclete exception: " + _value;
	    }
	    else if (_code.equals("II")) {
	        msg = "Userid is not attached to an agency individual";
	    }
	    else if (_code.equals("IC")) {
	        msg = "No sip userid/password associated with device";
	    }
	    else if (_code.equals("ID")) {
	        msg = "Agency individual is not attached to a device";
	    }
	    else if (_code.equals("IS")) {
	        msg = "Server is not currently responsible for this switch";
	    }
	    else if (_code.equals("IA")) {
	        msg = "Invalid server action";
	    }
	    else if (_code.equals("AR")) {
	        msg = "User is already registered with another device";
	    }
	    else if (_code.equals("SC")) {
	        msg = "setCredentials() was never called. Program error";
	    }
	    else if (_code.equals("EX")) {
	        msg = "Unknown server exception encountered calling Paraclete. Check server log";
	    }
	    else if (_code.equals("NU")) {
	        msg = "No url supplied for callAPI()";
	    }
	    else { 
	        msg = "Unknown Paraclete error code: " + _code;
	    }
	    throw new TurtleAPI.TurtleAPIException(msg, _code);
	}

	 private void formatURL() {
	        if (_serverURL.endsWith(File.separator)) {
	                _serverURL = _serverURL.substring(0, _serverURL.length() - 1);
	        }
	    }
         /**
          * Download a file
          * @param remoteFilePath remove reference to server file
          * @param fos file output stream for local copy of file
          * @throws IOException
         * @throws  
          */
         public void downloadFile(final String remoteFilePath, final FileOutputStream fos) throws IOException {
        	 
        	 InputStream is = null;
             try {
                
                 //URL encode file name
                 String normalizedPath = new URI(remoteFilePath).toASCIIString();
                 URI url = new URI(_serverURL + normalizedPath);
                 logger.debug("download " + url);
                 HttpGet request = new HttpGet();
                 request.setURI(url);
                 CloseableHttpResponse response = _httpClient.execute(request);
                 is = response.getEntity().getContent();

                 byte[] buffer = new byte[1024];
                 int len1 = 0;
                 while ((len1 = is.read(buffer)) != -1) {
                     fos.write(buffer, 0, len1);
                 }
                 fos.flush();
                 logger.debug("done downloading file");
             } catch (URISyntaxException e) {
				IOException ioe = new IOException(e.getMessage());
				ioe.setStackTrace(e.getStackTrace());
				throw ioe;
			} finally {
                 if (is != null) {
                         try {
                                 is.close();
                         } catch (Exception e) {
                         }
                 }
                 if (fos != null) {
                         try {
                                 fos.close();
                         } catch (Exception e) {
                         }
                 }
             }
         }

         public void uploadFile(final String uri, final File image, final File thumbnail) throws TurtleAPIException {
        	 Runnable runnable = new Runnable() {
        		 public void run() {
                	 InputStream stream = null;
                     try {
                         URI url = new URI(_serverURL + Servlet_Path + uri);
                         logger.debug("upload url " + url);
                         HttpPost request = new HttpPost(url);
                         MultipartEntityBuilder builder = MultipartEntityBuilder.create();        
                         builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                         FileBody imageBody = new FileBody(image); 
                         builder.addPart("image", imageBody);
                         if (thumbnail != null) {
                             FileBody thumbnailBody = new FileBody(thumbnail); 
                             builder.addPart("thumbnail", thumbnailBody);
                         }
                         HttpEntity entity = builder.build();	
                         request.setEntity(entity);
                         
                         CloseableHttpResponse response = _httpClient.execute(request);
                         
                         if (response.getStatusLine().getStatusCode() == 200) {
                             if (response.getEntity().getContentLength() != 0) {
                                 stream = response.getEntity().getContent();
                                 long current = 0;
                                 long max = response.getEntity().getContentLength();
                                 StringBuffer buffer = new StringBuffer();
                                 byte[] bytes = new byte[1024];
                                 int bytesRead = 0;
                                 while ((max < 0 && bytesRead >= 0) || (max > 0 && current < max)) {
                                     bytesRead = stream.read(bytes);
                                     if (bytesRead > 0) {
                                         buffer.append(new String(bytes, 0, bytesRead));
                                         current = current + bytesRead;
                                     }
                                 }
                                 _result = buffer.toString();
                                 if (!_result.startsWith("{")) {
                                     stream.close();
                                     stream = null;
                                     _code = "CE";
                                 }
                                 else {
                                     JsonParser parser = _factory.createParser(_result);
                                     _node = _mapper.readTree(parser);
                                     _code = _node.get("status").textValue();
                                 }
                             }
                             else { //length error
                                 //response.getEntity().consumeContent();
                                 _code = "LE";
                             }
                         }
                         else { //bad status
                             //response.getEntity().consumeContent();                        
                             _code = "SE";
                             _value = Integer.toString(response.getStatusLine().getStatusCode());
                         }
                         //logger.debug("done uploading image");
                     } catch (java.net.SocketTimeoutException e) {
                         _code = "TO10";
                     } catch (org.apache.http.conn.ConnectTimeoutException e) {
                         _code = "TO11";
                     } catch (Exception e) {
                         e.printStackTrace();
                         _code = "UE";
                         if (e instanceof HttpHostConnectException) {
                            _value = e.getClass().getName() + ": " + ((HttpHostConnectException)e).getHost() + " " + e.getMessage(); 	
                         }
                         else {
                             _value = e.getClass().getName() + ": " + e.getMessage();
                         }
                     } finally {
                         if (stream != null) {
                             try {
                            	 stream.close();
                             } catch (Exception e) {
                             }
                         }
                     }
        		 }
        	 };
        	 
        	 Thread thread = new Thread(runnable);
        	 thread.start();
        	 try {
        		 //logger.debug("uploading started");
				 thread.join(20000);	//at most 20 seconds?
			 } catch (InterruptedException e) {
				 // TODO Auto-generated catch block
				 e.printStackTrace();
			 }
        	 //logger.debug("uploading finished");
         }

         @Override
         public void setTimeout(int value) {
         }
}
