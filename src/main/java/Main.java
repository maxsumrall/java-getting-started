import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.RetryLogic;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

import static org.neo4j.driver.v1.Values.parameters;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

public class Main
{
    public static Driver driver = GraphDatabase
            .driver( "bolt+routing://ec2-54-170-252-11.eu-west-1.compute.amazonaws" + ".com:26000",
                    AuthTokens.basic( "neo4j", "max" ) );

    public static void main( String[] args )
    {
        port( Integer.valueOf( System.getenv( "PORT" ) ) );
        staticFileLocation( "/public" );

        get( "/", ( req, res ) ->
        {
            String bookmark = req.queryParams( "bookmark" );
            System.out.println( "bookmark = " + bookmark );

            List<Object> attributes = new ArrayList<>();

            try ( Session session = driver.session( AccessMode.READ ) )
            {
                Transaction transaction = session.beginTransaction( bookmark );
                StatementResult run = transaction
                        .run( "MATCH (n:Picture) RETURN n.uri AS uri, SIZE((n)<-[:FOR]-()) AS votes ORDER BY votes desc" );
                while ( run.hasNext() )
                {
                    Record next = run.next();
                    attributes.add( next.asMap() );
                }
                transaction.success();
            }

            Map<String,Object> params = new HashMap<>();
            params.put( "photos", attributes );

            return new ModelAndView( params, "index.ftl" );
        }, new FreeMarkerEngine() );

        post( "/", ( req, res ) ->
        {
            String currentUser = "Alistair";

            String myVote = req.queryParams( "photo" );
            System.out.println( "I voted for = " + myVote );

            String bookmark;
            try ( Session session = driver.session( AccessMode.WRITE ) )
            {
                try ( Transaction transaction = session.beginTransaction() )
                {

                    transaction.run( "MATCH (n:Picture {uri: {id}}), (person:Person {name: {name}}) " +
                                    "CREATE (person)-[:CAST]->(vote:Vote)-[:FOR]->(n)",
                            parameters( "id", myVote, "name", currentUser ) ).consume();
                    transaction.success();
                }

                bookmark = session.lastBookmark();
            }
            System.out.println( "bookmark = " + bookmark );

            res.redirect( "/?bookmark=" + bookmark );
            return "";
        } );

        get( "/eventual", ( req, res ) ->
        {
            List<Object> props = driver.transact( RetryLogic.TRY_UP_TO_3_TIMES_WITH_5_SECOND_PAUSE, AccessMode.READ, ( transaction ) ->
            {
                List<Object> attributes = new ArrayList<>(  );
                StatementResult run = transaction
                        .run( "MATCH (n:Picture) RETURN n.uri AS uri, SIZE((n)<-[:FOR]-()) AS votes ORDER BY votes desc" );
                while ( run.hasNext() )
                {
                    Record next = run.next();
                    attributes.add( next.asMap() );
                }
                transaction.success();
                return attributes;
            } );


            Map<String,Object> params = new HashMap<>();
            params.put( "photos", props );

            return new ModelAndView( params, "eventual.ftl" );
        }, new FreeMarkerEngine() );

        post( "/eventual", ( req, res ) ->
        {
            String currentUser = "Alistair";

            String myVote = req.queryParams( "photo" );

            driver.transact( RetryLogic.TRY_UP_TO_3_TIMES_WITH_5_SECOND_PAUSE, AccessMode.WRITE, ( transaction ) ->
            {
                transaction
                        .run( "MATCH (n:Picture {uri: {id}}), (person:Person {name: {name}}) " +
                                        "CREATE (person)-[:CAST]->(vote:Vote)-[:FOR]->(n)",
                                parameters( "id", myVote, "name", currentUser ) ).consume();

                transaction.success();
                return null;
            } );

            res.redirect( "/eventual");
            return "";
        } );

    }

}
