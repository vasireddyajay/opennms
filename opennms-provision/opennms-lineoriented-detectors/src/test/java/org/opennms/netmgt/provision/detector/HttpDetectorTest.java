/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2008 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 * OpenNMS Licensing       <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */
package org.opennms.netmgt.provision.detector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.provision.DetectFuture;
import org.opennms.netmgt.provision.server.SimpleServer;
import org.opennms.netmgt.provision.support.NullDetectorMonitor;


public class HttpDetectorTest {
    
    private AsyncHttpDetector m_detector;
    private SimpleServer m_server;
    
    private String headers = "HTTP/1.1 200 OK\r\n"
                            + "Date: Tue, 28 Oct 2008 20:47:55 GMT\r\n"
                            + "Server: Apache/2.0.54\r\n"
                            + "Last-Modified: Fri, 16 Jun 2006 01:52:14 GMT\r\n"
                            + "ETag: \"778216aa-2f-aa66cf80\"\r\n"
                            + "Accept-Ranges: bytes\r\n"
                            + "Vary: Accept-Encoding,User-Agent\r\n"
                            + "Connection: close\r\n"
                            + "Content-Type: text/html\r\n";
    
    private String serverContent = "<html>\r\n"
                                    + "<body>\r\n"
                                    + "<!-- default -->\r\n"
                                    + "</body>\r\n"
                                    + "</html>\r\n";
    
    private String serverOKResponse = headers + String.format("Content-Length: %s\r\n", serverContent.length()) + "\r\n" + serverContent;
                    
    
    private String notFoundResponse = "HTTP/1.1 404 Not Found\r\n"
                                    + "Date: Tue, 28 Oct 2008 20:47:55 GMT\r\n"
                                    + "Server: Apache/2.0.54\r\n"
                                    + "Last-Modified: Fri, 16 Jun 2006 01:52:14 GMT\r\n"
                                    + "ETag: \"778216aa-2f-aa66cf80\"\r\n"
                                    + "Accept-Ranges: bytes\r\n"
                                    + "Content-Length: 52\r\n"
                                    + "Vary: Accept-Encoding,User-Agent\r\n"
                                    + "Connection: close\rn"
                                    + "Content-Type: text/html\r\n"
                                    + "\r\n"
                                    + "<html>\r\n"
                                    + "<body>\r\n"
                                    + "<!-- default -->\r\n"
                                    + "</body>\r\n"
                                    + "</html>";
    
    private String notAServerResponse = "NOT A SERVER";
    

    
    @Before
    public void setUp() throws Exception {
        m_detector = new AsyncHttpDetector();
        
        
    }
    
    @After
    public void tearDown() throws IOException {
       if(m_server != null) {
           m_server.stopServer();   
       } 
    }
    
    @Test
    public void testDetectorFailNotAServerResponse() throws Exception {
        m_detector.init();
        m_server = createServer(notAServerResponse);
        m_detector.setPort(m_server.getLocalPort());
        
       assertFalse(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    @Test
    public void testDetectorFailNotFoundResponseMaxRetCode399() throws Exception {
        m_detector.setCheckRetCode(true);
        m_detector.setUrl("/blog");
        m_detector.setMaxRetCode(301);
        m_detector.init();
        
        m_server = createServer(notFoundResponse);
        m_detector.setPort(m_server.getLocalPort());
        
       assertFalse(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    @Test
    public void testDetectorSucessMaxRetCode399() throws Exception {
        m_detector.setCheckRetCode(true);
        m_detector.setUrl("/blog");
        m_detector.setMaxRetCode(399);
        m_detector.init();
        
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        
       assertTrue(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    @Test
    public void testDetectorFailMaxRetCodeBelow200() throws Exception {
        m_detector.setCheckRetCode(true);
        m_detector.setUrl("/blog");
        m_detector.setMaxRetCode(199);
        m_detector.init();
        
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        
       assertFalse(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    @Test
    public void testDetectorMaxRetCode600() throws Exception {
        m_detector.setCheckRetCode(true);
        m_detector.setMaxRetCode(600);
        m_detector.init();
        
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        
       assertTrue(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    
    @Test
    public void testDetectorSucessCheckCodeTrue() throws Exception {
        m_detector.setCheckRetCode(true);
        m_detector.setUrl("http://localhost/");
        m_detector.init();
        m_detector.setIdleTime(1000);
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        
       assertTrue(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    @Test
    public void testDetectorSuccessCheckCodeFalse() throws Exception {
        m_detector.setCheckRetCode(false);
        m_detector.init();
        
        m_server = createServer(getServerOKResponse());
        m_detector.setPort(m_server.getLocalPort());
        
       assertTrue(doCheck(m_detector.isServiceDetected(m_server.getInetAddress(), new NullDetectorMonitor())));
    }
    
    public void setServerOKResponse(String serverOKResponse) {
        this.serverOKResponse = serverOKResponse;
    }

    public String getServerOKResponse() {
        return serverOKResponse;
    }
    
    
    private SimpleServer createServer(final String httpResponse) throws Exception {
        SimpleServer server = new SimpleServer() {
            
            public void onInit() {
                addResponseHandler(contains("GET"), shutdownServer(httpResponse));
            }
        };
        server.init();
        server.startServer();
        
        return server;
    }
    private boolean doCheck(DetectFuture future) throws InterruptedException {
        future.await();
        return future.isServiceDetected();
    }
}
