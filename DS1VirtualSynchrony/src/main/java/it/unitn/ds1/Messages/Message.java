package it.unitn.ds1.Messages;

// Java imports
import java.lang.String;

public class Message extends GenericMessage{

    public String body;

    public Message(int actorId, String body){
        super(actorId);
        this.body = body;
    }

}