package it.unitn.ds1.Views;

// Akka imports
import akka.actor.ActorRef;

// Java imports
import java.io.Serializable;
import java.util.HashMap;
import java.util.Arrays;

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

        View updatedView = new View(this.viewId+1, this.participants);
        //System.out.println(Arrays.asList(this.participants));
        System.out.format("- Adding actor %d to view %d\n", actorId, updatedView.viewId);

        if(updatedView.participants.containsValue(actor)){
           updatedView.participants.values().removeIf(a -> a.equals(actor));
        }
        updatedView.participants.put(actorId, actor);

        System.out.format("- New view has %d actors\n", updatedView.participants.size());
        //System.out.println(Arrays.asList(updatedView.participants));

        return updatedView;

    }

    public boolean happensBefore(View newV){
        //System.out.format("[%d %d]\n", this.viewId, newV.viewId);
        return this.viewId < newV.viewId;
    }

}