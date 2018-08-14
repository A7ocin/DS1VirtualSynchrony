package it.unitn.ds1.Views;

// Akka imports
import akka.actor.ActorRef;

// Java imports
import java.io.Serializable;
import java.util.HashMap;

public class View implements Serializable{

    public HashMap<Integer, ActorRef> participants;
    public int viewId;

    public View(int viewId, HashMap<Integer, ActorRef> ... newParticipants){

        this.viewId = viewId;
        if(newParticipants.length == 0){
            System.out.format("- Creating a new view %d\n", viewId);
            this.participants = new HashMap<>();
        }
        else{
            System.out.format("- Updating view %d\n", viewId);
            this.participants = new HashMap<Integer, ActorRef>(newParticipants[0]);
        }

    }

    public View buildNewView(int actorId, ActorRef actor){

        View updatedView = this;

        if(updatedView.participants.containsValue(actor)){
           updatedView.participants.values().removeIf(a -> a.equals(actor));
        }
        updatedView.participants.put(actorId, actor);

        return updatedView;

    }

    public boolean happensBefore(View newV){
        return this.viewId < newV.viewId;
    }

}