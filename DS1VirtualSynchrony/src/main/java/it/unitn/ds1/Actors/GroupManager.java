package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.Props;
import akka.actor.ActorRef;

/**
 * Dedicated reliable group manager.
 * - ID = 0
 * - It serialises group view changes
 * - It sends view update messages to the group
 * - Receives crash notification messages from participants
 */
public class GroupManager extends GenericActor{

    /**
     * Group Manager constructor. Its ID will always be 0 by default.
     */
    public GroupManager(int id, String remotePath){
        super(remotePath);
        myId = id;
    }

    static public Props props(int id, String remotePath) {
        return Props.create(GroupManager.class, () -> new GroupManager(id, remotePath));
    }

    /**
     * Handling incoming messages.
     * Define the mapping between incoming message classes and the methods of the actor
     * @return A Receive object
     */
    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .build();
    }

}