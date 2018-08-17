package it.unitn.ds1.Messages;

// Java imports
import java.lang.String;
import java.time.*;

public class Heartbeat extends GenericMessage{

    private Instant beat;

    public Instant getBeat(){
        return this.beat;
    }

    public Heartbeat(int actorId){

        super(actorId);

        this.beat = Instant.now();

    }

}