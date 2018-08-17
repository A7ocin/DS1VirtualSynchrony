package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Views.View;
import it.unitn.ds1.Messages.JoinRequest;
import it.unitn.ds1.Messages.AssignId;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;
import it.unitn.ds1.Messages.Heartbeat;
import it.unitn.ds1.Messages.CanSendHeartbeat;
import it.unitn.ds1.Messages.CrashDetected;

// Java imports
import java.util.HashMap;
import java.time.*;

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
    private HashMap<Integer, Instant> heartbeats = new HashMap<Integer, Instant>();

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

        senderRef.tell(new CanSendHeartbeat(this.myId, getSelf()), getSelf());

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
            System.out.format("[%d] Killing %d\n", myId, actorId);
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

    private void checkIfCrashed(Heartbeat heartbeat){
        // CRASH DETECTION
        try {
            Instant previous = heartbeats.get(heartbeat.senderId);
            long delta = Duration.between(previous, Instant.now()).toMillis();
            //System.out.format("[%d] Delta: %d\n", myId, delta);
            if (delta > Ttimeout) {
                System.out.format("[%d] Process %d CRASHED!!! %d\n", myId, heartbeat.senderId, delta);
                CrashDetected crash = new CrashDetected(this.myId, heartbeat.senderId);
                getSelf().tell(crash, getSelf());
            }
        }
        catch(Exception e){
            // TODO
        }
    }

    private void onHeartbeatReceived(Heartbeat heartbeat){

        heartbeats.put(heartbeat.senderId, heartbeat.getBeat());
        //System.out.format("[%d] Received heartbeat from %d\n", myId, heartbeat.senderId);
        this.getContext().getSystem().scheduler().scheduleOnce(java.time.Duration.ofMillis(Ttimeout+1),
                new Runnable() {
                    @Override
                    public void run() {
                        checkIfCrashed(heartbeat);
                    }
                }, this.getContext().getSystem().dispatcher());
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