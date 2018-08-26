package it.unitn.ds1.Messages;

// Java imports
import java.lang.String;
import java.time.*;

public class FlushMessage extends GenericMessage{

    public FlushMessage(int actorId){

        super(actorId);

    }

}