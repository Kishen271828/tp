package command;

import customexceptions.UndoNotPermittedException;
import financialtransactions.Inflow;
import financialtransactions.Outflow;
import financialtransactions.Reminder;
import financialtransactions.TransactionManager;
import user.InactivityTimer;

//@@author ChongXern
public class UndoCommand extends BaseCommand {
    private static final int PERMITTED_UNDO_TIME = 10_000;
    private BaseCommand commandToUndo = null;
    private Inflow inflow;
    private Outflow outflow;
    private Reminder reminder;
    private String action;
    private boolean canUndo = false;
    private boolean canExecute;
    private InactivityTimer timer;
    private long startTime;

    public UndoCommand(String[] commandParts) {
        super(false, commandParts);
        if (commandParts != null) {
            action = commandParts[0];
            System.out.println("ACTION IS " + action);
        }
        canExecute = false;
        timer = new InactivityTimer();
    }

    public void setInflow(Inflow inflow) {
        this.inflow = inflow;
        System.out.println("THIS INFLOW IS " + this.inflow.getName());
        this.outflow = null;
        this.reminder = null;
    }
    public void setOutflow(Outflow outflow) {
        this.inflow = null;
        this.outflow = outflow;
        this.reminder = null;
    }
    public void setReminder(Reminder reminder) {
        this.inflow = null;
        this.outflow = null;
        this.reminder = reminder;
    }

    public void setCanUndo(boolean canUndo) {
        this.canUndo = canUndo;
        startTime = System.currentTimeMillis();
    }

    private boolean didUndoTimerRunout() {
        long timeDifference = System.currentTimeMillis() - startTime;
        return timeDifference < PERMITTED_UNDO_TIME;
    }

    public void allowExecute(String lastAction) {
        canExecute = (lastAction != null);
    }

    public String execute(TransactionManager manager) throws Exception {
        if (!canExecute) {
            System.out.println("CANNOT EXECUTE UNDO");
            throw new UndoNotPermittedException(true, true);
        }
        System.out.println("EXECUTING COMMAND UNDO");
        switch (action) { // Compute how to undo the command to be undone
        case "delete-inflow":
            System.out.println("ADDING BACK INFLOW");
            canUndo = true;
            int inflowIndex = Integer.parseInt(commandParts[1].substring(2));
            Inflow inflowToRemove = manager.getNthInflowFromList(inflowIndex);
            return manager.addTransaction(inflowToRemove);
        case "delete-outflow":
            canUndo = true;
            int outflowIndex = Integer.parseInt(commandParts[1].substring(2));
            Outflow outflowToRemove = manager.getNthOutflowFromList(outflowIndex);
            return manager.addTransaction(outflowToRemove);
        case "delete-reminder":
            canUndo = true;
            int reminderIndex = Integer.parseInt(commandParts[1].substring(2));
            Reminder reminderToRemove = manager.getNthReminderFromList(reminderIndex);
            return manager.addTransaction(reminderToRemove);
        case "add-inflow":
            System.out.println("DELETING PREVIOUS INFLOW");
            canUndo = true;
            manager.removeTransaction(inflow);
            break;
        case "add-outflow":
            canUndo = true;
            manager.removeTransaction(outflow);
            break;
        case "add-reminder":
            canUndo = true;
            manager.removeTransaction(reminder);
            break;
        default:
            throw new UndoNotPermittedException(didUndoTimerRunout(), true);
        }
        if (canUndo) {
            canUndo = false;
            return "Ok. " + action + " has been undone.";
        }
        throw new UndoNotPermittedException(didUndoTimerRunout(), true);
    }
}
