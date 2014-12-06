package ru.fizteh.fivt.students.dnovikov.storeable.Interpreter;

import ru.fizteh.fivt.students.dnovikov.storeable.DataBaseProvider;
import ru.fizteh.fivt.students.dnovikov.storeable.Exceptions.LoadOrSaveException;
import ru.fizteh.fivt.students.dnovikov.storeable.Exceptions.StopInterpreterException;
import ru.fizteh.fivt.students.dnovikov.storeable.Exceptions.WrongNumberOfArgumentsException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class Interpreter {

    private static final String PROMPT = "$ ";
    private final Map<String, Command> commands;
    private final DataBaseProvider dbConnector;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private boolean isBatch;

    public Interpreter(DataBaseProvider dbConnector, InputStream in, PrintStream out, PrintStream err, Command[] cmds) {
        if (in == null || out == null || err == null) {
            throw new IllegalArgumentException("InputStream or OutputStream is null");
        }
        this.in = in;
        this.out = out;
        this.err = err;
        this.dbConnector = dbConnector;
        this.commands = new HashMap<>();
        for (Command command : cmds) {
            this.commands.put(command.getName(), command);
        }
    }

    public void run(String[] args) {
        try {
            if (args.length == 0) {
                isBatch = false;
                interactiveMode();
            } else {
                isBatch = true;
                batchMode(args);
            }
        } catch (StopInterpreterException e) {
            // Stop the interpreter.
        }
    }

    private void batchMode(String[] args) throws StopInterpreterException {
        try {
            invokeLine(String.join(" ", args));
        } catch (IOException | WrongNumberOfArgumentsException e) {
            err.println(e.getMessage());
            try {
                if (dbConnector.getCurrentTable() != null) {
                    int unsavedChanges = dbConnector.getCurrentTable().getNumberOfUncommittedChanges();
                    if (unsavedChanges > 0) {
                        out.println(unsavedChanges + " unsaved changes");
                    } else {
                        dbConnector.saveTable();
                    }
                } else {
                    dbConnector.saveTable();
                }
            } catch (LoadOrSaveException exception) {
                err.println(exception.getMessage());
            }
            System.exit(1);
        }
    }

    private void invokeLine(String line) throws StopInterpreterException,
            WrongNumberOfArgumentsException, IOException {

        String[] commandsWithArgs = line.trim().split(";");
        for (String command : commandsWithArgs) {
            String[] tokens = command.trim().split("\\s+");
            String commandName = tokens[0];
            if (commandName.equals("") || commandName.equals(";")) {
                continue;
            }
            Command cmd = commands.get(commandName);
            if (cmd == null) {
                if (commandName.equals("exit")) {
                    throw new StopInterpreterException();
                } else {
                    throw new IOException(commandName + ": no such command");
                }
            }
            String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
            cmd.invoke(dbConnector, args);
        }
    }

    private void interactiveMode() throws StopInterpreterException {
        try (Scanner scanner = new Scanner(in)) {
            while (true) {
                out.print(PROMPT);
                try {
                    String str = scanner.nextLine();
                    invokeLine(str);
                } catch (IOException | WrongNumberOfArgumentsException e) {
                    err.println(e.getMessage());
                } catch (NoSuchElementException e) {
                    err.println(e.getMessage());
                    break;
                }
            }
        }
    }

    public boolean isBatch() {
        return isBatch;
    }
}