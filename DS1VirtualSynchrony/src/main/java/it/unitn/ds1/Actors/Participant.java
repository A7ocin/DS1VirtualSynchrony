package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.Props;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Messages.JoinRequest;
import it.unitn.ds1.Messages.AssignId;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;

// Java imports
import java.lang.Exception;

/**
 * Participant class.
 * - It is not allowed to send view update messages
 * - It notifies the GroupManager when it detects a crash
 */
public class Participant extends GenericActor{


    /**
     * Participant constructor. Its ID is assigned by the Group Manager
     */
    public Participant(String remotePath){
        super(remotePath);
    }

    static public Props props(String remotePath) {
        return Props.create(Participant.class, () -> new Participant(remotePath));
    }

    public void onAssignId(AssignId request){
        if(isCrashed()){
            return;
        }
        myId = request.id;

        System.out.format("[%d] New id: %d\n", myId, myId);
    }

    @Override
    public void preStart(){
        System.out.println("- New actor is asking to join");
        try{
            super.preStart();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        getContext().actorSelection(this.remotePath).tell(new JoinRequest(this.myId), getSelf());
    }

    /**
     * Handling incoming messages.
     * Define the mapping between incoming message classes and the methods of the actor
     * @return A Receive object
     */
    @Override
    public Receive createReceive(){
        return receiveBuilder()
                .match(ChangeView.class, this::onChangeView)
                .match(AssignId.class, this::onAssignId)
                .match(Message.class, this::onChatMessageReceived)
                .build();
    }

}