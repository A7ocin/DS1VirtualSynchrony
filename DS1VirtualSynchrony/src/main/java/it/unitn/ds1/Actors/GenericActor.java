package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ActorSystem;
import scala.concurrent.Await;
import akka.util.Timeout;

// Local imports
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Enums.SendingStatusType;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Messages.Message;
import it.unitn.ds1.Messages.Heartbeat;
import it.unitn.ds1.Messages.FlushMessage;
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
import org.apache.log4j.Logger;

/**
 * Generic Akka Actor.
 * It is extended by:
 * - GroupManager
 * - Participant
 * It:
 * - Sends multicast messages to other actors
 */
public abstract class GenericActor extends AbstractActor{

    protected int myId = -1;                // Unique ID. Manager has a fixed ID = 0
    protected String remotePath;            // Remote TCP path for accessing a remote actor
    public ActorStatusType status;          // Current status of the actor
    public SendingStatusType sendingStatus; // Current multicast sending status
    public View v, vTemp = null;            // Current and "to be confirmed" views
    private int Td = 1000;                  // Time threshold for message exchange
    public int Ttimeout = 10000;            // Timeout for heartbeat receival
    public ActorRef manager;
    private HashMap<Integer, Message> unstableMessages = new HashMap<Integer, Message>();
    private HashSet<String> delivered = new HashSet<String>();
    private HashSet<Integer> flushMessages = new HashSet<Integer>();
    public int messageCounter = 0;

    @Override
    public void preStart(){
        //logger.info("- My id is " + this.myId);
    }

    public final static Logger logger = Logger.getLogger(GenericActor.class);

    /**
     * Generic Actor constructor.
     * Sets all the parameters seen by the VS system's actor
     * @param remotePath The path for accessing the remote actor
     */
    public GenericActor(String remotePath){
        this.remotePath = remotePath;
        this.status = ActorStatusType.WAITING;
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

    public void sendUnstableMessages(View v){

        //logger.info("[" + myId + "] Sending unstable messages");
        Iterator<HashMap.Entry<Integer, Message>> itUnstable = unstableMessages.entrySet().iterator();
        while(itUnstable.hasNext()){
            HashMap.Entry<Integer, Message> um = itUnstable.next();
            Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry<Integer, ActorRef> participant = it.next();
                if(participant.getKey() == myId){
                    continue;
                }
                participant.getValue().tell(um.getValue(), getSelf());
                //logger.info("[" + myId + "] Sending unstable message " + um.getValue().body + " to participant " + participant.getKey());
            }
            try{
                networkDelay();
            }
            catch(Exception e){
                System.out.println("SLEEP ERROR");
            }
        }

    }

    public void sendFlushMessage(View v){

        Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, ActorRef> participant = it.next();
            if(participant.getKey() == myId){
                continue;
            }
            participant.getValue().tell(new FlushMessage(this.myId), getSelf());
            try{
                networkDelay();
            }
            catch(Exception e){
                System.out.println("SLEEP ERROR");
            }
        }

    }

    public void sendHeartbeat() {
        if (this.myId!=0 && status == ActorStatusType.STARTED) {
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
    }

    public void installView(View vNew){
        // TODO: complete this method
        if(this.v == null || this.v.viewId != vNew.viewId) {
            //logger.info("[" + myId + "] Installed view " + vNew.viewId);
            logger.info(this.myId+" install view "+vNew.viewId+" "+vNew.participants.keySet().toString().replaceAll("\\[","").replaceAll("\\]","").replaceAll(" ",""));
        }
        this.v = vNew;
        this.vTemp = null;
        this.setStatus(ActorStatusType.STARTED);
        sendHeartbeat();
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
        //New message text
        ts = this.myId + ":" + messageCounter;
        messageCounter++;

        Message m = new Message(myId, ts);
        Iterator<HashMap.Entry<Integer, ActorRef>> it = v.participants.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Integer, ActorRef> participant = it.next();
            if (participant.getKey() == this.myId) {
                continue;
            }
            participant.getValue().tell(m, getSelf());
            //logger.info("["+myId+"] Sent new chat message "+ts+" to "+participant.getKey());
            logger.info(myId+" send multicast " + m.body + " within "+v.viewId);
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
            //logger.warn("["+myId+"] Cannot install new view "+request.v.viewId);
            // THIS RETURN COULD BE PROBLEMATIC
            return;
        }
        setStatus(ActorStatusType.WAITING);
        //logger.info("["+myId+"] Actor "+request.senderId+" requested a view change");

        sendUnstableMessages(request.v);

        sendFlushMessage(request.v);

        if(flushMessages.size() < request.v.participants.size()-1){
            //logger.info("Waiting for all flushes" + flushMessages.size() + " " + (request.v.participants.size()-1) );
            this.getContext().getSystem().scheduler().scheduleOnce(java.time.Duration.ofMillis(1000),
                    new Runnable() {
                        @Override
                        public void run() {
                            getSelf().tell(new ChangeView(myId, request.v), getSelf());
                            //onChangeView(request);
                        }
                    }, this.getContext().getSystem().dispatcher());
        }

        installView(request.v);

        //sendHeartbeat();

        sendChatMessage();

    }

    public void onChatMessageReceived(Message message){

        if(status == ActorStatusType.STARTED) {

            // Check if duplicate
            if (!delivered.contains(message.body)) {
                unstableMessages.put(message.senderId, message);
                delivered.add(message.body);
                //logger.info("["+myId+"] Received message "+message.body+" from "+message.senderId);
                logger.info(myId + " deliver multicast " + message.body + " from " + message.senderId + " within " + v.viewId);
            }
        }

    }

    public void onFlushMessageReceived(FlushMessage flush){

        flushMessages.add(flush.senderId);

    }


}
