import com.heroku.sdk.jdbc.DatabaseUrl;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

public class Main
{

    public static void main( String[] args )
    {

        port( Integer.valueOf( System.getenv( "PORT" ) ) );
        staticFileLocation( "/public" );

        get( "/hello", ( req, res ) -> "Hello World" );

        get( "/", ( request, response ) ->
        {
            Map<String,Object> attributes = new HashMap<>();
            attributes.put( "message", "Hello World!" );

            return new ModelAndView( attributes, "index.ftl" );
        }, new FreeMarkerEngine() );

        get( "/neo4j", ( req, res ) ->
        {
            String outMessage = "Nothing found in DB";
            Driver driver = GraphDatabase.driver( "bolt://ec2-54-170-252-11.eu-west-1.compute.amazonaws.com:26000",
                    AuthTokens.basic( "neo4j","max" ));
            try ( Session session = driver.session( AccessMode.READ ) )
            {
                Transaction transaction = session.beginTransaction();
                StatementResult run = transaction.run( "MATCH (n) RETURN n" );
                Record single = run.single();
                outMessage = single.toString();
                transaction.success();
            }
            driver.close();
            return outMessage;
        } );

        get( "/db", ( req, res ) ->
        {
            Connection connection = null;
            Map<String,Object> attributes = new HashMap<>();
            try
            {
                connection = DatabaseUrl.extract().getConnection();

                Statement stmt = connection.createStatement();
                stmt.executeUpdate( "CREATE TABLE IF NOT EXISTS ticks (tick timestamp)" );
                stmt.executeUpdate( "INSERT INTO ticks VALUES (now())" );
                ResultSet rs = stmt.executeQuery( "SELECT tick FROM ticks" );

                ArrayList<String> output = new ArrayList<String>();
                while ( rs.next() )
                {
                    output.add( "Read from DB: " + rs.getTimestamp( "tick" ) );
                }

                attributes.put( "results", output );
                return new ModelAndView( attributes, "db.ftl" );
            }
            catch ( Exception e )
            {
                attributes.put( "message", "There was an error: " + e );
                return new ModelAndView( attributes, "error.ftl" );
            }
            finally
            {
                if ( connection != null )
                {
                    try
                    {
                        connection.close();
                    }
                    catch ( SQLException e )
                    {
                    }
                }
            }
        }, new FreeMarkerEngine() );

    }

}
