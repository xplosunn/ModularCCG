package launch;

import client.Main;
import server.ServerRun;

/**
 * Created by HS on 17-07-2014.
 */
public class ClientPlusServer {
    public static void main(String args[]){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerRun.main(null);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Main.main(null);
            }
        }).start();
    }
}
