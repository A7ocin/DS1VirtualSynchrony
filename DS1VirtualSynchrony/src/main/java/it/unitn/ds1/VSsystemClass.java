package it.unitn.ds1;

// Akka imports
import akka.actor.ActorSystem;
import akka.actor.ActorRef;

// Config imports
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;

// Local imports
import it.unitn.ds1.Actors.ActorType;
import it.unitn.ds1.Actors.GenericActor;
import it.unitn.ds1.Actors.GroupManager;
import it.unitn.ds1.Actors.Participant;


/**
 * DS1 2018 final project: Virtual Synchrony
 * @author Nicola Garau
 * @author Salvatore Manfredi
 */

public class VSsystemClass{

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
            System.out.println("- Node is PARTICIPANT");
            actorType = ActorType.PARTICIPANT;
            String remote_ip = config.getString("nodeapp.remote_ip");
            int remote_port = config.getInt("nodeapp.remote_port");
            // Starting with a bootstrapping node
            // The Akka path to the bootstrapping peer
            remotePath = "akka.tcp://mysystem@"+remote_ip+":"+remote_port+"/user/node";
            System.out.println("Starting participant node; bootstrapping node: " + remote_ip + ":"+ remote_port);
        }
        else {
            System.out.println("- Node is MANAGER");
            actorType = ActorType.MANAGER;
            System.out.println("- Starting manager node, id=" + myId);
        }
        // Create the actor system
        System.out.println("- Creating actor system...");
        final ActorSystem system = ActorSystem.create("mysystem", config);
        System.out.println("- Actor system created");

        switch(actorType){
            case PARTICIPANT:
                // Create a participant node actor
                System.out.println("- Creating PARTICIPANT node...");
                final ActorRef receiverParticipant = system.actorOf(
                        Participant.props(remotePath),
                        "node"      // actor name
                );
                System.out.println("- PARTICIPANT node created");
                break;
            case MANAGER:
                // Create a master node actor
                System.out.println("- Creating MANAGER node...");
                final ActorRef receiverManager = system.actorOf(
                        GroupManager.props(myId, remotePath),
                        "node"      // actor name
                );
                System.out.println("- MANAGER node created");
                break;
            default:
                System.out.println("Wrong actor type!");
                break;
        }
    }

}