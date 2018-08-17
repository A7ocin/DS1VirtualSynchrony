package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.Props;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Views.View;
import it.unitn.ds1.Messages.JoinRequest;
import it.unitn.ds1.Messages.AssignId;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;
import it.unitn.ds1.Messages.Heartbeat;
import it.unitn.ds1.Messages.CrashDetected;

/**
 * Dedicated reliable group manager.
 * - ID = 0
 * - It serialises group view changes
 * - It sends view update messages to the group
 * - Receives crash notification messages from participants
 * - It can't crash
 */
public class GroupManager extends GenericActor{

    private int participantId = 1;
    private View vStart;

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

    @Override
    public void preStart(){
        System.out.println("- Group manager is alive");
        try{
            super.preStart();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        vStart = new View(0);
        vStart = vStart.buildNewView(myId, getSelf());
        installView(vStart);
    }

    private int assignNewId(ActorRef senderRef){
        senderRef.tell(new AssignId(myId, participantId), getSelf());
        System.out.format("[%d] New join request from actor %d\n", myId, participantId);
        return participantId++;
    }

    private void requestNewView(int actorId, ActorRef actor, boolean add){

        System.out.format("[%d] Requesting new view\n", myId);
        View out;
        if(add) {
            if (this.vTemp == null) {
                out = this.v.buildNewView(actorId, actor);
            } else {
                out = this.vTemp.buildNewView(actorId, actor);
            }
        }
        else{
            if (this.vTemp == null) {
                out = this.v.removeFromView(actorId);
            } else {
                out = this.vTemp.removeFromView(actorId);
            }
        }

        this.vTemp = out;
        sendMulticastChangeView(out);

        //TODO: Now it's time to complete the view request

    }

    private void onJoinRequest(JoinRequest request){
        setStatus(ActorStatusType.WAITING);
        // Get sender of the join request and change its ID
        ActorRef senderRef = getSender();
        int newId = assignNewId(senderRef);
        requestNewView(newId, senderRef, true);
    }

    private void onCrashDetected(CrashDetected crash){
        setStatus(ActorStatusType.WAITING);
        ActorRef senderRef = getSender();
        requestNewView(crash.crashedId, senderRef, false);
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
                .match(JoinRequest.class, this::onJoinRequest)
                .match(Message.class, this::onChatMessageReceived)
                .match(Heartbeat.class, this::onHeartbeatReceived)
                .match(CrashDetected.class, this::onCrashDetected)
                .build();
    }

}