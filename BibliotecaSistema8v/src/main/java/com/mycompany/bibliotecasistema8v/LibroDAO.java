/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bibliotecasistema8v;

/**
 *
 * @author alexa
 */
import static com.mycompany.bibliotecasistema8v.BibliotecaSistema8v.getConnection;
import java.awt.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

public class LibroDAO {
    Connection connection = BibliotecaSistema8v.getConnection();
    
    // tabla de VerLibross
    public DefaultTableModel getLibrosTableModel() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ISBN");
        model.addColumn("Titulo");
        model.addColumn("Autor");
        model.addColumn("Año");
        model.addColumn("Ejemplares Totales");
        model.addColumn("Ejemplares Disponibles");

        String query = "SELECT " +
                       "l.ISBN, " +
                       "l.titulo, " +
                       "a.nombre AS autor, " +
                       "l.año, " +
                       "ce.cantidad AS ejemplares_totales, " +
                       "ce.cantidad - COALESCE(pl.prestados, 0) AS ejemplares_disponibles " +
                       "FROM libros l " +
                       "JOIN autores a ON l.autor_id = a.id " +
                       "JOIN cantidadEjemplares ce ON l.ISBN = ce.id_libro " +
                       "LEFT JOIN " +
                       "(SELECT id_libro, COUNT(*) AS prestados FROM prestamosLibros GROUP BY id_libro) pl ON l.ISBN = pl.id_libro";

        try (
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                model.addRow(new Object[]{
                    resultSet.getString("ISBN"),
                    resultSet.getString("titulo"),
                    resultSet.getString("autor"),
                    resultSet.getInt("año"),
                    resultSet.getInt("ejemplares_totales"),
                    resultSet.getInt("ejemplares_disponibles")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return model;
    }
    
    // Hacer un prestamo de un libro
    public boolean pedirLibro(String isbn) {
        String querySelect = "SELECT cantidad FROM cantidadEjemplares WHERE id_libro = ?";
        String queryUpdate = "UPDATE cantidadEjemplares SET cantidad = cantidad - 1 WHERE id_libro = ?";
        String queryInsert = "INSERT INTO prestamosLibros (id_usuario, id_libro) VALUES (?, ?)";
        int idUsuario = 1; // ID de usuario fijo

        try (
             PreparedStatement preparedStatementSelect = connection.prepareStatement(querySelect)) {

            preparedStatementSelect.setString(1, isbn);
            ResultSet resultSet = preparedStatementSelect.executeQuery();

            if (resultSet.next()) {
                int cantidadDisponible = resultSet.getInt("cantidad");
                if (cantidadDisponible > 0) {
                    // Disminuir la cantidad de ejemplares disponibles
                    try (PreparedStatement preparedStatementUpdate = connection.prepareStatement(queryUpdate)) {
                        preparedStatementUpdate.setString(1, isbn);
                        preparedStatementUpdate.executeUpdate();
                    }

                    // Registrar el préstamo
                    try (PreparedStatement preparedStatementInsert = connection.prepareStatement(queryInsert)) {
                        preparedStatementInsert.setInt(1, idUsuario);
                        preparedStatementInsert.setString(2, isbn);
                        preparedStatementInsert.executeUpdate();
                        return true; // El préstamo fue exitoso
                    }
                } else {
                    return false; // No hay ejemplares disponibles
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Hacer la devolucion de un libro
    public boolean devolverLibro(String isbn) {
    String querySelect = "SELECT * FROM prestamosLibros WHERE id_libro = ?";
    String queryDelete = "DELETE FROM prestamosLibros WHERE id_libro = ? LIMIT 1";
    String queryUpdate = "UPDATE cantidadEjemplares SET cantidad = cantidad + 1 WHERE id_libro = ?";

    try (
        PreparedStatement statementSelect = connection.prepareStatement(querySelect)) {
        
        statementSelect.setString(1, isbn);
        ResultSet resultSet = statementSelect.executeQuery();

        if (resultSet.next()) {
            // Eliminar el registro de préstamo
            try (PreparedStatement statementDelete = connection.prepareStatement(queryDelete)) {
                statementDelete.setString(1, isbn);
                statementDelete.executeUpdate();
            }

            // Incrementar la cantidad de ejemplares disponibles
            try (PreparedStatement statementUpdate = connection.prepareStatement(queryUpdate)) {
                statementUpdate.setString(1, isbn);
                statementUpdate.executeUpdate();
                return true;
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    return false;
    }
    
    // Eliminar un libro de la bbdd
    public boolean eliminarLibro(String isbn) {
    String querySelect = "SELECT * FROM libros WHERE ISBN = ?";
    String queryDeletePrestamos = "DELETE FROM prestamosLibros WHERE id_libro = ?";
    String queryDeleteCantidad = "DELETE FROM cantidadEjemplares WHERE id_libro = ?";
    String queryDeleteLibro = "DELETE FROM libros WHERE ISBN = ?";

    try (
        PreparedStatement statementSelect = connection.prepareStatement(querySelect)) {
        
        statementSelect.setString(1, isbn);
        ResultSet resultSet = statementSelect.executeQuery();

        if (resultSet.next()) {
            // Eliminar registros de préstamos
            try (PreparedStatement statementDeletePrestamos = connection.prepareStatement(queryDeletePrestamos)) {
                statementDeletePrestamos.setString(1, isbn);
                statementDeletePrestamos.executeUpdate();
            }

            // Eliminar registro de cantidad de ejemplares
            try (PreparedStatement statementDeleteCantidad = connection.prepareStatement(queryDeleteCantidad)) {
                statementDeleteCantidad.setString(1, isbn);
                statementDeleteCantidad.executeUpdate();
            }

            // Eliminar el libro
            try (PreparedStatement statementDeleteLibro = connection.prepareStatement(queryDeleteLibro)) {
                statementDeleteLibro.setString(1, isbn);
                statementDeleteLibro.executeUpdate();
                return true;
            }
        } else {
            System.out.println("El libro con ISBN " + isbn + " no existe.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    return false;
    }
  
    // agregar un autor al combobox, also es necesario hacerlo antes de agregar un libro
    public boolean agregarlibro(Autor autor) {
    String query = "INSERT INTO autores (nombre, edad, nacionalidad) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, autor.getNombre());
            statement.setInt(2, autor.getEdad());
            statement.setString(3, autor.getNacionalidad());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // agregar un libro a la BBDD
     
      
}