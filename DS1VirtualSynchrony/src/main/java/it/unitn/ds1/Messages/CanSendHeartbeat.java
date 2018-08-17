package it.unitn.ds1.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class CanSendHeartbeat extends GenericMessage{

    public int crashedId;
    public ActorRef manager;

    public CanSendHeartbeat(int actorId, ActorRef manager){
        super(actorId);
        this.manager = manager;
    }

}