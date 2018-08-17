package it.unitn.ds1.Messages;

import java.io.Serializable;

public class CrashDetected extends GenericMessage{

    public int crashedId;

    public CrashDetected(int actorId, int crashedId){
        super(actorId);
        this.crashedId = crashedId;
    }

}