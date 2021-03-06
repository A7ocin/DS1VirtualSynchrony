package it.unitn.ds1;

// Akka imports
import akka.actor.ActorSystem;
import akka.actor.ActorRef;

// Config imports
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;

// Local imports
import it.unitn.ds1.Enums.ActorType;
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Actors.GenericActor;
import it.unitn.ds1.Actors.GroupManager;
import it.unitn.ds1.Actors.Participant;

// Java imports
import java.net.Socket;
import java.lang.Exception;
import org.apache.log4j.Logger;


/**
 * DS1 2018 final project: Virtual Synchrony
 * @author Nicola Garau
 * @author Salvatore Manfredi
 */

public class VSsystemClass{

    public final static Logger logger = Logger.getLogger(VSsystemClass.class);

    private static boolean portAvailable(String host, int port) {
        boolean result = false;
        try {
            (new Socket(host, port)).close();
            result = true;
        }
        catch(Exception e) {
            // Could not connect.
        }

        return result;
    }

    /**
     * Entry point
     * Run with:
     * - gradle run -Dconfig=node0.conf
     * or
     * - gradle run -Dconfig=node1.conf
     * @param args
     */
    public static void main(String args[]){

        // Actor type used to switch between manager and participant nodes
        ActorType actorType = null;

        // Load configuration file and read our custom parameter (id will always be 0)
        Config config = ConfigFactory.load();
        int myId = config.getInt("nodeapp.id");

        // Our remote path for accessing remote actors (needs to be initialised)
        String remotePath = null;

        if (config.hasPath("nodeapp.remote_ip")) {
            //logger.info("- Node is PARTICIPANT");
            actorType = ActorType.PARTICIPANT;
            String remote_ip = config.getString("nodeapp.remote_ip");
            int remote_port = config.getInt("nodeapp.remote_port");
            String host = "akka.tcp://mysystem@"+remote_ip;
            // Starting with a bootstrapping node
            // The Akka path to the bootstrapping peer
            remotePath = "akka.tcp://mysystem@"+remote_ip+":"+remote_port+"/user/node";
            //logger.info("Starting participant node; bootstrapping node: " + remote_ip + ":"+ remote_port);
        }
        else {
            //logger.info("- Node is MANAGER");
            actorType = ActorType.MANAGER;
            //logger.info("- Starting manager node, id=" + myId);
        }
        // Create the actor system
        //logger.info("- Creating actor system...");
        final ActorSystem system = ActorSystem.create("mysystem", config);
        //logger.info("- Actor system created");

        switch(actorType){
            case PARTICIPANT:
                // Create a participant node actor
                //logger.info("- Creating PARTICIPANT node...");
                final ActorRef receiverParticipant = system.actorOf(
                        Participant.props(remotePath),
                        "node"      // actor name
                );
                //logger.info("- PARTICIPANT node created");
                break;
            case MANAGER:
                // Create a master node actor
                //logger.info("- Creating MANAGER node...");
                final ActorRef receiverManager = system.actorOf(
                        GroupManager.props(myId, remotePath),
                        "node"      // actor name
                );
                //logger.info("- MANAGER node created");
                break;
            default:
                //logger.info("Wrong actor type!");
                break;
        }
    }

}