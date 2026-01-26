package wethinkcode.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exercise 3.3
 */
public class Finder {

    private final Connection connection;

    /**
     * Create an instance of the Finder object using the provided database connection
     *
     * @param connection The JDBC connection to use
     */
    public Finder(Connection connection) {
        this.connection = connection;
    }

    /**
     * 3.3 (part 1) Complete this method
     * <p>
     * Finds all genres in the database
     *
     * @return a list of `Genre` objects
     * @throws SQLException the query failed
     */
    public List<Genre> findAllGenres() throws SQLException {
        List<Genre> findGenres = new ArrayList<>();

        String sql = "SELECT code, description FROM Genres";

        PreparedStatement statement = connection.prepareStatement(sql);

        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            String code = resultSet.getString("code");
            String description = resultSet.getString("description");
            findGenres.add(new Genre(code, description));
        }

        return findGenres;
    }

    /**
     * 3.3 (part 2) Complete this method
     * <p>
     * Finds all genres in the database that have specific substring in the description
     *
     * @param pattern The pattern to match
     * @return a list of `Genre` objects
     * @throws SQLException the query failed
     */
    public List<Genre> findGenresLike(String pattern) throws SQLException {
        List<Genre> results = new ArrayList<>();

        String sql = "SELECT code, description FROM Genres WHERE DESCRIPTION LIKE ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, "%" + pattern + "%");
        ResultSet resultSet = stmt.executeQuery();

        while (resultSet.next()) {
            String code = resultSet.getString("code");
            String description = resultSet.getString("description");
            results.add(new Genre(code, description));
        }
        return results;
    }

    /**
     * 3.3 (part 3) Complete this method
     * <p>
     * Finds all books with their genres
     *
     * @return a list of `BookGenreView` objects
     * @throws SQLException the query failed
     */
    public List<BookGenreView> findBooksAndGenres() throws SQLException {
        findAllGenres();
        List<BookGenreView> booksAndGenres = new ArrayList<>();

        String sql = "SELECT Books.title, Genres.description FROM Books JOIN Genres ON Books.genre_code = Genres.code";

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            String title = resultSet.getString("title");
            String description = resultSet.getString("description");
            booksAndGenres.add(new BookGenreView(title, description));
        }
        return booksAndGenres;
    }

    /**
     * 3.3 (part 4) Complete this method
     * <p>
     * Finds the number of books in a genre
     *
     * @return the number of books in the genre
     * @throws SQLException the query failed
     */
    public int findNumberOfBooksInGenre(String genreCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Books WHERE genre_code = ?";
        PreparedStatement statement= connection.prepareStatement(sql);

        statement.setString(1, genreCode);

        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            return(resultSet.getInt(1));
        }
        return 0;
    }
}
