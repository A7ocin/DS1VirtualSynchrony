package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Messages.JoinRequest;
import it.unitn.ds1.Messages.AssignId;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;
import it.unitn.ds1.Messages.Heartbeat;
import it.unitn.ds1.Messages.FlushMessage;
import it.unitn.ds1.Messages.CanSendHeartbeat;

// Java imports
import java.lang.Exception;

/**
 * Participant class.
 * - It is not allowed to send view update messages
 * - It notifies the GroupManager when it detects a crash
 */
public class Participant extends GenericActor{


    ActorRef manager;

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

        logger.info("["+myId+"] New id: "+myId);
    }

    public void onCanSendHeartbeat(CanSendHeartbeat message){
        this.manager = message.manager;
        sendHeartbeat();
    }

    public void sendHeartbeat(){
        Heartbeat h = new Heartbeat(myId);
        //System.out.format("[%d] MANAGER: %s\n", myId, manager);
        //System.out.format("[%d] Sent heartbeat to %s\n", myId, manager);
        manager.tell(h, getSelf());
        //networkDelay();

        this.getContext().getSystem().scheduler().scheduleOnce(java.time.Duration.ofMillis(500),
                new Runnable() {
                    @Override
                    public void run() {
                        sendHeartbeat();
                    }
                }, this.getContext().getSystem().dispatcher());
    }

    @Override
    public void preStart(){
        logger.info("- New actor is asking to join");
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
                .match(CanSendHeartbeat.class, this::onCanSendHeartbeat)
                .match(FlushMessage.class, this::onFlushMessageReceived)
                .build();
    }

}