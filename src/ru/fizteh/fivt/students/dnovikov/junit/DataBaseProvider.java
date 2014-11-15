package ru.fizteh.fivt.students.dnovikov.junit;

import javafx.util.Pair;
import ru.fizteh.fivt.storage.strings.Table;
import ru.fizteh.fivt.storage.strings.TableProvider;
import ru.fizteh.fivt.students.dnovikov.junit.Exceptions.LoadOrSaveException;
import ru.fizteh.fivt.students.dnovikov.junit.Exceptions.TableNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DataBaseProvider implements TableProvider {
    private DataBaseTable currentTable = null;
    private Path rootDirectory;

    private ArrayList<Table> tables = new ArrayList<>();
    private Map<String, Table> tableNames = new TreeMap<>();

    public DataBaseProvider(String directoryPath) throws LoadOrSaveException {
        if (directoryPath == null) {
            throw new IllegalArgumentException("database directory not set");
        } else {
            rootDirectory = new File(directoryPath).toPath();
        }
        loadTables();
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public DataBaseTable getCurrentTable() {
        return currentTable;
    }
    public void setCurrentTable(Table table) {
        currentTable = (DataBaseTable) table;
    }

    @Override
    public Table getTable(String name) {
        if (name == null) {
            throw new IllegalArgumentException("cannot get table: null");
        }
        return tableNames.get(name);
    }

    @Override
    public Table createTable(String name) throws LoadOrSaveException {
        if (name == null) {
            throw new IllegalArgumentException("cannot create table");
        }
        if (tableNames.get(name) != null) {
            return null;
        } else {
            Path newDir = new File(rootDirectory + File.separator + name).toPath();
            try {
                Files.createDirectory(newDir);
            } catch (IOException e) {
                throw new LoadOrSaveException("can't create directory :" + newDir.toAbsolutePath());
            }
            DataBaseTable table = new DataBaseTable(name, this);
            tableNames.put(name, table);
            tables.add(table);
            return table;
        }

    }

    @Override
    public void removeTable(String name) throws LoadOrSaveException, TableNotFoundException {
        if (name == null) {
            throw new IllegalArgumentException("cannot remove table: null");
        }
        DataBaseTable table = (DataBaseTable) tableNames.get(name);
        if (table == null) {
            throw new TableNotFoundException();
        } else {
            if (table.equals(currentTable)) {
                currentTable = null;
            }
            tableNames.remove(name);
            table.drop();
            tables.remove(table);
        }
    }

    public List<Pair<String, Integer>> showTable() {

        List<Pair<String, Integer>> result = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            DataBaseTable table = (DataBaseTable) tables.get(i);
            String tableName = table.getName();
            int size = table.size();
            result.add(new Pair<>(tableName, size));
        }
        return result;
    }

    public void loadTables() throws LoadOrSaveException {
        if (rootDirectory.toFile().exists() && rootDirectory.toFile().isDirectory()) {
            File[] foldersInRoot = rootDirectory.toFile().listFiles();
            for (File folder : foldersInRoot) {
                if (!folder.isDirectory()) {
                    throw new LoadOrSaveException("file '" + folder.getName() + "' in root directory");
                }
            }
            for (File folder : foldersInRoot) {
                DataBaseTable currTable = new DataBaseTable(folder.getName(), this);
                tables.add(currTable);
                tableNames.put(folder.getName(), currTable);
            }
        } else if (!rootDirectory.toFile().exists()) {
            throw new LoadOrSaveException("root directory '" + rootDirectory.getFileName() + "' not found");
        } else {
            throw new LoadOrSaveException("root directory '" + rootDirectory.getFileName() + "' is not directory");
        }
    }

    public void saveTable() throws IOException, LoadOrSaveException {
        if (currentTable != null) {
            currentTable.save();
        }
    }
}
