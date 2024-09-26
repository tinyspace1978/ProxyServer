package cn.tinyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionManager {
    private static ConnectionManager instance = new ConnectionManager();
    private final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private final Object LOCK = new Object();
    private final Map<String, List<ProcessConnection>> allTcpServer = new ConcurrentHashMap<>();
    ExecutorService inExecutor = Executors.newCachedThreadPool();

    private ConnectionManager() {

    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public ExecutorService getCachedExecutor() {
        return inExecutor;
    }

    public Set<String> getAllHosts() {
        return allTcpServer.keySet();
    }

    public ProcessConnection popConnection() {
        for (int num = 0; num < 100; num++) {
            synchronized (LOCK) {
                for (String host : allTcpServer.keySet()) {
                    List<ProcessConnection> connections = allTcpServer.get(host);
                    if (connections != null) {
                        for (int i = 0; i < connections.size(); i++) {
                            ProcessConnection conn = connections.get(i);
                            if (conn == null) {
                                logger.error(i + " conn is null");
                                connections.remove(i);
                                i--;
                                continue;
                            }
                            //logger.info("conn " + num + ":" + i + ": " + conn.getName() + ":busy=" + conn.isBusy());
                            if (!conn.isBusy()) {
                                logger.debug("obtain conn:" + conn.getName());
                                conn.setBusy(true);
                                conn.setLastInTime(System.currentTimeMillis());
                                conn.setLastOutTime(System.currentTimeMillis());
                                return conn;
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(100L);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public ProcessConnection getConnection(String host) {
        List<ProcessConnection> connections = allTcpServer.get(host);
        if (connections != null && connections.size() > 0) {
            return connections.get(0);
        }
        return null;
    }

    public boolean isConnection(String host) {
        List<ProcessConnection> connections = allTcpServer.get(host);
        return connections != null && connections.size() > 0;
    }

    public synchronized void addConnection(String host, ProcessConnection processConnection) {
        List<ProcessConnection> connections = allTcpServer.get(host);
        if (connections == null) {
            connections = new LinkedList<>();
            connections.add(processConnection);
            allTcpServer.put(host, connections);
        } else {
            connections.add(processConnection);
        }
        logger.info(host + "加入" + processConnection.getName() + ":" + connections.size());
    }

    public void removeConnection(String host, String hostId) {
        List<ProcessConnection> connections = allTcpServer.get(host);
        if (connections != null) {
            for (int i = 0; i < connections.size(); i++) {
                ProcessConnection conn = connections.get(i);
                if (conn == null || conn.getName() == null) {
                    connections.remove(i);
                    i--;
                    continue;
                }
                if (conn.getName().equals(hostId)) {
                    connections.remove(i);
                    conn.close();
                    break;
                }
            }
        }
    }

    public void closeAll() {
        for (String host : allTcpServer.keySet()) {
            List<ProcessConnection> connections = allTcpServer.get(host);
            if (connections != null) {
                for (int i = 0; i < connections.size(); i++) {
                    ProcessConnection conn = connections.get(i);
                    conn.close();
                }
            }
        }
        allTcpServer.clear();
    }


}
