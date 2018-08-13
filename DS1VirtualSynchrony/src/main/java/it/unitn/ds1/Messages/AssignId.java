package it.unitn.ds1.Messages;

public class AssignId extends GenericMessage{

    // The new id that the Manager assigns to the joining participant
    public int id;

    public AssignId(int actorId, int idTBA){
        super(actorId);
        id = idTBA;
    }

}