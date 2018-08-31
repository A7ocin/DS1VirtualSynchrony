package it.unitn.ds1.Messages;

// Java imports
import java.lang.String;

// Local imports
import it.unitn.ds1.Views.View;

public class Message extends GenericMessage{

    public String body;
    public View v;

    public Message(int actorId, String body, View view){
        super(actorId);
        this.body = body;
        this.v = view;
    }

}