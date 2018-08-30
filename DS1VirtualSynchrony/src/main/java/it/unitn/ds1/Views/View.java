package it.unitn.ds1.Views;

// Akka imports
import akka.actor.ActorRef;

// Java imports
import java.io.Serializable;
import java.util.HashMap;
import java.util.Arrays;
import org.apache.log4j.Logger;

public class View implements Serializable{

    public HashMap<Integer, ActorRef> participants;
    public int viewId;
    public final static Logger logger = Logger.getLogger(View.class);

    public View(int viewId, HashMap<Integer, ActorRef> ... newParticipants){

        this.viewId = viewId;
        if(newParticipants.length == 0){
            //logger.info("\n- Creating a new view "+viewId);
            this.participants = new HashMap<>();
        }
        else{
            //logger.info("- Updating view "+viewId);
            this.participants = new HashMap<Integer, ActorRef>(newParticipants[0]);
        }

    }

    public View buildNewView(int actorId, ActorRef actor){

        int viewNumber;
        if(this.participants.size() == 0 || (this.participants.size() == 1 && this.participants.containsValue(actor))){
            viewNumber =this.viewId;
        }
        else{
            viewNumber =this.viewId+1;
        }
        View updatedView = new View(viewNumber, this.participants);
        //System.out.println(Arrays.asList(this.participants));
        //logger.info("- Adding actor "+actorId+" to view "+updatedView.viewId);

        if(updatedView.participants.containsValue(actor)){
           updatedView.participants.values().removeIf(a -> a.equals(actor));
        }
        updatedView.participants.put(actorId, actor);

        //if(updatedView.participants.size()>1)
            //logger.info("- New view has "+updatedView.participants.size()+" actors");
        //else
            //logger.info("- New view has "+updatedView.participants.size()+" actor");
        //System.out.println(Arrays.asList(updatedView.participants));

        return updatedView;

    }

    public View removeFromView(int actorId){

        View updatedView = new View(this.viewId+1, this.participants);
        //System.out.println(Arrays.asList(this.participants));
        //logger.info("- Removing actor "+actorId+" from view "+updatedView.viewId);

        updatedView.participants.remove(actorId);

        //if(updatedView.participants.size()>1)
            //logger.info("- New view has "+updatedView.participants.size()+" actors");
        //else
            //logger.info("- New view has "+updatedView.participants.size()+" actor");
        //System.out.println(Arrays.asList(updatedView.participants));

        return updatedView;

    }

    public boolean happensBefore(View newV){
        //System.out.format("[%d %d]\n", this.viewId, newV.viewId);
        return this.viewId < newV.viewId;
    }

}
