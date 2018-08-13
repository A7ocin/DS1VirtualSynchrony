package it.unitn.ds1.Messages;

import java.io.Serializable;

public class JoinRequest extends GenericMessage{

    public JoinRequest(int actorId){
        super(actorId);
    }

}