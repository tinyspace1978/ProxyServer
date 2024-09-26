# ProxyServer
A fast reverse proxy to help you expose a local server behind a NAT or firewall to the internet.

#Usage
step 1:
run ProxyServer on your internet server

### java -jar ProxyServer.jar server
### or java -jar ProxyServer.jar server serverIp userName userToken


a nginx server is recommended.
ClientServer forward the request by Hostname.
for example:

    server {
        listen       80;
        server_name  internet.yourdomain.cn;
        add_header via $upstream_addr;

        location / {
	     proxy_pass    http://127.0.0.1:34681;
         include others.conf;
	     proxy_set_header Host 'inner.hostname.local'; #here is the hostname behind your firewall.
        }


        location /ws {  #some websocket
        	proxy_pass http://localhost:34681;
        	proxy_http_version 1.1;
		    proxy_set_header Host 'inner.hostname.local';
        	proxy_set_header Upgrade "websocket";  #websocket need these headers
        	proxy_set_header Connection "Upgrade";
        }
    }


step 2:
run ProxyServer on your local area network
###    java -jar ProxyServer.jar client internetServerIp
###or java -jar ProxyServer.jar client internetServerIp userName userToken

now you can visit http://inner.hostname.local via http://internet.yourdomain.cn

notice: internet Server need open port 38080 for your local network.
internet Server listen 34681 for web request.
