package it.unitn.ds1.Messages;

// Local imports
import it.unitn.ds1.Views.View;

public class ChangeView extends GenericMessage{

    // This is the view that is going to be installed via this message
    public View v;

    public ChangeView(int actorId, View toInstall){

        super(actorId);
        v = toInstall;

    }

}