package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.AbstractActor;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Enums.SendingStatusType;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;
import it.unitn.ds1.Messages.Heartbeat;
import it.unitn.ds1.Messages.CanSendHeartbeat;
import it.unitn.ds1.Messages.CrashDetected;
import it.unitn.ds1.Views.View;

// Java imports
import java.util.HashMap;
import java.util.Map;
import java.lang.String;
import java.lang.Exception;
import java.util.Iterator;
import java.util.Date;
import java.sql.Timestamp;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.HashSet;
import java.time.*;

/**
 * Generic Akka Actor.
 * It is extended by:
 * - GroupManager
 * - Participant
 * It:
 * - Sends multicast messages to other actors
 */
public abstract class GenericActor extends AbstractActor{

    protected int myId;                     // Unique ID. Manager has a fixed ID = 0
    protected String remotePath;            // Remote TCP path for accessing a remote actor
    public ActorStatusType status;          // Current status of the actor
    public SendingStatusType sendingStatus; // Current multicast sending status
    public View v, vTemp = null;            // Current and "to be confirmed" views
    private int Td = 1000;                  // Time threshold for message exchange
    public int Ttimeout = 10000;            // Timeout for heartbeat receival
    public ActorRef manager;
    private HashMap<Integer, Message> unstableMessages = new HashMap<Integer, Message>();
    private HashSet<String> delivered = new HashSet<String>();

    /**
     * Generic Actor constructor.
     * Sets all the parameters seen by the VS system's actor
     * @param remotePath The path for accessing the remote actor
     */
    public GenericActor(String remotePath){
        this.remotePath = remotePath;
        this.status = ActorStatusType.STARTED;
    }

    public void setStatus(ActorStatusType newStatus){
        this.status = newStatus;
    }

    /**
     * Check if the current actor has crashed
     * @return true if it did
     */
    public boolean isCrashed(){
        return this.status == ActorStatusType.CRASHED;
    }

    /**
     * Check if a view (or view proposal) exists
     * @param newV the view to be checked
     * @return true if it exists
     */
    public boolean viewExists(View newV){
        return newV != null;
    }

    /**
     * Check if the minimum requirements for installing the view are satisfied
     * @param newV the new view
     * @return true if they are satisfied
     */
    public boolean canInstallView(View newV){
        if(viewExists(newV)){
            if(this.v==null || this.v.happensBefore(newV)){
                return true;
            }
            return false;
        }
        return false;
    }

    public void sendUnstableMessages(){

        System.out.format("[%d] Sending unstable messages\n", myId);
        Iterator<HashMap.Entry<Integer, Message>> itUnstable = unstableMessages.entrySet().iterator();
        while(itUnstable.hasNext()){
            HashMap.Entry<Integer, Message> um = itUnstable.next();
            Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<Integer, ActorRef> participant = it.next();
                participant.getValue().tell(um.getValue(), getSelf());
                System.out.format("[%d] Sending unstable message %s to partecipant %d\n", myId, um.getValue().body, participant.getKey());
                try{
                    networkDelay();
                }
                catch(Exception e){
                    System.out.println("SLEEP ERROR");
                }
            }
        }

    }

    public void sendFlushMessage(){

        System.out.format("[%d] Sending flush message\n", myId);

    }

    public void installView(View vNew){
        // TODO: complete this method
        this.v = vNew;
        this.vTemp = null;
        System.out.format("[%d] Installed view %d\n", myId, vNew.viewId);
    }

    public void sendMulticastChangeView(View v){
        ChangeView cvm = new ChangeView(myId, v);
        Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, ActorRef> participant = it.next();
            participant.getValue().tell(cvm, getSelf());
            System.out.format("[%d] Telling partecipant %d to change view to %d\n", myId, participant.getKey(), v.viewId);
            try{
                networkDelay();
            }
            catch(Exception e){
                System.out.println("SLEEP ERROR");
            }
        }
    }

    public void networkDelay(){
        Random r = new Random();
        int min = 100;
        int time = r.nextInt((Td - min) + 1) + min;
        try{
            Thread.sleep(time);
        }
        catch(Exception e){
            System.out.println("SLEEP ERROR");
        }
    }

    public void sendChatMessage() {
        Date date = new Date();
        long time = date.getTime();
        String ts = "[" + this.myId + "] " + new Timestamp(time).toString();

        Message m = new Message(myId, ts);
        Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, ActorRef> participant = it.next();
            if (participant.getKey() == this.myId) {
                continue;
            }
            participant.getValue().tell(m, getSelf());
            System.out.format("[%d] Sent new chat message %s to %d\n", myId, ts, participant.getKey());
            networkDelay();
        }

        this.getContext().getSystem().scheduler().scheduleOnce(java.time.Duration.ofMillis(1000),
                new Runnable() {
                    @Override
                    public void run() {
                        //getSelf().tell(m, getSelf());
                        sendChatMessage();
                    }
                }, this.getContext().getSystem().dispatcher());
    }

    public void onChangeView(ChangeView request){
        if(isCrashed() || !canInstallView(request.v)){
            System.out.format("[%d] Can't install new view %d\n", myId, request.v.viewId);
            return;
        }
        setStatus(ActorStatusType.WAITING);
        System.out.format("[%d] Actor %d requested a view change\n", myId, request.senderId);

        sendUnstableMessages();

        sendFlushMessage();

        installView(request.v);

        //sendHeartbeat();

        sendChatMessage();

    }

    public void onChatMessageReceived(Message message){

        if(isCrashed()){
            return;
        }

        // Check if duplicate
        if(!delivered.contains(message.body)){
            unstableMessages.put(message.senderId, message);
            delivered.add(message.body);
            System.out.format("[%d] Received message %s from %d\n", myId, message.body, message.senderId);
        }

    }

//    public void onHeartbeatReceived(Heartbeat heartbeat){
//
//        if(isCrashed()){
//            return;
//        }
//        //System.out.format("[%d] Received heartbeat from %d\n", myId, heartbeat.senderId);
//        heartbeats.put(heartbeat.senderId, heartbeat.getBeat());
//
//    }


}