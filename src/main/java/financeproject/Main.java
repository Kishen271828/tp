package financeproject;

import command.BaseCommand;
import customexceptions.CategoryNotFoundException;
import customexceptions.ExceededAttemptsException;
import customexceptions.ExitLoginException;
import customexceptions.InactivityTimeoutException;
import customexceptions.IncompletePromptException;
import customexceptions.IncorrectCommandSyntaxException;
import customexceptions.UndoNotPermittedException;
import financialtransactions.TransactionManager;
import parser.Parser;
import storage.Storage;
import user.Authentication;
import user.BaseUser;
import user.InactivityTimer;
import userinterface.UI;

public class Main {
    public static void main(String[] args) throws SecurityException {
        Storage storage = new Storage("./data"); // Storage manager for jar file
        TransactionManager manager;

        UI ui = new UI();
        ui.printMessage("Welcome. Enter your username and password to login.");

        Parser parser = new Parser(ui);
        BaseCommand baseCommand = null;
        String response = "";

        // Authenticating user
        BaseUser user;
        InactivityTimer inactivityTimer = new InactivityTimer();
        try {
            ui.printMessage("Username: ");
            response = ui.readInput();
            //user = storage.loadUser(response);
            if (!response.toLowerCase().equals("bob")){
                ui.printMessage("User not found, exiting program");
                return;
            }
            user = storage.loadMockUser();
            Authentication.authenticateUser(user, ui);
            ui.printMessage("User has been authenticated. Starting program...");
        } catch (ExceededAttemptsException | ExitLoginException e) {
            ui.printMessage(e.getMessage());
            return;
        }
        try {
            manager = storage.loadFile(user.getUsername());
        } catch (CategoryNotFoundException e){
            ui.printMessage(e.getMessage());
            return;
        }
        ui.printMessage(manager.generateQuickReport());

        // Main program flow
        ui.printLine();
        ui.printMessage("How can we help you financially today?\n" + //
                                "Type 'help' to view guide");
        ui.printLine();
        do {
            response = ui.readInput();
            parser.setManager(manager);
            try {
                baseCommand = parser.parseCommand(response);
                response = baseCommand.execute(manager);
                ui.printLine();
                ui.printMessage(response);
                ui.printLine();
                inactivityTimer.resetTimer();
            } catch (IncompletePromptException | IncorrectCommandSyntaxException |
                     IllegalArgumentException | UndoNotPermittedException e) {
                ui.printMessage(e.getMessage());
            } catch (Exception e) {
                ui.printMessage("Uh-oh, something went wrong: " + e.getMessage());
            }

            try{
                ui.printMessage(storage.saveFile(user.getUsername(), manager));
            } catch (Exception e){
                ui.printMessage(e.getMessage());
            }

            try {
                inactivityTimer.checkTimeElapsed();
            } catch (InactivityTimeoutException e) {
                if (e.isTimeOut()) {
                    assert baseCommand != null;
                    baseCommand.setIsExit(true);
                } else if (e.isGracePeriod()) {
                    ui.printMessage("Some time has passed. Would you still like to continue: ");
                    String wantToContinue = ui.readInput();
                    if (wantToContinue.equalsIgnoreCase("y") ||
                            wantToContinue.equalsIgnoreCase("yes")) {
                        ui.printMessage("Session continued.");
                        inactivityTimer.resetTimer();
                    } else if (wantToContinue.equalsIgnoreCase("n") ||
                            wantToContinue.equalsIgnoreCase("no")) {
                        ui.printMessage("Session ended. ");
                        assert baseCommand != null;
                        baseCommand.setIsExit(true);
                    }
                }
            }
        } while (baseCommand == null || !baseCommand.isExit());
        ui.closeScanner();
    }
}
