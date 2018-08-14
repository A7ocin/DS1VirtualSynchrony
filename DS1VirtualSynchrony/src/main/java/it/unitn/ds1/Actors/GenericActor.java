package it.unitn.ds1.Actors;

// Akka imports
import akka.actor.AbstractActor;
import akka.actor.ActorRef;

// Local imports
import it.unitn.ds1.Enums.ActorStatusType;
import it.unitn.ds1.Messages.ChangeView;
import it.unitn.ds1.Views.View;

/**
 * Generic Akka Actor.
 * It is extended by:
 * - GroupManager
 * - Participant
 * It:
 * - Sends multicast messages to other actors
 */
public abstract class GenericActor extends AbstractActor{

    protected int myId;             // Unique ID. Manager has a fixed ID = 0
    protected String remotePath;    // Remote TCP path for accessing a remote actor
    public ActorStatusType status;  // Current status of the actor
    public View v, vTemp;           // Current and "to be confirmed" views

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
            if(this.v.happensBefore(newV)){
                return true;
            }
            return false;
        }
        return false;
    }


    public void onChangeView(ChangeView request){
        if(isCrashed() || !canInstallView(request.v)){
            System.out.format("[%d] Can't install new view", myId);
            return;
        }
        setStatus(ActorStatusType.WAITING);
        System.out.format("[%d] Actor %d requested a view change", myId, request.senderId);

    }

}