package io.quarkiverse.googlecloudservices.it;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

@Path("/firestore")
public class FirestoreResource {
    @Inject
    Firestore firestore;

    private void fill() throws ExecutionException, InterruptedException {
        // insert 3 persons
        CollectionReference persons = firestore.collection("persons");
        List<ApiFuture<WriteResult>> futures = new ArrayList<>();
        futures.add(persons.document("1").set(new Person(1L, "John", "Doe")));
        futures.add(persons.document("2").set(new Person(2L, "Jane", "Doe")));
        futures.add(persons.document("3").set(new Person(3L, "Charles", "Baudelaire")));
        ApiFutures.allAsList(futures).get();
    }

    @GET
    @Path("query")
    @Produces(MediaType.TEXT_PLAIN)
    public String query() throws ExecutionException, InterruptedException {
        fill();

        // search for lastname=Doe
        CollectionReference persons = firestore.collection("persons");
        Query query = persons.whereEqualTo("lastname", "Doe");
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        return querySnapshot.get().getDocuments().stream()
                .map(document -> document.getId() + " - " + document.getString("firstname") + " "
                        + document.getString("lastname") + "\n")
                .collect(Collectors.joining());
    }

    @GET
    @Path("all")
    @Produces(MediaType.TEXT_PLAIN)
    public String all() throws ExecutionException, InterruptedException {
        fill();

        CollectionReference persons = firestore.collection("persons");
        return StreamSupport.stream(persons.listDocuments().spliterator(), false)
                .map(r -> {
                    try {
                        return r.get().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }).map(document -> document.getId() + " - " + document.getString("firstname") + " "
                        + document.getString("lastname") + "\n")
                .collect(Collectors.joining());
    }

}
