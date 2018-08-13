package it.unitn.ds1.Messages;

import java.io.Serializable;

public class GenericMessage implements Serializable{

    // The ID of the actor who is sending the message
    final public int senderId;

    public GenericMessage(int actorId){
        senderId = actorId;
    }

}