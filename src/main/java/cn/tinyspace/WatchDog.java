package cn.tinyspace;

import java.util.List;

public class WatchDog extends Thread {
    List<ClientServer> watch;
    int maxClients;
    int clientPort;

    public WatchDog(List<ClientServer> watch, int maxClients, int clientPort) {
        this.watch = watch;
        this.maxClients = maxClients;
        this.clientPort = clientPort;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(5000L);
            } catch (Exception e) {

            }
            for (int i = 0; i < watch.size(); i++) {
                ClientServer clientServer = watch.get(i);
                if (clientServer.getClient() == null || !clientServer.getClient().isConnected()) {
                    watch.remove(i);
                    break;
                }
            }
            try {
                Thread.sleep(500L);
            } catch (Exception e) {
            }
            if (watch.size() < maxClients) {
                ClientServer clientServer = new ClientServer(clientPort, null);
                ConnectionManager.getInstance().getCachedExecutor().execute(clientServer);
                watch.add(clientServer);
            }
        }
    }
}
