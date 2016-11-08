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

        get( "/", ( request, response ) ->
        {
            Map<String,Object> attributes = new HashMap<>();
            attributes.put( "message", "Hello World!" );

            return new ModelAndView( attributes, "index.ftl" );
        }, new FreeMarkerEngine() );

        get( "/photos", ( req, res ) ->
        {
            String bookmark = req.queryParams( "bookmark" );
            System.out.println( "bookmark = " + bookmark );

            List<Object> attributes = new ArrayList<>();

            try ( Session session = driver.session( AccessMode.READ ) )
            {
                Transaction transaction = session.beginTransaction(bookmark);
                StatementResult run = transaction.run(
                        "MATCH (n:Picture) RETURN n.uri AS uri, SIZE((n)<-[:FOR]-()) AS votes" );
                while ( run.hasNext() )
                {
                    Record next = run.next();
                    attributes.add( next.asMap() );
                }
                transaction.success();
            }

            Map<String, Object> params = new HashMap<>(  );
            params.put( "photos", attributes );

            return new ModelAndView( params, "photos.ftl" );
        }, new FreeMarkerEngine() );

        post("/photos", (req, res) ->
        {
            String currentUser = "Alistair";

            String myVote = req.queryParams( "photo" );
            System.out.println( "I voted for = " + myVote );

            String bookmark;
            try ( Session session = driver.session( AccessMode.WRITE ) )
            {
                try(Transaction transaction = session.beginTransaction())
                {

                    transaction
                            .run( "MATCH (n:Picture {uri: {id}}), (person:Person {name: {name}}) " +
                                            "CREATE (person)-[:CAST]->(vote:Vote)-[:FOR]->(n)",
                                    parameters( "id", myVote, "name", currentUser ) ).consume();
                    transaction.success();
                }

                bookmark = session.lastBookmark();
            }

            System.out.println( "bookmark = " + bookmark );


            res.redirect( "/photos?bookmark=" + bookmark );
            return "";
        });

    }

}
