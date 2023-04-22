package phase2;
import java.sql.*;
import java.util.*;

public class phase3DB {

    // create connection to the database
    public static Connection createConnection() throws SQLException {
        Connection connection = null;
        try {
            String url = "jdbc:mysql://localhost:3306/hospitalDB";
            String username = "root";
            String password = "";
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.out.println("Error connecting to hospitalDB: " + e.getMessage());
        }
        return connection;
    }// end create connection

    // retrieve all data from a table
    public static void retrieveAllData(String tableName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;

        ResultSet resultSet = null;
        try {
            conn = createConnection();
            String retrieveQuery = "SELECT * FROM " + tableName;
            stmt = conn.prepareStatement(retrieveQuery);
            resultSet = stmt.executeQuery();
            System.out.println("\nData from the " + tableName + " table:");
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(resultSet.getString(i) + " ");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving data from table: " + e.getMessage());
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    // calculate the average of a column in a table
    public static void calculateAverage(String tableName, String columnName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            conn = createConnection();
            String query = "SELECT AVG(" + columnName + ") FROM " + tableName;
            stmt = conn.prepareStatement(query);
            resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                System.out.println("\nAverage of " + columnName + " in " + tableName + ": " + resultSet.getFloat(1));
            }
        } catch (SQLException e) {
            System.out.println("Error calculating average: " + e.getMessage());
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    public static void insertData(String tableName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = createConnection();
            String insertQuery = "DESC " + tableName;
            stmt = conn.prepareStatement(insertQuery);
            ResultSet resultSet = stmt.executeQuery();
            List<String> columnNames = new ArrayList<>();
            while (resultSet.next()) {
                columnNames.add(resultSet.getString(1));
            }
            List<String> values = new ArrayList<>();
            Scanner scnr = new Scanner(System.in);
            for (String columnName : columnNames) {
                System.out.print("Enter " + columnName + ": ");
                String value = scnr.nextLine();
                values.add(value);
            }
            insertQuery = "INSERT INTO " + tableName + " (" + String.join(", ", columnNames) + ") VALUES (" + String.join(", ", Collections.nCopies(columnNames.size(), "?")) + ")";
            stmt = conn.prepareStatement(insertQuery);
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i+1, values.get(i));
            }
            int rowsInserted = stmt.executeUpdate();
            System.out.println("\n" + rowsInserted + " row(s) inserted.");
        } catch (SQLException e) {
            System.out.println("Error inserting data: " + e.getMessage());
            System.out.println("MySQL error code: " + e.getErrorCode());
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    
    public static void deleteData(String tableName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = createConnection();
            String delQuery = "DESC " + tableName;
            stmt = conn.prepareStatement(delQuery);
            ResultSet resultSet = stmt.executeQuery();
            List<String> columnNames = new ArrayList<>();
            while (resultSet.next()) {
                columnNames.add(resultSet.getString(1));
            }
            System.out.println("Columns in the " + tableName + " table: " + columnNames);
            List<String> values = new ArrayList<>();
            Scanner scanner = new Scanner(System.in);
            for (String columnName : columnNames) {
                System.out.print("Enter value for " + columnName + ": ");
                String value = scanner.nextLine();
                values.add(value);
            }
            delQuery = "DELETE FROM " + tableName + " WHERE ";
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    delQuery += " AND ";
                }
                delQuery += columnNames.get(i) + " = ?";
            }
            stmt = conn.prepareStatement(delQuery);
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i+1, values.get(i));
            }
            int rowsDeleted = stmt.executeUpdate();
            System.out.println("\n" + rowsDeleted + " row(s) deleted.");
        } catch (SQLException e) {
            System.out.println("Error deleting data: " + e.getMessage());
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }


    /*// Update data in a table
    public static void updateData(String tableName) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = createConnection();
            String query = "DESC " + tableName;
            statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            List<String> columnNames = new ArrayList<>();
            while (resultSet.next()) {
                columnNames.add(resultSet.getString(1));
            }
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the ID of the row to update: ");
            String id = scanner.nextLine();
            List<String> setValues = new ArrayList<>();
            for (String columnName : columnNames) {
                System.out.print("Enter new value for " + columnName + ": ");
                String value = scanner.nextLine();
                setValues.add(columnName + " = ?");
            }
            query = "UPDATE " + tableName + " SET " + String.join(", ", setValues) + " WHERE ID = ?";
            statement = connection.prepareStatement(query);
            int index = 1;
            for (String setValue : setValues) {
                statement.setString(index, scanner.nextLine());
                index++;
            }
            statement.setString(index, id);
            int rowsUpdated = statement.executeUpdate();
            System.out.println("\n" + rowsUpdated + " row(s) updated.");
        } catch (SQLException e) {
            System.out.println("Error updating data: " + e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
*/

    public static void main(String[] args) throws SQLException {
        Scanner scnr = new Scanner(System.in);
        System.out.println("WELCOME TO LAS PALMAS HOSPITAL");
        boolean exit = false;
        while (!exit) {
            System.out.println("\nPlease select an option:");
            System.out.println("1. Retrieve all data from a table");
            System.out.println("2. Calculate the average of a column in a table");
            System.out.println("3. Insert data into a table");
            //System.out.println("4. Update data in a table");
            System.out.println("5. Delete data from a table");
            System.out.println("6. Exit");
            int choice = scnr.nextInt();
            scnr.nextLine(); // consume new line character
            switch (choice) {
                case 1:
                    System.out.print("\nEnter table name to retrieve data from: ");
                    String tableName = scnr.nextLine();
                    retrieveAllData(tableName);
                    break;
                case 2:
                    System.out.print("\nEnter table name to calculate the average: ");
                    tableName = scnr.nextLine();
                    System.out.print("Enter column name to: ");
                    String columnName = scnr.nextLine();
                    calculateAverage(tableName, columnName);
                    break;
                case 3:
                    System.out.print("\nEnter table name to Insert data into: ");
                    tableName = scnr.nextLine();
                    insertData(tableName);
                    break;
                /*case 4:
                    System.out.print("\nEnter table name: ");
                    tableName = scanner.nextLine();
                    updateData(tableName);
                    break;*/
                case 5:
                    System.out.print("\nEnter table name to delete data from: ");
                    tableName = scnr.nextLine();
                    deleteData(tableName);
                    break;
                case 6:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid: Select a different option");
            }
        }
    }
}