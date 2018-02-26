package me.smartproxy.tunnel.httpconnect;

import android.util.Base64;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.Bidi;
import android.util.Base64;
import me.smartproxy.core.ProxyConfig;
import me.smartproxy.tunnel.Tunnel;
import me.smartproxy.core.tmpConfig;

public class HttpConnectTunnel extends Tunnel {

	private boolean m_TunnelEstablished;
	private HttpConnectConfig m_Config;
	
	public HttpConnectTunnel(HttpConnectConfig config,Selector selector) throws IOException {
		super(config.ServerAddress,selector);
		m_Config=config;
	}

	@Override
	protected void onConnected(ByteBuffer buffer) throws Exception {
        String auth = String.format("%s:%s", tmpConfig.UserName, tmpConfig.Password);
        if (auth.equals("")){
            auth = "";
        } else {
            auth = Base64.encodeToString(auth.getBytes("UTF-8"), Base64.NO_PADDING);
            auth = auth.replace("\n", "");
            auth = String.format("\r\nProxy-Authorization: Basic %s", auth);

        }
        System.out.print(m_DestAddress.getHostName());
		String request = String.format("CONNECT %s:%d HTTP/1.1\r\nHOST: %s:%d\r\nAccept: */*\r\nProxy-Connection: keep-alive\r\nUser-Agent: %s\r\nX-App-Install-ID: %s%s\r\n\r\n",
				m_DestAddress.getHostName(),
				m_DestAddress.getPort(),
				m_DestAddress.getHostName(),
				m_DestAddress.getPort(),
				ProxyConfig.Instance.getUserAgent(),
				ProxyConfig.AppInstallID,
                auth
        );
		
		buffer.clear();
		buffer.put(request.getBytes());
		buffer.flip();
		if(this.write(buffer,true)){//发送连接请求到代理服务器
			this.beginReceive();//开始接收代理服务器响应数据
		}
	}

	void trySendPartOfHeader(ByteBuffer buffer)  throws Exception {
		int bytesSent=0;
		int _size = 1024;
		if(buffer.remaining()>_size){
			int pos=buffer.position()+buffer.arrayOffset();
    		String firString=new String(buffer.array(),pos,_size).toUpperCase();

			int limit=buffer.limit();
			buffer.limit(buffer.position()+_size);
			super.write(buffer,false);
			bytesSent=_size-buffer.remaining();
			buffer.limit(limit);
			if(ProxyConfig.IS_DEBUG)
				System.out.printf("Send %d bytes(%s) to %s\n",bytesSent,firString,m_DestAddress);
    		/*if(firString.startsWith("GET /") || firString.startsWith("POST /")){
    			int limit=buffer.limit();
    			buffer.limit(buffer.position()+_size);
    			super.write(buffer,false);
    			bytesSent=_size-buffer.remaining();
    			buffer.limit(limit);
    			if(ProxyConfig.IS_DEBUG)
    				System.out.printf("Send %d bytes(%s) to %s\n",bytesSent,firString,m_DestAddress);
    		} else {
    			System.out.printf("debug: %s\n", firString);
			}*/
		}
	}
	
 
	@Override
	protected void beforeSend(ByteBuffer buffer) throws Exception {
		if(ProxyConfig.Instance.isIsolateHttpHostHeader()){
    		trySendPartOfHeader(buffer);//尝试发送请求头的一部分，让请求头的host在第二个包里面发送，从而绕过机房的白名单机制。
    	}
	}

	@Override
	protected void afterReceived(ByteBuffer buffer) throws Exception {
		if(!m_TunnelEstablished){
			//收到代理服务器响应数据
			//分析响应并判断是否连接成功
			String response=new String(buffer.array(),buffer.position(),12);
			System.out.println(response);
			// if(response.matches("^HTTP/1.[01] 200$")||response.matches("^HTTP/1.[01] 403$")){
			if(response.matches("^HTTP/1.[01] [0-9]+$")) {
				buffer.limit(buffer.position());
			}else {
				throw new Exception(String.format("Proxy server responsed an error: %s",response));
			}

			m_TunnelEstablished=true;
			super.onTunnelEstablished();
		}
	}

	@Override
	protected boolean isTunnelEstablished() {
		return m_TunnelEstablished;
	}

	@Override
	protected void onDispose() {
		m_Config=null;
	}

 
}
